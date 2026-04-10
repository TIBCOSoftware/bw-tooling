#!/usr/bin/env bash
set -euo pipefail

# ==============================
# Configuration (edit as needed)
# ==============================

# Script metadata
VERSION="0.1"
AUTHOR="Alexandre Vazquez <alexandre.vazquez@tibco.com>"

# Optional external configuration overrides
CONFIG_PROPS_FILE="${CONFIG_PROPS_FILE:-./config.props}"

# AppManage flags vary by installation. Tweak the templates below if needed.
# Template variables are shell-style ${VARS} resolved via envsubst.
APPMANAGE_EXPORT_EAR_TMPL='${APPMANAGE_BIN} --propFile ${APPMANAGE_BIN_FOLDER}/AppManage.tra -export -domain "${DOMAIN}" -app "${APP_NAME}" -user "${ADMIN_USER}" -pw "${ADMIN_PASS}" -out "${EAR_PATH}.xml" -ear "${EAR_PATH}" -genEar'
# Some BW5 versions use -host/-port instead of -url; edit accordingly if needed.


# ================
# Helper functions
# ================

DEBUG="false"
CURRENT_APP=""
log() { if [[ "$DEBUG" == "true" ]]; then printf '[%(%Y-%m-%d %H:%M:%S)T] [DEBUG]%s %s\n' -1 "${CURRENT_APP:+ [${CURRENT_APP}]}" "$*" >&2; fi; return 0; }
err() { printf '[%(%Y-%m-%d %H:%M:%S)T] [ERROR]%s %s\n' -1 "${CURRENT_APP:+ [${CURRENT_APP}]}" "$*" >&2; return 0; }
die() { printf '[%(%Y-%m-%d %H:%M:%S)T] [ERROR]%s %s\n' -1 "${CURRENT_APP:+ [${CURRENT_APP}]}" "$*" >&2; exit 1; }

# One-line outcome summary for non-batch flows
summary() { printf '%s\n' "$*"; }

require_bin() {
  local b="$1"
  command -v "$b" >/dev/null 2>&1 || die "Required binary not found in PATH: $b"
}

check_prereqs() {
  # Check all given binaries and print a tool/status/path table; exit if any missing
  local -a tools=("$@")
  local b tool_w=4 status_w=6 path_w=4
  local -a statuses paths

  for b in "${tools[@]}"; do
    local p
    p=$(command -v "$b" 2>/dev/null || true)
    if [[ -n "$p" ]]; then
      statuses+=("OK"); paths+=("$p")
    else
      statuses+=("MISSING"); paths+=("not found in PATH")
    fi
    (( ${#b}           > tool_w   )) && tool_w=${#b}
    (( ${#statuses[-1]} > status_w )) && status_w=${#statuses[-1]}
    (( ${#paths[-1]}    > path_w   )) && path_w=${#paths[-1]}
  done

  local border fmt
  border="+$(printf -- '-%.0s' $(seq 1 $((tool_w+2))))+$(printf -- '-%.0s' $(seq 1 $((status_w+2))))+$(printf -- '-%.0s' $(seq 1 $((path_w+2))))+"
  fmt="| %-${tool_w}s | %-${status_w}s | %-${path_w}s |\n"

  local i has_missing=0
  for (( i=0; i<${#tools[@]}; i++ )); do
    [[ "${statuses[$i]}" == "MISSING" ]] && has_missing=1
  done

  if (( has_missing )); then
    printf '%s\n' "$border" >&2
    printf "$fmt" "Tool" "Status" "Path" >&2
    printf '%s\n' "$border" >&2
    for (( i=0; i<${#tools[@]}; i++ )); do
      printf "$fmt" "${tools[$i]}" "${statuses[$i]}" "${paths[$i]}" >&2
    done
    printf '%s\n' "$border" >&2
    err "Install the missing tools and re-run the script."
    exit 1
  fi
}

# Cache expensive yq flavor detection so we call yq --version only once
YQ_FLAVOR_CACHE=""

# Strip leading/trailing whitespace in one place for clarity
trim_spaces() {
  local s="${1-}"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

# Normalize whitespace, collapse runs, and clamp to 500 chars for readable logs
normalize_whitespace() {
  local s="${1-}"
  s=${s//$'\r'/}
  s=${s//$'\n'/ }
  s=$(printf '%s' "$s" | tr -s '[:space:]' ' ')
  printf '%s' "${s:0:500}"
}

# Returns 0 (true) if the given deployment properties XML belongs to a BW app
# Detection heuristics (any one is sufficient):
#  - //product/type == 'BW'
#  - presence of a <bw> element
is_bw_properties_xml() {
  local xml="$1"
  [[ -f "$xml" ]] || return 1
  # Namespace-agnostic XPath using local-name() for robustness
  local prod_type
  prod_type=$(xmlstarlet sel -t -v "normalize-space(//*[local-name()='product']/*[local-name()='type'])" "$xml" 2>/dev/null | tr -d '\r')
  if [[ "$prod_type" == "BW" ]]; then
    return 0
  fi
  local has_bw
  has_bw=$(xmlstarlet sel -t -v "count(//*[local-name()='bw'])" "$xml" 2>/dev/null || echo "0")
  if [[ "$has_bw" =~ ^[1-9] ]]; then
    return 0
  fi
  # Fallback to a simple grep in case xmlstarlet fails for any reason
  if grep -qi '<bw[[:space:]>]' "$xml" 2>/dev/null; then
    return 0
  fi
  return 1
}

detect_yq_flavor() {
  if [[ -n "${YQ_FLAVOR_CACHE:-}" ]]; then
    echo "$YQ_FLAVOR_CACHE"
    return 0
  fi
  local v
  if ! v=$(yq --version 2>&1); then
    die "yq not found"
  fi
  # mikefarah/yq v4 typically prints: \"yq (https://github.com/mikefarah/yq/) version 4.x.x\"
  # python yq prints something like: \"yq 3.x.x\" and supports --yaml-output/--in-place flags
  if grep -qi 'mikefarah' <<<"$v" || grep -Eq '\bversion 4' <<<"$v"; then
    YQ_FLAVOR_CACHE="mf"
  else
    YQ_FLAVOR_CACHE="py"
  fi
  echo "$YQ_FLAVOR_CACHE"
}

# Set a YAML value in-place, compatible with both yq flavors
yq_set() {
  local file="$1"; shift
  local expr="$1"
  local yq_flavor
  yq_flavor=$(detect_yq_flavor)
  if [[ "$yq_flavor" == "mf" ]]; then
    yq eval -i "$expr" "$file"
  else
    yq -y -i "$expr" "$file"
  fi
}

# Normalize a string to a valid k8s resource name: lowercase, digits, and hyphens only
# - replace any other char with '-'
# - collapse multiple hyphens
# - trim leading/trailing hyphens
to_k8s_name() {
  local name="$1"
  name="$(echo "$name" | tr '[:upper:]' '[:lower:]')"
  name="$(echo "$name" | sed 's/[^a-z0-9-]/-/g')"
  name="$(echo "$name" | sed 's/-\{2,\}/-/g')"
  name="$(echo "$name" | sed 's/^-//; s/-$//')"
  echo "$name"
}

# Normalize to a valid Kubernetes label value while preserving intent
# - replace slashes with underscores
# - strip all whitespace
# - replace other invalid characters with underscores
# - trim leading/trailing non-alphanumeric chars
# - fall back to "default" if nothing remains
to_k8s_label_value() {
  local value="$1"
  value="${value//\//_}"
  value="${value//[[:space:]]/}"
  value="$(echo "$value" | sed 's/[^A-Za-z0-9_.-]/_/g')"
  value="$(echo "$value" | sed 's/^[^A-Za-z0-9]*//; s/[^A-Za-z0-9]*$//')"
  echo "$value"
}

# Compute Platform tags from an application display name/path
# - If the name contains folders (e.g. "Folder A/Sub B/My App"),
#   tags become the comma-separated list of folder paths preceding the app name.
# - Current logic mirrors upload behavior: a single tag being the full folder path.
compute_tags_vars() {
  # stdin/none; args: app_display_name
  local app_disp="$1"
  local nm tags_json tags_csv
  nm="${app_disp%/}"
  if [[ "$nm" == */* ]]; then
    local dirpath="${nm%/*}"
    # Trim spaces
    dirpath="$(trim_spaces "$dirpath")"
    # JSON-escape backslashes and quotes for tags_json
    local esc="${dirpath//\\/\\\\}"
    esc="${esc//\"/\\\"}"
    tags_json='["'"$esc"'"]'
    # CSV version is the literal folder path (no JSON escaping)
    tags_csv="$dirpath"
  else
    tags_json='[]'
    tags_csv=""
  fi
  printf '%s\t%s\n' "$tags_json" "$tags_csv"
}

# Set .appConfig.tags in a values.yaml file based on an app display name
set_values_appconfig_tags() {
  local values_file="$1"
  local app_disp_name="$2"
  local override_csv="${3-}"
  local out tjson tcsv
  if [[ -n "$override_csv" ]]; then
    # Use explicit CSV override
    tcsv="$override_csv"
  else
    out="$(compute_tags_vars "$app_disp_name")"
    # split TSV to vars
    tjson="${out%%$'\t'*}"
    tcsv="${out#*$'\t'}"
  fi
  # Write CSV into values.yaml under appConfig.tags, forcing double quotes
  tcsv=$(to_k8s_label_value "$tcsv")
  local yq_flavor
  yq_flavor=$(detect_yq_flavor)
  if [[ "$yq_flavor" == "mf" ]]; then
    # mikefarah yq: set value via env var (older builds lack --arg) and enforce double-quoted style
    YQ_APPCONFIG_TAGS="$tcsv" yq eval -i '.appConfig.tags = strenv(YQ_APPCONFIG_TAGS) | (.appConfig.tags style="double")' "$values_file"
  else
    # python yq: set the value, then post-process the specific line to ensure quoting
    yq -y -i --arg v "$tcsv" '.appConfig.tags = $v' "$values_file"
    local tmp
    tmp="$(mktemp)"
    awk -v val="$tcsv" '
      BEGIN{inapp=0}
      /^[^[:space:]]/ { inapp=0 }
      /^appConfig:[[:space:]]*$/ { print; inapp=1; next }
      {
        if (inapp==1) {
          if (match($0, /^([[:space:]]+)tags:[[:space:]]*/, m)) {
            print m[1] "tags: \"" val "\""; inapp=0; next
          }
        }
        print
      }
    ' "$values_file" > "$tmp" && mv "$tmp" "$values_file"
  fi
}

# Helpers for parsing Platform API errors
json_field_text() {
  local body="$1" filter="$2" value=""
  if command -v jq >/dev/null 2>&1; then
    value=$(jq -r "$filter // empty" <<<"$body" 2>/dev/null || true)
  fi
  normalize_whitespace "$value"
}

# Extract a human-friendly error message from a Platform API response body
extract_error_message() {
  local body="$1"
  local msg=""
  if command -v jq >/dev/null 2>&1; then
    msg=$(echo "$body" | jq -r '
      (.message
       // .error
       // .errorMessage
       // .error_description
       // .description
       // .detail
       // .developerMessage
       // (.errors | if type=="array" then (map(.message // .detail // tostring) | join("; "))
                   elif type=="object" then (.message // .detail // tostring)
                   else empty end)
       // .status
       // empty)'
      2>/dev/null || true)
  fi
  # Fallback to raw body if jq not available or message empty/null
  if [[ -z "$msg" || "$msg" == "null" ]]; then
    msg=$(echo "$body" | tr -d '\r' | tr '\n' ' ' | sed 's/[[:space:]]\{2,\}/ /g')
  fi
  # Trim to a reasonable length
  echo "$msg" | cut -c1-500
}

# Prefer upload-specific error message from Platform response
extract_upload_error_reason() {
  local body="$1"
  echo "$(extract_errmsg_any "$body")"
}

# Extract upload-specific detailed reason (errDetail) if present
extract_upload_error_detail() {
  local body="$1"
  echo "$(json_field_text "$body" '.errDetail')"
}
# Extract errMsg from body or nested JSON inside known fields; fallback to readable message
extract_errmsg_any() {
  local body="$1"
  local msg=""
  if command -v jq >/dev/null 2>&1; then
    msg=$(jq -r '
      def flatten:
        if type == "string" then
          [.] + (try (fromjson | flatten) catch [])
        elif type == "array" then
          map(flatten) | add
        elif type == "object" then
          ([.errorMsg, .errMsg, .message, .detail,
            .description, .developerMessage, .error,
            .errDetail, .errorDetail, .errors?, .[]?]
           | map(select(. != null))
           | map(flatten)
           | add)
        else []
        end;
      (flatten | map(select(type == "string" and length > 0)) | .[0]) // empty
    ' <<<"$body" 2>/dev/null || true)
  fi
  if [[ -z "$msg" || "$msg" == "null" ]]; then
    msg=$(extract_error_message "$body")
  fi
  echo "$(normalize_whitespace "$msg")"
}

# Extract raw errDetail from Platform response (deployment errors)
extract_errdetail() {
  local body="$1"
  echo "$(json_field_text "$body" '.errDetail')"
}

usage() {
  cat <<EOF
Usage:
  $(basename "$0") <DOMAIN> <APP_NAME> [--namespace <ns>] [--platform <PLATFORM_ENV>] [--offline] [--no-deploy] [--no-start]
  $(basename "$0") <DOMAIN> --batch [--offline] [--namespace <ns>] [--platform <PLATFORM_ENV>] [--no-deploy] [--no-start]
  $(basename "$0") [<DOMAIN>] [<APP_NAME>] --deploy-offline --platform <PLATFORM_ENV>
  $(basename "$0") --app <NAME> --ear <PATH> --xml <PATH> [--namespace <ns>] [--platform <PLATFORM_ENV>] [--no-deploy] [--no-start]
  $(basename "$0") <DOMAIN> <APP_NAME> --analyze-only [--report [<path>]]
  $(basename "$0") --version

Behavior:
  - Loads ADMIN_URL/ADMIN_USER/ADMIN_PASS from: ${ENV_DIR:-./env}/<DOMAIN>.env
  - Exports EAR and deployment properties XML with AppManage
  - Writes YAML of Global Variables to output and updates values.yaml
  - If --platform is provided, can upload & deploy via Platform APIs using env/${PLATFORM_ENV}.env
  - Quiet by default; use --debug for verbose logs
  - Platform portability analysis runs automatically when an EAR is available

Batch mode:
  - Uses AppManage -batchExport to export ALL apps' EARs from the domain.
  - Parses AppManage logs to extract app names and sets fullnameOverride from those names.
  - If not --offline, also exports deployment properties per app and updates values.

Offline and control flags:
  --offline            Export artifacts only (EAR + props + values); no upload or deploy
  --no-deploy          Upload EAR to platform (requires --platform), but do not deploy
  --no-start           Deploy with replicaCount=0 (app not started)

Portability analysis flags:
  --analyze-only       Run analysis only; do not upload or deploy (no --platform needed)
  --report [<path>]    Generate HTML readiness report (default: output/<app>-analysis-<ts>.html)
  --report-cli [<path>] Generate plain-text readiness report (default: output/<app>-analysis-<ts>.txt)
  --allow-blockers     Deploy even if BLOCKER issues are found (use with caution)
  --insecure-tls       Disable TLS certificate verification for Platform API calls (adds curl -k)

Options:
  --namespace <ns>     Namespace for deploy (optional; omitted if not provided)
  --platform <name>    Platform environment name (uses ${ENV_DIR}/<name>.env for PLATFORM_* vars)
  --batch              Export all apps in domain via AppManage -batchExport
  --offline            Export only; do not upload or deploy
  --no-deploy          Upload only; do not deploy (requires --platform)
  --no-start           Deploy with replicaCount=0 (requires --platform)
  --force              If app exists, perform upgrade instead of omitting
  --deploy-offline     Deploy using only artifacts from output/ via Platform API
  --app <name>         Use provided EAR/XML instead of AppManage; application name
  --ear <path>         Path to an existing EAR file (with --app)
  --xml <path>         Path to deployment properties XML (with --app)
  --debug              Print detailed logs during execution
  --version            Print version and author
  -h, --help           Show this help

Environment overrides:
  ENV_DIR, APPMANAGE_BIN, WORK_DIR, OUTPUT_DIR,
  VALUES_FILE

Platform env file (env/<name>.env) must define:
  PLATFORM_BW5CE_BASE_URL
  PLATFORM_BW5CE_BASE_VERSION
  PLATFORM_BW5CE_BASE_IMAGE_TAG
  PLATFORM_TOKEN
  
Version: $VERSION
Author:  $AUTHOR
EOF
}

# YAML generation from Global Variables inside FINAL_PROPS
generate_yaml_from_global_vars() {
  local xml="$1"
  local yaml_out="$2"
  local NS="http://www.tibco.com/xmlns/ApplicationManagement"

  {
    echo "globalVariables:"
    # Collect <name>|<value> from NameValuePair and NameValuePairInteger
    xmlstarlet sel -N a="$NS" \
      -t -m "//a:NVPairs[@name='Global Variables']/*[self::a:NameValuePair or self::a:NameValuePairInteger]" \
      -v "concat(a:name,'|',a:value)" -n "$xml" \
    | while IFS='|' read -r key val; do
        # trim
        key="$(trim_spaces "$key")"
        val="$(trim_spaces "$val")"
        # replace '/' in keys to avoid YAML path semantics
        key="${key//\//__SlAsH__}"

        # number? now also quote as string (use double quotes for numbers)
        if [[ "$val" =~ ^-?[0-9]+([.][0-9]+)?$ ]]; then
          # escape backslashes and double quotes for YAML double-quoted style
          local v="$val"
          v="${v//\\/\\\\}"
          v="${v//\"/\\\"}"
          printf "  %s: \"%s\"\n" "$key" "$v"
        else
          # YAML-safe single quotes for general strings
          val="${val//\'/\'\'}"
          printf "  %s: '%s'\n" "$key" "$val"
        fi
      done
  } > "$yaml_out"

  log "YAML (Global Variables) ....: $yaml_out"
}

# =====================
# Batch report utilities
# =====================

declare -a BATCH_REPORT_ROWS=()
declare -a BATCH_NAMES=()
declare -a BATCH_STATUS=()
declare -a BATCH_NOTES=()
declare -a BATCH_HTML_LINKS=()   # relative path to individual HTML report; empty if not generated
declare -i BATCH_COUNT=0
LAST_ERROR_DETAILS=""
PLATFORM_BUILD_ID=""

add_report_row() {
  local name="$1" status="$2" notes="$3" html_link="${4:-}"
  BATCH_NAMES+=("$name")
  BATCH_STATUS+=("$status")
  BATCH_NOTES+=("$notes")
  BATCH_HTML_LINKS+=("$html_link")
  BATCH_COUNT+=1
}

# Repeat a character N times without external deps
repeat_char() {
  local ch="$1"; local n="$2"; local out=""
  while (( n > 0 )); do out+="$ch"; ((n--)); done
  printf '%s' "$out"
}

print_batch_report() {
  local domain="$1" ts="$2"
  local header_name="Application Name" header_status="Status" header_notes="Notes"
  local name_w=${#header_name} status_w=${#header_status} notes_w=${#header_notes}
  local i name status notes
  for (( i=0; i<BATCH_COUNT; i++ )); do
    name="${BATCH_NAMES[$i]}"; status="${BATCH_STATUS[$i]}"; notes="${BATCH_NOTES[$i]}"
    (( ${#name} > name_w )) && name_w=${#name}
    (( ${#status} > status_w )) && status_w=${#status}
    (( ${#notes} > notes_w )) && notes_w=${#notes}
  done
  # Limit notes width growth for readability in terminals
  (( notes_w > 100 )) && notes_w=100

  local border="+$(repeat_char '-' $((name_w+2)))+$(repeat_char '-' $((status_w+2)))+$(repeat_char '-' $((notes_w+2)))+"
  local fmt="| %-$(printf %s "$name_w")s | %-$(printf %s "$status_w")s | %-$(printf %s "$notes_w")s |\n"
  local out_file="${OUTPUT_DIR}/batch-report-${domain}-${ts}.txt"
  local html_file="${OUTPUT_DIR}/batch-report-${domain}-${ts}.html"

  # Truncate file
  : > "$out_file"

  # Print header border
  printf '%s\n' "$border"
  printf '%s\n' "$border" >> "$out_file"

  # Header row
  local line
  printf -v line "$fmt" "$header_name" "$header_status" "$header_notes"
  printf '%s' "$line"
  printf '%s' "$line" >> "$out_file"

  # Separator
  printf '%s\n' "$border"
  printf '%s\n' "$border" >> "$out_file"

  # Rows
  for (( i=0; i<BATCH_COUNT; i++ )); do
    name="${BATCH_NAMES[$i]}"; status="${BATCH_STATUS[$i]}"; notes="${BATCH_NOTES[$i]}"
    local shown_notes="$notes"
    if (( ${#shown_notes} > notes_w )); then
      shown_notes="${shown_notes:0:$(($notes_w-3))}..."
    fi
    printf -v line "$fmt" "$name" "$status" "$shown_notes"
    printf '%s' "$line"
    printf '%s' "$line" >> "$out_file"
  done

  # Footer border
  printf '%s\n' "$border"
  printf '%s\n' "$border" >> "$out_file"
  log "Batch report saved: $out_file"
  generate_batch_html_report "$domain" "$ts" "$html_file"
}

generate_batch_html_report() {
  local domain="$1" ts="$2" out_file="$3"
  local ts_fmt
  ts_fmt="$(date '+%Y-%m-%d %H:%M:%S')"

  # Count totals
  local total=$BATCH_COUNT r=0 c=0 v=0 b=0 s=0 e=0 o=0
  local i
  for (( i=0; i<BATCH_COUNT; i++ )); do
    case "${BATCH_STATUS[$i]}" in
      READY)    (( r++ )) ;;
      CAUTION)  (( c++ )) ;;
      REVIEW)   (( v++ )) ;;
      BLOCKED)  (( b++ )) ;;
      DEPLOYED) (( r++ )) ;;
      EXPORTED|UPLOADED) (( s++ )) ;;
      SKIPPED|OMITTED)   (( o++ )) ;;
      ERROR)    (( e++ )) ;;
    esac
  done

  cat > "$out_file" <<BATCHHTML
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>BW5 Platform Portability — Batch Summary: ${domain}</title>
<style>
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;margin:0;background:#f5f7fa;color:#222}
.hdr{background:#1a2b4a;color:#fff;padding:24px 32px}
.hdr h1{margin:0 0 4px;font-size:1.4rem;font-weight:600}
.hdr p{margin:0;opacity:.7;font-size:.85rem}
.body{padding:24px 32px;max-width:1200px;margin:0 auto}
.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:28px}
.card{background:#fff;border-radius:8px;padding:14px 18px;box-shadow:0 1px 3px rgba(0,0,0,.1);text-align:center}
.num{font-size:2rem;font-weight:700}
.num.ok{color:#166534}.num.bl{color:#dc2626}.num.ca{color:#d97706}.num.er{color:#7f1d1d}
.lbl{font-size:.75rem;text-transform:uppercase;letter-spacing:.05em;opacity:.6;margin-top:4px}
section{background:#fff;border-radius:8px;box-shadow:0 1px 3px rgba(0,0,0,.08);margin-bottom:20px;overflow:hidden}
h2{margin:0;padding:14px 20px;font-size:.95rem;font-weight:600;background:#f8fafc;border-bottom:1px solid #e2e8f0}
table{width:100%;border-collapse:collapse;font-size:.88rem}
th{background:#f8fafc;padding:10px 16px;text-align:left;font-weight:600;border-bottom:1px solid #e2e8f0}
td{padding:10px 16px;border-bottom:1px solid #f1f5f9;vertical-align:middle}
tr:last-child td{border-bottom:none}
.badge{display:inline-block;padding:3px 10px;border-radius:12px;font-size:.8rem;font-weight:600}
.s-READY,.s-DEPLOYED{background:#d1fae5;color:#065f46}
.s-CAUTION{background:#fef3c7;color:#92400e}
.s-REVIEW{background:#dbeafe;color:#1e40af}
.s-BLOCKED{background:#fee2e2;color:#991b1b}
.s-EXPORTED,.s-UPLOADED{background:#e0e7ff;color:#3730a3}
.s-SKIPPED,.s-OMITTED{background:#f3f4f6;color:#6b7280}
.s-ERROR{background:#fef2f2;color:#b91c1c}
a{color:#1d4ed8;text-decoration:none}a:hover{text-decoration:underline}
.foot{margin-top:32px;font-size:.78rem;color:#999;text-align:center;padding-bottom:24px}
</style>
</head>
<body>
<div class="hdr">
  <h1>BW5 Platform Portability — Batch Summary</h1>
  <p>Domain: <strong>${domain}</strong> &nbsp;|&nbsp; ${total} application(s) &nbsp;|&nbsp; Generated: ${ts_fmt}</p>
</div>
<div class="body">
  <div class="grid">
    <div class="card"><div class="num ok">${r}</div><div class="lbl">Ready / Deployed</div></div>
    <div class="card"><div class="num ca">${b}</div><div class="lbl">Blocked</div></div>
    <div class="card"><div class="num ca" style="color:#d97706">${c}</div><div class="lbl">Caution / Review</div></div>
    <div class="card"><div class="num er">${e}</div><div class="lbl">Errors / Skipped</div></div>
  </div>
  <section>
    <h2>Applications (${total})</h2>
    <table>
      <tr><th>Application</th><th>Status</th><th>Notes</th><th>Report</th></tr>
BATCHHTML

  for (( i=0; i<BATCH_COUNT; i++ )); do
    local name="${BATCH_NAMES[$i]}"
    local status="${BATCH_STATUS[$i]}"
    local notes="${BATCH_NOTES[$i]}"
    local link="${BATCH_HTML_LINKS[$i]}"
    local esc_name esc_notes link_cell
    esc_name=$(_html_esc "$name")
    esc_notes=$(_html_esc "$notes")
    if [[ -n "$link" ]]; then
      link_cell="<a href=\"$(_html_esc "$link")\">view report</a>"
    else
      link_cell="<span style=\"color:#9ca3af\">—</span>"
    fi
    printf '      <tr><td>%s</td><td><span class="badge s-%s">%s</span></td><td>%s</td><td>%s</td></tr>\n' \
      "$esc_name" "$status" "$status" "$esc_notes" "$link_cell" >> "$out_file"
  done

  cat >> "$out_file" <<BATCHHTML
    </table>
  </section>
  <div class="foot">Generated by bw5ToCE.sh v${VERSION} &nbsp;|&nbsp; TIBCO BusinessWorks 5 Platform Portability Toolkit</div>
</div>
</body>
</html>
BATCHHTML
  printf 'Batch HTML summary: %s\n' "$out_file"
  log "Batch HTML summary written: $out_file"
}

# ==========================================
# Platform Portability Analysis
# ==========================================

# Findings: each entry is tab-separated "FILE\tITEM\tDESCRIPTION"
ANALYSIS_BLOCKERS=()
ANALYSIS_WARNINGS=()
ANALYSIS_NOTES=()
ANALYSIS_QUALITY=()

# Supported core BW5 activity type prefixes (always in the base image)
BWCE_CORE_PREFIXES=(
  "com.tibco.bw."
  "com.tibco.pe."
  "com.tibco.plugin.file."
  "com.tibco.plugin.ftp."
  "com.tibco.plugin.soap."
  "com.tibco.plugin.http."
  "com.tibco.plugin.jdbc."
  "com.tibco.plugin.jms."
  "com.tibco.plugin.ems."
  "com.tibco.plugin.mail."
  "com.tibco.plugin.rendezvous."
  "com.tibco.plugin.timer."
  "com.tibco.plugin.java."
  "com.tibco.plugin.xml."
  "com.tibco.plugin.xslt."
  "com.tibco.plugin.mapper."
  "com.tibco.plugin.generalactivities."
  "com.tibco.plugin.shared."
  "com.tibco.plugin.noop."
  "com.tibco.plugin.log."
  "com.tibco.plugin.parse."
)

# Supported additional adapters/plugins per the supported list.
# Prefixes taken from the canonical PluginActivityMap in the extractor package.
BWCE_PLUGIN_PREFIXES=(
  "com.tibco.plugin.ae."             # Adapter Engine (AE) framework activities
  "com.tibco.plugin.adb."
  "com.tibco.plugin.sap."
  "com.tibco.plugin.filesadapter."
  "com.tibco.plugin.ae.fileadapter."
  "com.tibco.plugin.siebel."
  "com.tibco.plugin.ldap."
  "com.tibco.plugin.sp."            # SFTP (com.tibco.plugin.sp.SFTP*)
  "com.tibco.plugin.bwlx."          # Large XML
  "com.tibco.plugin.json."           # REST/JSON (com.tibco.plugin.json.activities.*)
  "com.tibco.plugin.restjson."       # REST/JSON (legacy namespace)
  "com.tibco.bw.palette.rest."
  "com.tibco.plugin.salesforce."
  "com.tibco.plugin.mongodb."
  "com.tibco.plugin.kafka."
  "com.tibco.plugin.pulsar."
  "com.tibco.plugin.ax.bc."          # B2B Connector (com.tibco.plugin.ax.bc.*)
  "com.tibco.plugin.iProcessForms."  # iProcess
  "com.tibco.plugin.staffware."      # iProcess (alternate namespace)
  "com.tibco.plugin.dataconversion."
  "com.tibco.plugin.bwmq."           # IBM MQ (com.tibco.plugin.bwmq.*)
  "com.tibco.plugin.workday."
  "com.tibco.plugin.oracleebs."      # Oracle E-Business Suite
  "com.tibco.plugin.pdf."
  "com.tibco.plugin.sharepoint."
  "com.tibco.swift2.bwplugin."       # SWIFT (com.tibco.swift2.bwplugin.swiftcheck/swiftmxcheck.*)
)

# Adapters and plugins explicitly known to be unsupported in BWCE.
# Prefixes taken from the canonical PluginActivityMap in the extractor package.
# Parallel arrays: index N in PREFIXES maps to index N in LABELS.
# Note: JD Edwards, PeopleSoft, OSIsoft PI, and Tuxedo are pure Adapter SDK resources
# with no <pd:type> entries — they are detected via *.aar scanning instead (see below).
BWCE_KNOWN_UNSUPPORTED_PREFIXES=(
  # Plugins
  "com.tibco.plugin.ejb."                     # EJB
  "com.tibco.plugin.bwmi."                    # Mobile Integration
  "com.tibco.plugin.netsuite."                # NetSuite
  "com.tibco.solution.xref.plugin.activity."  # SmartMapper
  "com.tibco.plugin.firefly.activities."      # ActiveSpaces 1/2
  # Mainframe
  "com.tibco.plugin.cicspi."                  # CICS
  "com.tibco.plugin.hl7."                     # HL7
)
BWCE_KNOWN_UNSUPPORTED_LABELS=(
  "EJB Plugin"
  "Mobile Integration Plugin"
  "NetSuite Plugin"
  "SmartMapper Plugin"
  "ActiveSpaces 1/2 Plugin"
  "CICS Mainframe Plugin"
  "HL7 Plugin"
)

# ── Adapter (AAR) detection tables ───────────────────────────────────────────
# AAR files live at the EAR root and contain a TIBCO.xml with:
#   <componentSoftwareName>VALUE</componentSoftwareName>
# that identifies the adapter type.
#
# The VALUE confirmed from real sample EARs:
#   adb  → confirmed from ProjADB732rpc sample
# Other values are inferred from PluginAdapterMap keys (short form / full form).
# Update these when additional adapter samples become available.
#
# Supported adapter componentSoftwareName values:
BWCE_AAR_SUPPORTED_NAMES=(
  "adb"     "adadb"          # ADB (confirmed: adb)
  "r3"      "adr3"           # SAP R/3
  "ldap"    "adldap"         # LDAP
  "sbl"     "adsbl"          # Siebel
  "files"   "adfiles"        # Files Adapter
  "as400"   "adas400"        # AS/400
  "adapter_sdk"              # Generic Adapter SDK
)

# Unsupported adapter componentSoftwareName values (parallel arrays):
BWCE_AAR_UNSUPPORTED_NAMES=(
  "jdexe"   "adjdexe"        # JD Edwards
  "psft8"   "adpsft8"        # PeopleSoft
  "pi"      "adpi"           # OSIsoft PI
  "tuxedo"  "adtuxedo"       # Tuxedo
)
BWCE_AAR_UNSUPPORTED_LABELS=(
  "JD Edwards Adapter"  "JD Edwards Adapter"
  "PeopleSoft Adapter"  "PeopleSoft Adapter"
  "OSIsoft PI Adapter"  "OSIsoft PI Adapter"
  "Tuxedo Adapter"      "Tuxedo Adapter"
)

# Returns the display label for a known-unsupported type prefix, or 1 if unknown.
_analysis_known_unsupported_label() {
  local t="$1" i
  for i in "${!BWCE_KNOWN_UNSUPPORTED_PREFIXES[@]}"; do
    [[ "$t" == "${BWCE_KNOWN_UNSUPPORTED_PREFIXES[$i]}"* ]] \
      && printf '%s' "${BWCE_KNOWN_UNSUPPORTED_LABELS[$i]}" && return 0
  done
  return 1
}

_analysis_add_blocker()  { ANALYSIS_BLOCKERS+=("$1"$'\t'"$2"$'\t'"$3"); }
_analysis_add_warning()  { ANALYSIS_WARNINGS+=("$1"$'\t'"$2"$'\t'"$3"); }
_analysis_add_note()     { ANALYSIS_NOTES+=("$1"$'\t'"$2"$'\t'"$3"); }
_analysis_add_quality()  { ANALYSIS_QUALITY+=("$1"$'\t'"$2"$'\t'"$3"); }

# Returns the trimmed value when it looks hardcoded (non-empty, no GV reference).
# BW5 Global Variable references use the %%VAR_NAME%% syntax in resource files.
_analysis_hardcoded_val() {
  local val
  val=$(printf '%s' "$1" | tr -d '[:space:]')
  if [[ -n "$val" && "$val" != *'%%'* ]]; then printf '%s' "$val"; fi
}

_analysis_is_supported_type() {
  local t="$1" p
  for p in "${BWCE_CORE_PREFIXES[@]}" "${BWCE_PLUGIN_PREFIXES[@]}"; do
    [[ "$t" == "$p"* ]] && return 0
  done
  return 1
}

# Check TIBCO.xml to infer if checkpoint storage is database-driven.
# Prints "true" if BWDatabase* vars with "Checkpoint Data Repository" description are found.
_analysis_check_tibco_xml() {
  local tibco_xml="$1"
  [[ -f "$tibco_xml" ]] || { printf 'false'; return; }
  if grep -qi 'Checkpoint Data Repository\|bw\.checkpoint\|CheckpointDatabase' "$tibco_xml" 2>/dev/null; then
    printf 'true'
  else
    printf 'false'
  fi
}

# Scan a single .process file for portability considerations.
# $1 = process file path
# $2 = "true" if checkpoint appears DB-backed (from TIBCO.xml analysis)
_analysis_scan_process() {
  local pfile="$1"
  local has_db_checkpoint="${2:-false}"
  local fname
  fname="$(basename "$pfile")"

  # Collect all activity type values: <pd:type>TYPENAME</pd:type>
  local types
  types=$(grep -oE '<pd:type>[^<]+</pd:type>' "$pfile" 2>/dev/null \
    | sed 's/<pd:type>//g; s/<\/pd:type>//g' || true)

  [[ -z "$types" ]] && return 0

  # --- BLOCKER: HTTP Basic Auth (server-mode) ---
  # HTTPEventSource (HTTPReceiver) with useHTTPAuthentication=true
  if echo "$types" | grep -q 'com.tibco.plugin.http.HTTPEventSource'; then
    if grep -q '<useHTTPAuthentication>true</useHTTPAuthentication>' "$pfile" 2>/dev/null; then
      _analysis_add_blocker "$fname" "HTTP Basic Auth — HTTPReceiver" \
        "HTTP Basic Auth in BW5 Classic relies on TIBCO Administrator domain users, which are not available in the Containers runtime. Cloud-native best practice is to externalize authentication outside the engine, using API Gateway, Ingress, or TIBCO Cloud API Management instead."
    fi
  fi
  # SOAPEventSource with useBasicAuthentication=true
  if echo "$types" | grep -q 'com.tibco.plugin.soap.SOAPEventSource'; then
    if grep -q '<useBasicAuthentication>true</useBasicAuthentication>' "$pfile" 2>/dev/null; then
      _analysis_add_blocker "$fname" "HTTP Basic Auth — SOAPEventSource" \
        "HTTP Basic Auth in BW5 Classic relies on TIBCO Administrator domain users, which are not available in the Containers runtime. Cloud-native best practice is to externalize authentication outside the engine, using API Gateway, Ingress, or TIBCO Cloud API Management instead."
    fi
  fi

  # --- BLOCKER: Unsupported activity types ---
  local type_ref
  while IFS= read -r type_ref; do
    type_ref="$(trim_spaces "$type_ref")"
    [[ -z "$type_ref" ]] && continue
    # Only flag com.tibco.* namespaces; ignore blanks, loop types, etc.
    [[ "$type_ref" != com.tibco.* ]] && continue
    if ! _analysis_is_supported_type "$type_ref"; then
      local _label
      if _label=$(_analysis_known_unsupported_label "$type_ref"); then
        _analysis_add_blocker "$fname" "Not Yet Available: $_label" \
          "This plugin is not yet available in this version of TIBCO BusinessWorks 5 (Containers). TIBCO is continuously expanding the platform's capabilities — please contact your TIBCO representative for detailed timelines or check for availability in a future release."
      else
        _analysis_add_blocker "$fname" "Not Yet Available: $type_ref" \
          "Activity type '$type_ref' is not yet available in this version of TIBCO BusinessWorks 5 (Containers). TIBCO is continuously expanding the platform's capabilities — please contact your TIBCO representative for detailed timelines or check for availability in a future release."
      fi
    fi
  done <<< "$types"

  # --- WARNING: Wait & Notify ---
  local wait_notify
  wait_notify=$(echo "$types" | grep -iE 'WaitForNotif|WaitNotif|NotifyActivity' || true)
  if [[ -n "$wait_notify" ]]; then
    _analysis_add_warning "$fname" "Wait & Notify — Review Scope" \
      "Wait/Notify pattern detected. If the scope is single-instance this works as expected. For cross-instance scenarios, please review the design: TIBCO BusinessWorks 5 (Containers) follows standard Kubernetes practices where each instance is independent, and there is no out-of-the-box inter-instance communication for this feature."
  fi

  # --- WARNING: Checkpoint ---
  if echo "$types" | grep -q 'CheckpointActivity'; then
    if [[ "$has_db_checkpoint" == "true" ]]; then
      _analysis_add_warning "$fname" "Checkpoint — DB Storage" \
        "Checkpoint with database storage detected. This is fully supported in TIBCO BusinessWorks 5 (Containers). We recommend validating behavior under autoscaling and multi-replica deployments to ensure checkpoint consistency."
    else
      _analysis_add_warning "$fname" "Checkpoint — File Storage" \
        "Checkpoint detected without database storage. File-based checkpoint storage requires additional persistent storage such as a PersistentVolumeClaim (PVC) and volume mount. We recommend switching to a JDBC-based Checkpoint Data Repository for the best experience in a containerized environment."
    fi
  fi

  # --- WARNING: Module Shared Variable usage (scope check done via shared resources) ---
  # We flag the process if it references a .moduleSharedVariable resource
  local var_refs
  var_refs=$(grep -oE '<variableConfig>[^<]+</variableConfig>' "$pfile" 2>/dev/null \
    | sed 's/<variableConfig>//g; s/<\/variableConfig>//g' || true)
  if echo "$var_refs" | grep -q '\.moduleSharedVariable\|\.sharedvariable'; then
    _analysis_add_warning "$fname" "Module Shared Variable — Review Scope" \
      "Module Shared Variable referenced. If the scope is single-instance this works as expected. For cross-instance scenarios, please review the design: consider switching to a DB-persisted Shared Variable, following similar principles as Checkpoint storage."
  fi

  # --- WARNING: Engine Command Activity (operational lifecycle commands) ---
  # Only flag commands that interact with the BW5 Classic runtime lifecycle;
  # operational-stats commands (GetActivityStats, etc.) are safe to ignore.
  if echo "$types" | grep -q 'com\.tibco\.pe\.core\.EngineCommandActivity'; then
    local flagged_cmds
    flagged_cmds=$(grep -oE '<command>(Shutdown|SuspendProcessInstance|SuspendProcessStarter|ResumeProcessInstance|ResumeProcessStarter)</command>' "$pfile" 2>/dev/null \
      | sed 's/<command>//g; s/<\/command>//g' | sort -u | tr '\n' ',' | sed 's/,$//' || true)
    if [[ -n "$flagged_cmds" ]]; then
      _analysis_add_warning "$fname" "Engine Command — Operational Lifecycle Review" \
        "Engine Command Activity with lifecycle operations ($flagged_cmds) detected. In BW5 Classic, these commands are typically triggered by external operational tooling such as Hawk Microagents or RedTail. In TIBCO BusinessWorks 5 (Containers), runtime lifecycle management is handled natively by Kubernetes and the TIBCO Platform — through pod lifecycle management, health probes, and the Control Plane. Review any operational workflows or tooling that rely on these commands and align them with the cloud-native management capabilities of TIBCO BusinessWorks 5 (Containers)."
    fi
  fi

  # --- WARNING: External Command Activity ---
  if echo "$types" | grep -q 'com\.tibco\.plugin\.generalactivities\.ExternalCommandActivity'; then
    _analysis_add_warning "$fname" "External Command Activity — Review Base Image" \
      "External Command Activity detected. These activities execute OS-level commands and rely on binaries being available inside the container image. The TIBCO BusinessWorks 5 (Containers) base image may not include all required commands or utilities. Review each External Command Activity and verify that the required binaries are present in the base image, or plan for a custom base image that includes the additional dependencies."
  fi

  # --- NOTE: File I/O ---
  local file_types
  file_types=$(echo "$types" | grep 'com\.tibco\.plugin\.file\.' || true)
  if [[ -n "$file_types" ]]; then
    local ops
    ops=$(echo "$file_types" | sed 's/com\.tibco\.plugin\.file\.//g' | tr '\n' ',' | sed 's/,$//')
    _analysis_add_note "$fname" "File I/O — Review Storage Design" \
      "File system activities ($ops) detected. Temporary or internal files work as expected, though container storage is ephemeral. Read-only content may require a volume mount. If files need to be shared outside the application scope or with other parties, review the design: a PersistentVolumeClaim (PVC) or object storage (e.g., S3 via REST) may be required."
  fi

  # --- NOTE: Rendezvous ---
  if echo "$types" | grep -q 'com\.tibco\.plugin\.rendezvous\.'; then
    _analysis_add_note "$fname" "TIBCO Rendezvous — Review Deployment Design" \
      "TIBCO Rendezvous activities detected. Test carefully, as RV in a cloud environment may require TIBCO TRNS software or additional configuration. Re-evaluate the design to determine if it can be replaced with another TIBCO Messaging alternative such as TIBCO EMS or TIBCO Cloud Messaging, if needed."
  fi

  # --- NOTE: Fault Tolerant Group ---
  if grep -qiE 'FaultTolerant|ftgroup|FTGroup' "$pfile" 2>/dev/null; then
    _analysis_add_note "$fname" "Fault Tolerant Group — Cloud-Native HA" \
      "Fault Tolerant Group reference detected. TIBCO BusinessWorks 5 (Containers) leverages Kubernetes built-in high availability through Deployment replicas, health probes, and self-healing — providing equivalent resilience natively. Review your design to take full advantage of these cloud-native HA capabilities."
  fi

  # ── Best Practice checks ──────────────────────────────────────────────────

  # --- QUALITY: No process description ---
  local proc_desc
  proc_desc=$(grep -oE '<pd:description>[^<]*</pd:description>' "$pfile" 2>/dev/null \
    | sed 's/<pd:description>//g; s/<\/pd:description>//g' | head -1 | tr -d '[:space:]' || true)
  if [[ -z "$proc_desc" ]]; then
    _analysis_add_quality "$fname" "No Process Description" \
      "The process has no description. Adding a meaningful description improves maintainability and helps teams quickly understand the process purpose and business context."
  fi

  # --- QUALITY: No catch-all error handler ---
  if ! grep -qE '<catchAll>true</catchAll>' "$pfile" 2>/dev/null; then
    _analysis_add_quality "$fname" "No Catch-All Error Handler" \
      "The process does not include a catch-all error handler. Adding one ensures unexpected exceptions are captured and handled gracefully, improving reliability and observability in production."
  fi

  # --- QUALITY: render-xml with pretty-print ---
  if grep -qE 'tib:render-xml\s*\([^)]+,[^)]+,\s*true\(' "$pfile" 2>/dev/null; then
    _analysis_add_quality "$fname" "render-xml with Pretty-Print" \
      "A render-xml() call uses the pretty-print option (third argument true()). Pretty-printing adds whitespace for readability but has a measurable performance overhead. Disable it in production processes to reduce CPU usage."
  fi
}

# ── Best Practice: shared connection resource scanning ────────────────────────
# Each function receives the path to a single resource file and adds
# ANALYSIS_QUALITY entries for any hardcoded (non-GV) connection values.

_analysis_scan_sharedhttp() {
  local rfile="$1"
  local fname
  fname="$(basename "$rfile")"
  local host port hv pv
  host=$(grep -oE '<Host>[^<]+</Host>' "$rfile" 2>/dev/null \
    | sed 's/<Host>//g; s/<\/Host>//g' | head -1 || true)
  port=$(grep -oE '<Port>[^<]+</Port>' "$rfile" 2>/dev/null \
    | sed 's/<Port>//g; s/<\/Port>//g' | head -1 || true)
  hv=$(_analysis_hardcoded_val "$host")
  pv=$(_analysis_hardcoded_val "$port")
  if [[ -n "$hv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded Host — Shared HTTP" \
      "The host '$hv' is hard-coded in the Shared HTTP resource. Use a Global Variable (%%GV_NAME%%) to allow environment-specific configuration without rebuilding the EAR."
  fi
  if [[ -n "$pv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded Port — Shared HTTP" \
      "The port '$pv' is hard-coded in the Shared HTTP resource. Use a Global Variable (%%GV_NAME%%) to allow environment-specific configuration without rebuilding the EAR."
  fi
}

_analysis_scan_sharedjdbc() {
  local rfile="$1"
  local fname
  fname="$(basename "$rfile")"
  local url user pass uv xv pv
  url=$(grep -oE '<location>[^<]+</location>' "$rfile" 2>/dev/null \
    | sed 's/<location>//g; s/<\/location>//g' | head -1 || true)
  user=$(grep -oE '<user>[^<]+</user>' "$rfile" 2>/dev/null \
    | sed 's/<user>//g; s/<\/user>//g' | head -1 || true)
  pass=$(grep -oE '<password>[^<]+</password>' "$rfile" 2>/dev/null \
    | sed 's/<password>//g; s/<\/password>//g' | head -1 || true)
  uv=$(_analysis_hardcoded_val "$url")
  xv=$(_analysis_hardcoded_val "$user")
  pv=$(_analysis_hardcoded_val "$pass")
  if [[ -n "$uv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded URL — Shared JDBC" \
      "The JDBC URL '$uv' is hard-coded in the Shared JDBC resource. Use a Global Variable to allow environment-specific configuration and seamless deployment across environments."
  fi
  if [[ -n "$xv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded User — Shared JDBC" \
      "The database user is hard-coded in the Shared JDBC resource. Use a Global Variable to keep credentials out of the EAR and allow per-environment configuration."
  fi
  if [[ -n "$pv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded Password — Shared JDBC" \
      "A database password is hard-coded in the Shared JDBC resource. Use a Global Variable and inject the value securely at runtime via Kubernetes Secrets or a secrets manager."
  fi
}

_analysis_scan_sharedjms() {
  local rfile="$1"
  local fname
  fname="$(basename "$rfile")"
  local url user pass uv xv pv
  url=$(grep -oE '<ProviderURL>[^<]+</ProviderURL>' "$rfile" 2>/dev/null \
    | sed 's/<ProviderURL>//g; s/<\/ProviderURL>//g' | head -1 || true)
  user=$(grep -oE '<username>[^<]+</username>' "$rfile" 2>/dev/null \
    | sed 's/<username>//g; s/<\/username>//g' | head -1 || true)
  pass=$(grep -oE '<password>[^<]+</password>' "$rfile" 2>/dev/null \
    | sed 's/<password>//g; s/<\/password>//g' | head -1 || true)
  uv=$(_analysis_hardcoded_val "$url")
  xv=$(_analysis_hardcoded_val "$user")
  pv=$(_analysis_hardcoded_val "$pass")
  if [[ -n "$uv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded URL — Shared JMS" \
      "The JMS provider URL '$uv' is hard-coded in the Shared JMS resource. Use a Global Variable to allow environment-specific configuration without rebuilding the EAR."
  fi
  if [[ -n "$xv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded User — Shared JMS" \
      "The JMS user is hard-coded in the Shared JMS resource. Use a Global Variable to keep credentials out of the EAR and allow per-environment configuration."
  fi
  if [[ -n "$pv" ]]; then
    _analysis_add_quality "$fname" "Hard-coded Password — Shared JMS" \
      "A JMS password is hard-coded in the Shared JMS resource. Use a Global Variable and inject the value securely at runtime via Kubernetes Secrets or a secrets manager."
  fi
}

# Scan all connection resource files (.sharedhttp, .sharedjdbc, .jms) in a directory.
_analysis_scan_connections() {
  local dir="$1"
  local f
  while IFS= read -r -d '' f; do _analysis_scan_sharedhttp "$f"; done \
    < <(find "$dir" -name "*.sharedhttp" -print0 2>/dev/null)
  while IFS= read -r -d '' f; do _analysis_scan_sharedjdbc "$f"; done \
    < <(find "$dir" -name "*.sharedjdbc" -print0 2>/dev/null)
  while IFS= read -r -d '' f; do _analysis_scan_sharedjms "$f"; done \
    < <(find "$dir" -name "*.jms" -print0 2>/dev/null)
}

# Scan all shared resource files extracted from a SAR
_analysis_scan_shared_resources() {
  local res_dir="$1"
  local rfile fname resource_type
  while IFS= read -r -d '' rfile; do
    fname="$(basename "$rfile")"
    resource_type=$(grep -oE '<resourceType>[^<]+</resourceType>' "$rfile" 2>/dev/null \
      | sed 's/<resourceType>//g; s/<\/resourceType>//g' | head -1 || true)

    # Module Shared Variable without DB persistence
    if [[ "$resource_type" == "ae.shared.moduleSharedVariable" ]]; then
      local persistence
      persistence=$(grep -oE '<persistence>[^<]+</persistence>' "$rfile" 2>/dev/null \
        | sed 's/<persistence>//g; s/<\/persistence>//g' | head -1 || true)
      if [[ "$persistence" != "database" && "$persistence" != "jdbc" ]]; then
        _analysis_add_warning "$fname" "Module Shared Variable — Non-DB Persistence" \
          "Module Shared Variable with non-database persistence ('${persistence:-none/default}') detected. File-based storage requires a PersistentVolumeClaim (PVC) and volume mount. For cross-instance sharing, switching to JDBC-based persistence is recommended to ensure consistency across replicas."
      fi
    fi
  done < <(find "$res_dir" \( \
    -name "*.moduleSharedVariable" -o \
    -name "*.sharedvariable" \
    \) -print0 2>/dev/null)
}

# Scan a single .aar (Adapter Archive) at the EAR root.
# Extracts componentSoftwareName from the AAR's TIBCO.xml and flags
# unsupported adapters as BLOCKERs.
_analysis_scan_aar() {
  local aar_file="$1"
  local work_dir="$2"
  local fname
  fname="$(basename "$aar_file")"

  local aar_dir="$work_dir/$(basename "$aar_file").analysis.d"
  mkdir -p "$aar_dir"
  unzip -qo "$aar_file" -d "$aar_dir" 2>/dev/null || return 0

  local tibco_xml="$aar_dir/TIBCO.xml"
  [[ -f "$tibco_xml" ]] || return 0

  local sw_name
  sw_name=$(grep -oE '<componentSoftwareName>[^<]+</componentSoftwareName>' "$tibco_xml" 2>/dev/null \
    | sed 's/<componentSoftwareName>//g; s/<\/componentSoftwareName>//g' | head -1 || true)
  [[ -z "$sw_name" ]] && return 0

  # Check known-unsupported adapters
  local i
  for i in "${!BWCE_AAR_UNSUPPORTED_NAMES[@]}"; do
    if [[ "$sw_name" == "${BWCE_AAR_UNSUPPORTED_NAMES[$i]}" ]]; then
      _analysis_add_blocker "$fname" "Not Yet Available: ${BWCE_AAR_UNSUPPORTED_LABELS[$i]}" \
        "Adapter '${BWCE_AAR_UNSUPPORTED_LABELS[$i]}' is not yet available in this version of TIBCO BusinessWorks 5 (Containers). TIBCO is continuously expanding the platform's capabilities — please contact your TIBCO representative for detailed timelines or check for availability in a future release."
      return 0
    fi
  done

  # Check known-supported adapters (no action needed)
  local sup
  for sup in "${BWCE_AAR_SUPPORTED_NAMES[@]}"; do
    [[ "$sw_name" == "$sup" ]] && return 0
  done

  # Unknown adapter — report for investigation
  _analysis_add_blocker "$fname" "Not Yet Available: $sw_name" \
    "Adapter '$sw_name' availability in TIBCO BusinessWorks 5 (Containers) could not be verified. TIBCO is continuously expanding the platform's capabilities — please contact your TIBCO representative or check for availability in a future release."
}

# Scan a single .serviceagent file for HTTP Basic Auth (server-mode).
_analysis_scan_service_agent() {
  local safile="$1"
  local fname
  fname="$(basename "$safile")"
  if grep -q '<useBasicAuthentication>true</useBasicAuthentication>' "$safile" 2>/dev/null; then
    _analysis_add_blocker "$fname" "HTTP Basic Auth — ServiceAgent" \
      "HTTP Basic Auth in BW5 Classic relies on TIBCO Administrator domain users, which are not available in the Containers runtime. Cloud-native best practice is to externalize authentication outside the engine, using API Gateway, Ingress, or TIBCO Cloud API Management instead."
  fi
}

# Main analysis driver: extracts EAR, scans all processes and shared resources.
# Populates ANALYSIS_BLOCKERS, ANALYSIS_WARNINGS, ANALYSIS_NOTES.
analyze_ear_for_portability() {
  local ear_file="$1"
  local work_dir="$2"

  ANALYSIS_BLOCKERS=()
  ANALYSIS_WARNINGS=()
  ANALYSIS_NOTES=()
  ANALYSIS_QUALITY=()

  require_bin unzip

  local ear_dir="$work_dir/analysis_ear"
  mkdir -p "$ear_dir"
  unzip -qo "$ear_file" -d "$ear_dir" 2>/dev/null \
    || { err "Cannot extract EAR for analysis: $ear_file"; return 1; }

  # Infer checkpoint storage type from TIBCO.xml
  local has_db_checkpoint
  has_db_checkpoint=$(_analysis_check_tibco_xml "$ear_dir/TIBCO.xml")

  # Process each PAR (process archive)
  local par_file par_dir process_file
  while IFS= read -r -d '' par_file; do
    par_dir="${par_file}.analysis.d"
    mkdir -p "$par_dir"
    unzip -qo "$par_file" -d "$par_dir" 2>/dev/null || continue
    log "Analysis: scanning $(basename "$par_file")"
    while IFS= read -r -d '' process_file; do
      _analysis_scan_process "$process_file" "$has_db_checkpoint"
    done < <(find "$par_dir" -name "*.process" -print0 2>/dev/null)
    local agent_file
    while IFS= read -r -d '' agent_file; do
      _analysis_scan_service_agent "$agent_file"
    done < <(find "$par_dir" -name "*.serviceagent" -print0 2>/dev/null)
    _analysis_scan_connections "$par_dir"
  done < <(find "$ear_dir" -name "*.par" -print0 2>/dev/null)

  # Process each AAR (adapter archive) at the EAR root
  local aar_file
  while IFS= read -r -d '' aar_file; do
    log "Analysis: scanning adapter $(basename "$aar_file")"
    _analysis_scan_aar "$aar_file" "$ear_dir"
  done < <(find "$ear_dir" -maxdepth 1 -name "*.aar" -print0 2>/dev/null)

  # Process each SAR (shared archive)
  local sar_file sar_dir
  while IFS= read -r -d '' sar_file; do
    sar_dir="${sar_file}.analysis.d"
    mkdir -p "$sar_dir"
    unzip -qo "$sar_file" -d "$sar_dir" 2>/dev/null || continue
    log "Analysis: scanning shared resources in $(basename "$sar_file")"
    _analysis_scan_shared_resources "$sar_dir"
    _analysis_scan_connections "$sar_dir"
  done < <(find "$ear_dir" -name "*.sar" -print0 2>/dev/null)

  log "Analysis complete: ${#ANALYSIS_BLOCKERS[@]} blockers, ${#ANALYSIS_WARNINGS[@]} warnings, ${#ANALYSIS_NOTES[@]} notes, ${#ANALYSIS_QUALITY[@]} quality"
}

print_analysis_summary() {
  local app_name="$1"
  local b_count=${#ANALYSIS_BLOCKERS[@]}
  local w_count=${#ANALYSIS_WARNINGS[@]}
  local n_count=${#ANALYSIS_NOTES[@]}
  local q_count=${#ANALYSIS_QUALITY[@]}
  local hr
  hr="$(repeat_char '─' 72)"

  printf '\n%s\n' "$hr"
  printf ' PLATFORM PORTABILITY: %s\n' "$app_name"
  printf '%s\n' "$hr"

  if (( b_count == 0 && w_count == 0 && n_count == 0 )); then
    printf ' STATUS: READY — No portability considerations detected\n'
  elif (( b_count > 0 )); then
    printf ' STATUS: BLOCKED — %d blocker(s) must be resolved before transitioning\n' "$b_count"
  elif (( w_count > 0 )); then
    printf ' STATUS: CAUTION — %d behavior change(s) to review\n' "$w_count"
  else
    printf ' STATUS: REVIEW — %d note(s) for cloud-native adaptation\n' "$n_count"
  fi

  if (( q_count > 0 )); then
    printf ' QUALITY: %d best-practice suggestion(s) — see Best Practices section\n' "$q_count"
  fi
  printf '%s\n' "$hr"

  if (( b_count == 0 && w_count == 0 && n_count == 0 && q_count == 0 )); then
    printf '\n'
    return 0
  fi

  local entry file item desc

  if (( b_count > 0 )); then
    printf '\n BLOCKERS (%d) — must resolve before deploying to containers:\n\n' "$b_count"
    for entry in "${ANALYSIS_BLOCKERS[@]}"; do
      IFS=$'\t' read -r file item desc <<< "$entry"
      printf '  [B] %-40s  %s\n      %s\n\n' "$item" "$file" "$desc"
    done
  fi

  if (( w_count > 0 )); then
    printf ' WARNINGS (%d) — behavior differs in containers:\n\n' "$w_count"
    for entry in "${ANALYSIS_WARNINGS[@]}"; do
      IFS=$'\t' read -r file item desc <<< "$entry"
      printf '  [W] %-40s  %s\n      %s\n\n' "$item" "$file" "$desc"
    done
  fi

  if (( n_count > 0 )); then
    printf ' NOTES (%d) — items to review for cloud-native deployment:\n\n' "$n_count"
    for entry in "${ANALYSIS_NOTES[@]}"; do
      IFS=$'\t' read -r file item desc <<< "$entry"
      printf '  [N] %-40s  %s\n      %s\n\n' "$item" "$file" "$desc"
    done
  fi

  if (( q_count > 0 )); then
    printf ' BEST PRACTICES (%d) — quality improvements for cloud-native deployment:\n\n' "$q_count"
    for entry in "${ANALYSIS_QUALITY[@]}"; do
      IFS=$'\t' read -r file item desc <<< "$entry"
      printf '  [Q] %-40s  %s\n      %s\n\n' "$item" "$file" "$desc"
    done
  fi

  printf '%s\n\n' "$hr"
}

generate_cli_report() {
  local app_name="$1"
  local report_file="${2:-}"
  if [[ -z "$report_file" ]]; then
    print_analysis_summary "$app_name"
  else
    print_analysis_summary "$app_name" > "$report_file"
    printf 'CLI report: %s\n' "$report_file"
    log "CLI report written: $report_file"
  fi
}

_html_esc() {
  local s="$1"
  s="${s//&/&amp;}"; s="${s//</&lt;}"; s="${s//>/&gt;}"; s="${s//\"/&quot;}"
  printf '%s' "$s"
}

generate_html_report() {
  local app_name="$1"
  local report_file="$2"
  local b_count=${#ANALYSIS_BLOCKERS[@]}
  local w_count=${#ANALYSIS_WARNINGS[@]}
  local n_count=${#ANALYSIS_NOTES[@]}
  local q_count=${#ANALYSIS_QUALITY[@]}
  local ts
  ts="$(date '+%Y-%m-%d %H:%M:%S')"

  local badge_class badge_label
  if   (( b_count > 0 )); then badge_class="blocked"; badge_label="BLOCKED"
  elif (( w_count > 0 )); then badge_class="caution"; badge_label="CAUTION"
  elif (( n_count > 0 )); then badge_class="review";  badge_label="REVIEW"
  else                         badge_class="ready";   badge_label="READY"
  fi

  cat > "$report_file" <<HTMLEOF
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>BW5 Platform Portability: $(printf '%s' "$app_name" | sed 's/</\&lt;/g;s/>/\&gt;/g')</title>
<style>
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;margin:0;background:#f5f7fa;color:#222}
.hdr{background:#1a2b4a;color:#fff;padding:24px 32px}
.hdr h1{margin:0 0 4px;font-size:1.4rem;font-weight:600}
.hdr p{margin:0;opacity:.7;font-size:.85rem}
.body{padding:24px 32px;max-width:1100px;margin:0 auto}
.badge{display:inline-block;padding:6px 20px;border-radius:20px;font-weight:700;font-size:1rem;margin-bottom:20px}
.blocked{background:#fee2e2;color:#991b1b}
.caution{background:#fef9c3;color:#854d0e}
.review{background:#dbeafe;color:#1e40af}
.ready{background:#d1fadf;color:#166534}
.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:28px}
.card{background:#fff;border-radius:8px;padding:16px 20px;box-shadow:0 1px 3px rgba(0,0,0,.1);text-align:center}
.num{font-size:2.2rem;font-weight:700}
.num.b{color:#dc2626}.num.w{color:#d97706}.num.n{color:#2563eb}.num.q{color:#7c3aed}
.lbl{font-size:.78rem;text-transform:uppercase;letter-spacing:.05em;opacity:.6;margin-top:4px}
section{background:#fff;border-radius:8px;box-shadow:0 1px 3px rgba(0,0,0,.08);margin-bottom:20px;overflow:hidden}
h2{margin:0;padding:14px 20px;font-size:.95rem;font-weight:600}
h2.bh{background:#fee2e2;color:#991b1b}
h2.wh{background:#fef9c3;color:#92400e}
h2.nh{background:#eff6ff;color:#1e40af}
h2.qh{background:#f3e8ff;color:#5b21b6}
h2.oh{background:#d1fadf;color:#166534}
table{width:100%;border-collapse:collapse;font-size:.88rem}
th{background:#f8fafc;padding:10px 16px;text-align:left;font-weight:600;border-bottom:1px solid #e2e8f0}
td{padding:10px 16px;border-bottom:1px solid #f1f5f9;vertical-align:top}
tr:last-child td{border-bottom:none}
.bi td:first-child{color:#dc2626;font-weight:600}
.wi td:first-child{color:#b45309;font-weight:600}
.ni td:first-child{color:#1d4ed8;font-weight:600}
.qi td:first-child{color:#6d28d9;font-weight:600}
.foot{margin-top:32px;font-size:.78rem;color:#999;text-align:center;padding-bottom:24px}
</style>
</head>
<body>
<div class="hdr">
  <h1>BW5 Platform Portability Report</h1>
  <p>Application: <strong>$(printf '%s' "$app_name" | sed 's/</\&lt;/g;s/>/\&gt;/g')</strong> &nbsp;|&nbsp; Generated: ${ts}</p>
</div>
<div class="body">
  <div class="badge ${badge_class}">${badge_label}</div>
  <div class="grid">
    <div class="card"><div class="num b">${b_count}</div><div class="lbl">Blockers</div></div>
    <div class="card"><div class="num w">${w_count}</div><div class="lbl">Warnings</div></div>
    <div class="card"><div class="num n">${n_count}</div><div class="lbl">Notes</div></div>
    <div class="card"><div class="num q">${q_count}</div><div class="lbl">Best Practices</div></div>
  </div>
HTMLEOF

  # Helper to append a table section
  local entry file item desc row_class sec_class hdr_class hdr_icon
  _append_section() {
    local -n _arr="$1"
    local _row_class="$2" _hdr_class="$3" _title="$4"
    [[ ${#_arr[@]} -eq 0 ]] && return 0
    printf '<section><h2 class="%s">%s</h2>\n' "$_hdr_class" "$_title" >> "$report_file"
    printf '<table><tr><th>Issue</th><th>File</th><th>Details &amp; Recommendation</th></tr>\n' >> "$report_file"
    for entry in "${_arr[@]}"; do
      IFS=$'\t' read -r file item desc <<< "$entry"
      printf '<tr class="%s"><td>%s</td><td>%s</td><td>%s</td></tr>\n' \
        "$_row_class" "$(_html_esc "$item")" "$(_html_esc "$file")" "$(_html_esc "$desc")" >> "$report_file"
    done
    printf '</table></section>\n' >> "$report_file"
  }

  _append_section ANALYSIS_BLOCKERS "bi" "bh" "&#10006; Blockers — must resolve before transitioning (${b_count})"
  _append_section ANALYSIS_WARNINGS "wi" "wh" "&#9888; Warnings — behavior differs in containers (${w_count})"
  _append_section ANALYSIS_NOTES    "ni" "nh" "&#8505; Notes — cloud-native considerations (${n_count})"
  _append_section ANALYSIS_QUALITY  "qi" "qh" "&#10024; Best Practices — quality improvements for cloud-native deployment (${q_count})"

  if (( b_count == 0 && w_count == 0 && n_count == 0 && q_count == 0 )); then
    printf '<section><h2 class="oh">&#10003; Ready — no portability considerations detected</h2>' >> "$report_file"
    printf '<p style="padding:16px 20px;margin:0">This application appears ready for containerized deployment.</p></section>\n' >> "$report_file"
  fi

  cat >> "$report_file" <<HTMLEOF
  <div class="foot">Generated by bw5ToCE.sh v${VERSION} &nbsp;|&nbsp; TIBCO BusinessWorks 5 Platform Portability Toolkit</div>
</div>
</body>
</html>
HTMLEOF

  printf 'Analysis report: %s\n' "$report_file"
  log "HTML report written: $report_file"
}

update_values_yaml() {
  local values="$1"
  local gv_yaml="$2"

  # Create a minimal values file if it does not exist
  if [[ ! -f "$values" ]]; then
    cat > "$values" <<'EOF'
appConfig:
  bwProfile: default.substvar
appProps:
  default.substvar: {}
EOF
  fi

  local yq_flavor
  yq_flavor=$(detect_yq_flavor)

  if [[ "$yq_flavor" == "mf" ]]; then
    # mikefarah/yq v4 syntax
    yq eval -i '.appProps = (.appProps // {})' "$values"
    yq eval-all -i '.appProps["default.substvar"] = (select(fileIndex==1).globalVariables // {})' "$values" "$gv_yaml"
  else
    # python yq (jq-style filters)
    # Ensure appProps exists
    yq -y -i '.appProps = (.appProps // {})' "$values"
    # Extract .globalVariables from the generated YAML as JSON and inject via --argjson
    local gv_json
    gv_json="$(yq -r '.globalVariables | tojson' "$gv_yaml")"
    yq -y -i --argjson GV "$gv_json" '.appProps["default.substvar"] = $GV' "$values"
  fi

  log "Updated values.yaml appProps.default.substvar -> $values"
}

load_platform_env() {
  local name="$1"
  local file="${ENV_DIR}/${name}.env"
  [[ -f "$file" ]] || die "Platform env file not found: $file"
  # shellcheck source=/dev/null
  . "$file"
  : "${PLATFORM_BW5CE_BASE_URL:?Missing PLATFORM_BW5CE_BASE_URL in $file}"
  : "${PLATFORM_BW5CE_BASE_VERSION:?Missing PLATFORM_BW5CE_BASE_VERSION in $file}"
  : "${PLATFORM_BW5CE_BASE_IMAGE_TAG:?Missing PLATFORM_BW5CE_BASE_IMAGE_TAG in $file}"
  : "${PLATFORM_TOKEN:?Missing PLATFORM_TOKEN in $file}"
}

platform_api_upload_build() {
  # Uploads EAR to Platform and echoes buildId on success
  local ear_file="$1"
  local build_name
  build_name="$(basename "$ear_file" .ear)"

  local upload_url
  upload_url="${PLATFORM_BW5CE_BASE_URL}/public/v1/dp/builds?baseversion=${PLATFORM_BW5CE_BASE_VERSION}&baseimagetag=${PLATFORM_BW5CE_BASE_IMAGE_TAG}"
  if [[ "$AUTOPROVISION" == "true" ]]; then
    upload_url+="&autoProvision=true"
  fi

  local http_code body_file upload_resp build_id
  body_file="$(mktemp)"
  http_code=$(curl "${CURL_OPTS[@]}" -s -w '%{http_code}' -o "$body_file" -H "Authorization: Bearer ${PLATFORM_TOKEN}" -X POST \
    "$upload_url" \
    -H 'accept: application/json' \
    -H 'Content-Type: multipart/form-data' \
    -F "request={\"dependencies\":[],\"tags\":[],\"buildName\":\"${build_name}\"}" \
    -F "earFile=@${ear_file}" )
  upload_resp="$(cat "$body_file")"
  rm -f "$body_file"

  if [[ "$http_code" == "000" ]]; then
    LAST_ERROR_DETAILS="No connectivity to Platform: server unreachable"
    err "No connectivity to Platform (could not reach: $upload_url)"
    return 1
  fi

  if [[ "$http_code" -lt 200 || "$http_code" -ge 300 ]]; then
    local note
    note=$(extract_upload_error_detail "$upload_resp")
    if [[ -z "$note" ]]; then
      note=$(extract_upload_error_reason "$upload_resp")
    fi
    LAST_ERROR_DETAILS="Upload: $note"
    err "Upload failed (HTTP $http_code): $note"
    # Include full response body to aid troubleshooting
    if command -v jq >/dev/null 2>&1; then
      local pretty
      pretty=$(echo "$upload_resp" | jq -c . 2>/dev/null || echo "$upload_resp")
      log "Upload error body: $pretty"
    else
      log "Upload error body: $upload_resp"
    fi
    return 1
  fi

  build_id="$(echo "$upload_resp" | jq -r '.buildId // empty')"
  if [[ -z "$build_id" || "$build_id" == null ]]; then
    local note
    note=$(extract_upload_error_detail "$upload_resp")
    if [[ -z "$note" ]]; then
      note=$(extract_upload_error_reason "$upload_resp")
    fi
    if [[ -n "$note" ]]; then
      LAST_ERROR_DETAILS="Upload: $note"
      err "Upload failed: missing buildId. $note"
    else
      LAST_ERROR_DETAILS="Missing buildId in Platform response"
      err "Upload failed: Missing buildId in Platform response"
    fi
    if command -v jq >/dev/null 2>&1; then
      local pretty
      pretty=$(echo "$upload_resp" | jq -c . 2>/dev/null || echo "$upload_resp")
      log "Upload error body: $pretty"
    else
      log "Upload error body: $upload_resp"
    fi
    return 1
  fi
  PLATFORM_BUILD_ID="$build_id"
  return 0
}

platform_api_deploy_values() {
  # Deploys using given values.yaml and buildId. Returns 0 on success.
  # Args:
  #   1) values_file - path to values.yaml
  #   2) build_id    - buildId returned by upload
  local values_file="$1"
  local build_id="$2"
  local deploy_url
  deploy_url="${PLATFORM_BW5CE_BASE_URL}/public/v2/dp/deploy/release?eula=true&buildId=${build_id}"
  # Only add namespace if user explicitly provided --namespace
  if [[ "$NAMESPACE_SET" == "true" ]]; then
    deploy_url+="&namespace=${BWCE_NAMESPACE}"
  fi

  local deploy_http_code deploy_body_file deploy_resp
  deploy_body_file="$(mktemp)"
  deploy_http_code=$(curl -H "Authorization: Bearer ${PLATFORM_TOKEN}" "${CURL_OPTS[@]}" -s -w '%{http_code}' -o "$deploy_body_file" -X POST \
    "$deploy_url" \
    -H 'accept: application/json' \
    -H 'Content-Type: multipart/form-data' \
    -F "values.yaml=@${values_file};type=application/x-yaml")
  deploy_resp="$(cat "$deploy_body_file")"
  rm -f "$deploy_body_file"

  if [[ "$deploy_http_code" == "000" ]]; then
    LAST_ERROR_DETAILS="No connectivity to Platform: server unreachable"
    err "No connectivity to Platform (could not reach: $deploy_url)"
    return 1
  fi

  if [[ "$deploy_http_code" -lt 200 || "$deploy_http_code" -ge 300 ]]; then
    local note
    note=$(extract_errdetail "$deploy_resp")
    if [[ -z "$note" ]]; then
      note=$(extract_errmsg_any "$deploy_resp")
    fi
    LAST_ERROR_DETAILS="Deploy: $note"
    # Always print a concise error summary and response body
    err "Deploy failed (HTTP $deploy_http_code): $note"
    if command -v jq >/dev/null 2>&1; then
      local pretty
      pretty=$(echo "$deploy_resp" | jq -c . 2>/dev/null || echo "$deploy_resp")
      log "Deploy error body: $pretty"
    else
      log "Deploy error body: $deploy_resp"
    fi
    return 1
  fi
  log "Platform Deploy Response: $deploy_resp"
  return 0
}

platform_api_check_app_exists() {
  # Looks up app by name; echoes appId if found, empty otherwise
  # Args: 1) app_name
  local app_name="$1"
  [[ -n "$app_name" ]] || { echo ""; return 1; }
  require_bin jq
  local list_url resp app_id http_code body_file
  k8s_name="$(to_k8s_name "$app_name")"
  list_url="${PLATFORM_BW5CE_BASE_URL}/public/v1/dp/apps?filterKey=name&filterValue=${k8s_name}"
  body_file="$(mktemp)"
  http_code=$(curl -H "Authorization: Bearer ${PLATFORM_TOKEN}" "${CURL_OPTS[@]}" -s -w '%{http_code}' -o "$body_file" -X GET \
    "$list_url" \
    -H 'accept: application/json')
  resp="$(cat "$body_file")"
  rm -f "$body_file"
  if [[ "$http_code" == "000" ]]; then
    err "No connectivity to Platform (could not reach: $list_url)"
    echo ""; return 1
  fi
  app_id=$(echo "$resp" | jq -r --arg name "$k8s_name" '((.. | arrays | .[] | objects | select(.appName == $name) | .appId)) // empty' 2>/dev/null || echo "")
  if [[ "$app_id" == "null" ]]; then app_id=""; fi
  echo "$app_id"
}

platform_api_upgrade_values() {
  # Performs upgrade by PUTing values.yaml to /apps/{appId}/release/values
  # Args: 1) values_file 2) app_id
  local values_file="$1" app_id="$2" build_id="$3"
  local url http_code body_file resp
  url="${PLATFORM_BW5CE_BASE_URL}/public/v2/dp/apps/${app_id}/release/values?buildId=${build_id}"
  body_file="$(mktemp)"
  http_code=$(curl -H "Authorization: Bearer ${PLATFORM_TOKEN}" "${CURL_OPTS[@]}" -s -w '%{http_code}' -o "$body_file" -X PUT \
    "$url" \
    -H 'accept: application/json' \
    -H 'Content-Type: multipart/form-data' \
    -F "values.yaml=@${values_file};type=application/x-yaml")
  resp="$(cat "$body_file")"
  rm -f "$body_file"

  if [[ "$http_code" == "000" ]]; then
    LAST_ERROR_DETAILS="No connectivity to Platform: server unreachable"
    err "No connectivity to Platform (could not reach: $url)"
    return 1
  fi

  if [[ "$http_code" -lt 200 || "$http_code" -ge 300 ]]; then
    local note
    note=$(extract_errdetail "$resp")
    if [[ -z "$note" ]]; then note=$(extract_errmsg_any "$resp"); fi
    LAST_ERROR_DETAILS="Upgrade: $note"
    err "Upgrade failed (HTTP $http_code): $note"
    return 1
  fi
  log "Platform Upgrade Response: $resp"
  return 0
}

platform_upload() {
  local ear_file="$1"
  require_bin curl
  require_bin jq
  log "Uploading EAR to Platform..."
  if ! platform_api_upload_build "$ear_file"; then
    return 1
  fi
  local build_id="$PLATFORM_BUILD_ID"
  log "Build ID: $build_id"
  echo "$build_id"
}

set_replica_count_zero() {
  local values_file="$1"
  local yq_flavor
  yq_flavor=$(detect_yq_flavor)
  if [[ "$yq_flavor" == "mf" ]]; then
    yq eval -i '.replicaCount = 0' "$values_file"
  else
    yq -y -i '.replicaCount = 0' "$values_file"
  fi
  log "Set replicaCount: 0 in $values_file"
}

platform_upload_and_deploy() {
  local ear_file="$1"        # final EAR
  local values_file="$2"     # local values (OUT_VALUES) to merge & upload
  local out_dir="$3"
  local app_disp_name="${4:-}"  # display app name for lookup; falls back to APP_NAME

  require_bin curl
  require_bin jq

  # Determine app name for existence check (use basename if a path is provided)
  local app_lookup_name="${app_disp_name:-$APP_NAME}"
  # Strip any trailing slash and take the basename for platform lookup
  app_lookup_name="${app_lookup_name%/}"
  app_lookup_name="${app_lookup_name##*/}"
  if [[ -z "$app_lookup_name" ]]; then
    # Try to read from values.yaml
    app_lookup_name=$(yq -r '.appConfig.appName // empty' "$values_file" 2>/dev/null || echo "")
  fi

  # If not forcing, and app exists, omit
  if [[ "$FORCE_UPGRADE" != "true" && -n "$app_lookup_name" ]]; then
    local existing_id
    existing_id="$(platform_api_check_app_exists "$app_lookup_name")"
    if [[ -n "$existing_id" ]]; then
      LAST_ERROR_DETAILS="Application and version already deployed there"
      log "App '$app_lookup_name' exists (id=$existing_id). Skipping deploy (use --force to upgrade)."
      return 2
    fi
  fi

  local __saved_tags_json="${PLATFORM_TAGS_JSON:-}"
  log "Uploading EAR to Platform..."
  if ! platform_api_upload_build "$ear_file"; then
    PLATFORM_TAGS_JSON="$__saved_tags_json"
    return 1
  fi
  local build_id="$PLATFORM_BUILD_ID"
  log "Build ID: $build_id"

  # If forcing and app exists, perform upgrade
  if [[ "$FORCE_UPGRADE" == "true" && -n "$app_lookup_name" ]]; then
    local existing_id
    existing_id="$(platform_api_check_app_exists "$app_lookup_name")"
    if [[ -n "$existing_id" ]]; then
      log "Upgrading existing app '$app_lookup_name' (id=$existing_id) using values.yaml"
      # Ensure appId is present in values
      yq_set "$values_file" ".appConfig.appId = \"$existing_id\""
      if platform_api_upgrade_values "$values_file" "$existing_id" "$build_id"; then
        log "Upgrade completed for '$app_lookup_name'"
        return 0
      else
        return 1
      fi
    fi
  fi

  # Fresh deploy: upload + deploy
  # If values.yaml contains CSV tags, convert to JSON array for upload

  local __values_tags_csv
  __values_tags_csv="$(yq -r '.appConfig.tags // empty' "$values_file" 2>/dev/null || echo "")"
  if [[ -n "$__values_tags_csv" && "$__values_tags_csv" != "null" ]]; then
    require_bin jq
    PLATFORM_TAGS_JSON=$(jq -cn --arg csv "$__values_tags_csv" '
      ($csv | split(",")
            | map(gsub("^[[:space:]]+|[[:space:]]+$";""))
            | map(select(length>0))
            | reduce .[] as $x ([]; if index($x) then . else . + [$x] end)
      )
    ')
  fi


  log "Deploying via Platform API..."
  local __deploy_rc
  platform_api_deploy_values "$values_file" "$build_id" || __deploy_rc=$?
  # Restore previous tag override
  PLATFORM_TAGS_JSON="$__saved_tags_json"
  return ${__deploy_rc:-0}
}

# ==============
# Batch features
# ==============

batch_export_apps() {
  # Performs AppManage -batchExport for the domain and returns a TSV list:
  #   app_display_name\tear_absolute_path
  local tmp_dir="$1"
  local log_file="$2"

  log "Starting AppManage batch export..." >&2
  local cmd=("$APPMANAGE_BIN" --propFile "${APPMANAGE_BIN_FOLDER}/AppManage.tra" -batchExport -domain "$DOMAIN" -user "$ADMIN_USER" -pw "$ADMIN_PASS" -dir "$tmp_dir")
  log "CMD: ${cmd[*]}" >&2
  # Capture both stdout and stderr (AppManage sometimes logs to stderr)
  "${cmd[@]}" >"$log_file" 2>&1 || true

  # Parse lines like:
  # [ AppName ]: Finished exporting ear file  /path/to/AppName.ear
  # [ Folder/ ]: Finished exporting ear file  /path/to/Folder/AppName.ear
  # Build a mapping app -> ear and app -> xml and emit TSV rows: app\tear\txml
  awk '
    function trim(s){ sub(/^[[:space:]]+/,"",s); sub(/[[:space:]]+$/,"",s); return s }
    function base_noext(p){ gsub(/.*\//,"",p); sub(/\.[^.]*$/,"",p); return p }
    function compute_app(bracket, path){ bracket=trim(bracket); return (bracket ~ /\/$/ ? bracket base_noext(path) : bracket) }
    {
      if (match($0,/\[([^\]]+)\][[:space:]]*:[[:space:]]*Finished exporting ear file[[:space:]]+(.*\.ear)/,m)) {
        p = trim(m[2]); a = compute_app(m[1], p); ear[a] = p; apps[a] = 1;
      } else if (match($0,/\[([^\]]+)\][[:space:]]*:[[:space:]]*Finished exporting configuration file[[:space:]]+(.*\.xml)/,m)) {
        p = trim(m[2]); a = compute_app(m[1], p); xml[a] = p; apps[a] = 1;
      }
    }
    END {
      for (a in apps) {
        print a "\t" ear[a] "\t" xml[a];
      }
    }
  ' "$log_file"
}

batch_process_app() {
  # Process a single app from batch export
  # Args: app_display_name ear_path props_xml_path tmp_dir
  LAST_ERROR_DETAILS=""
  local app_disp="$1"; shift
  CURRENT_APP="$app_disp"
  local ear_src="$1"; shift
  local prop_xml_src="$1"; shift
  local tmp_dir="$1"; shift

  # Trim leading/trailing whitespace from the display name (logs often include spaces inside brackets)
  app_disp="$(trim_spaces "$app_disp")"

  # Extract folder (tag) and base name for proper naming and tagging
  local app_folder app_base
  if [[ "$app_disp" == */* ]]; then
    app_folder="${app_disp%/*}"
    app_base="${app_disp##*/}"
  else
    app_folder=""
    app_base="$app_disp"
  fi

  # Build CSV of unique folder components (order-preserving), replacing '/' with ','
  local tags_csv
  if [[ -n "$app_folder" ]]; then
    tags_csv=$(awk -v s="$app_folder" '
      BEGIN {
        n=split(s,a,"/"); sep="";
        for(i=1;i<=n;i++){
          gsub(/^[[:space:]]+|[[:space:]]+$/,"",a[i]);      # trim
          gsub(/[[:space:]]+/, "", a[i]);                   # remove internal spaces (New Folder -> NewFolder)
          if (a[i]=="") continue;
          if (!(a[i] in seen)) { out = out sep a[i]; sep=", "; seen[a[i]]=1 }
        }
        print out
      }
    ' </dev/null)
  else
    tags_csv=""
  fi

  # safe_app keeps folders to keep output paths unique
  local safe_app
  safe_app="${app_disp//[^A-Za-z0-9._-]/_}"
  # k8s resource name uses the base app name only
  local k8s_name
  k8s_name="$(to_k8s_name "$app_base")"
  local ts
  ts="${TS}"

  local app_out_dir
  app_out_dir="${OUTPUT_DIR}/${safe_app}"
  mkdir -p "$app_out_dir"

  local final_ear
  final_ear="${app_out_dir}/${safe_app}-${ts}.ear"
  if [[ -n "$ear_src" && -f "$ear_src" ]]; then
    cp -f "$ear_src" "$final_ear"
    log "[${app_disp}] EAR -> $final_ear"
  else
    err "[${app_disp}] EAR not found in batch export: $ear_src"
  fi

  # Create/update per-app values file based on base VALUES_FILE, setting fullnameOverride
  local out_values
  out_values="${app_out_dir}/${safe_app}-values.yaml"
  if [[ -f "$VALUES_FILE" ]]; then
    cp -f "$VALUES_FILE" "$out_values"
  else
    echo -e "appProps:\n  default.substvar: {}" > "$out_values"
  fi
  local yq_flavor
  yq_flavor=$(detect_yq_flavor)
  if [[ "$yq_flavor" == "mf" ]]; then
    yq eval -i ".fullnameOverride = \"$k8s_name\"" "$out_values"
    # Use base name for platform lookups/upgrades (no folders)
    yq eval -i ".appConfig.appName = \"$app_base\"" "$out_values"
  else
    yq -y -i ".fullnameOverride = \"$k8s_name\"" "$out_values"
    yq -y -i ".appConfig.appName = \"$app_base\"" "$out_values"
  fi
  # Set appConfig.tags explicitly from the folder path CSV (deduped)
  set_values_appconfig_tags "$out_values" "$app_disp" "$tags_csv"
  log "[${app_disp}] values -> fullnameOverride=$k8s_name"

  # Determine properties XML produced by batchExport and copy to output folder
  local prop_xml_path
  if [[ -n "$prop_xml_src" && -f "$prop_xml_src" ]]; then
    prop_xml_path="$prop_xml_src"
  else
    prop_xml_path="${tmp_dir}/${app_disp}.xml"
  fi
  local final_props
  final_props="${app_out_dir}/${safe_app}-deployment-props-${ts}.xml"
  if [[ -f "$prop_xml_path" ]]; then
    cp -f "$prop_xml_path" "$final_props"
  else
    err "[${app_disp}] Properties XML not found: $prop_xml_path"
  fi

  # Validate BW XML type; omit non-BW apps
  if [[ -f "$final_props" ]] && ! is_bw_properties_xml "$final_props"; then
    log "[${app_disp}] Not a BW deployment XML (omitting)."
    add_report_row "$app_disp" "OMITTED" "not BW (product/type!=BW)"
    return 0
  fi

  # Platform Portability Analysis (per app in batch)
  local _app_report_link=""   # relative path from output/ to this app's HTML report
  if [[ -f "$final_ear" ]]; then
    local batch_analysis_tmp
    batch_analysis_tmp="$(mktemp -d "${tmp_dir}/.analysis_${safe_app}.XXXX")"
    analyze_ear_for_portability "$final_ear" "$batch_analysis_tmp"
    local b_c=${#ANALYSIS_BLOCKERS[@]} w_c=${#ANALYSIS_WARNINGS[@]} n_c=${#ANALYSIS_NOTES[@]} q_c=${#ANALYSIS_QUALITY[@]}
    local analysis_note="Analysis: ${b_c}B/${w_c}W/${n_c}N/${q_c}Q"
    if [[ "$GENERATE_REPORT" == "true" ]]; then
      local batch_report_path="${app_out_dir}/${safe_app}-analysis-${ts}.html"
      generate_html_report "$app_disp" "$batch_report_path"
      _app_report_link="${safe_app}/$(basename "$batch_report_path")"
    fi
    if [[ "$GENERATE_CLI_REPORT" == "true" ]]; then
      if [[ -n "$CLI_REPORT_FILE" ]]; then
        local batch_cli_report_path="${app_out_dir}/${safe_app}-analysis-${ts}.txt"
        generate_cli_report "$app_disp" "$batch_cli_report_path"
      else
        generate_cli_report "$app_disp"
      fi
    fi
    if (( b_c > 0 )) && [[ "$ALLOW_BLOCKERS" != "true" ]]; then
      add_report_row "$app_disp" "BLOCKED" "${analysis_note} — use --allow-blockers to override" "$_app_report_link"
      return 0
    fi
  fi

  # If offline export, stop here
  if [[ "$OFFLINE_EXPORT" == "true" ]]; then
    add_report_row "$app_disp" "EXPORTED" "offline export only" "$_app_report_link"
    return 0
  fi

  # Ensure properties XML exists for further steps
  [[ -f "$final_props" ]] || { add_report_row "$app_disp" "ERROR" "Properties XML missing" "$_app_report_link"; return 0; }

  local props_yaml
  props_yaml="${app_out_dir}/${safe_app}-global-variables-${ts}.yaml"
  generate_yaml_from_global_vars "$final_props" "$props_yaml"
  update_values_yaml "$out_values" "$props_yaml"
  # Ensure fullnameOverride remains set
  if [[ "$yq_flavor" == "mf" ]]; then
    yq eval -i ".fullnameOverride = \"$k8s_name\"" "$out_values"
    yq eval -i ".appConfig.appName = \"$app_base\"" "$out_values"
  else
    yq -y -i ".fullnameOverride = \"$k8s_name\"" "$out_values"
    yq -y -i ".appConfig.appName = \"$app_base\"" "$out_values"
  fi
  # Preserve tags set earlier from folder path CSV (re-assert to be safe)
  set_values_appconfig_tags "$out_values" "$app_disp" "$tags_csv"

  # Optional deployment for batch if platform desired
  if [[ "$OFFLINE_EXPORT" == "true" ]]; then
    log "[${app_disp}] offline: skipping deployment."
    add_report_row "$app_disp" "EXPORTED" "offline export only" "$_app_report_link"
    return 0
  fi

  if [[ -n "$PLATFORM_ENV" ]]; then
    load_platform_env "$PLATFORM_ENV"
    if [[ "$NO_DEPLOY" == "true" ]]; then
      if platform_upload "$final_ear" >/dev/null; then
        log "[${app_disp}] no-deploy: uploaded only."
        add_report_row "$app_disp" "UPLOADED" "no-deploy" "$_app_report_link"
      else
        add_report_row "$app_disp" "ERROR" "$LAST_ERROR_DETAILS" "$_app_report_link"
      fi
      return 0
    fi
    if [[ "$NO_START" == "true" ]]; then
      set_replica_count_zero "$out_values"
      if platform_upload_and_deploy "$final_ear" "$out_values" "$app_out_dir"; then
        log "[${app_disp}] no-start: deployed with replicaCount=0."
        add_report_row "$app_disp" "DEPLOYED" "no-start (replicaCount=0)" "$_app_report_link"
      else
        add_report_row "$app_disp" "ERROR" "$LAST_ERROR_DETAILS" "$_app_report_link"
      fi
      return 0
    fi
    if platform_upload_and_deploy "$final_ear" "$out_values" "$app_out_dir" "$app_base"; then
      log "[${app_disp}] Platform deployment done."
      add_report_row "$app_disp" "DEPLOYED" "" "$_app_report_link"
    else case "$?" in
      2)
        add_report_row "$app_disp" "OMITTED" "Application and version already deployed there" "$_app_report_link" ;;
      *)
      add_report_row "$app_disp" "ERROR" "$LAST_ERROR_DETAILS" "$_app_report_link"
      ;;
    esac
    fi
    return 0
  fi

  # No platform env provided; skip deployment
  log "[${app_disp}] No --platform supplied; skipping deployment."
  add_report_row "$app_disp" "EXPORTED" "no --platform provided" "$_app_report_link"
  CURRENT_APP=""
}

deploy_offline_from_output() {
  # Iterates output/* and deploys using Platform API with the most recent EAR and per-app values.
  # If an app name is provided (first arg), only deploy that app.
  if [[ "${ANALYZE_ONLY:-false}" != "true" ]]; then
    require_bin curl; require_bin jq
  fi
  shopt -s nullglob
  # Accept optional single app filter; safe with set -u
  local app_filter="${1-}"
  local app_dir
  if [[ -n "$app_filter" ]]; then
    # sanitize to the same safe folder naming used elsewhere
    local safe_app
    safe_app="${app_filter//[^A-Za-z0-9._-]/_}"
    if [[ -d "${OUTPUT_DIR}/${safe_app}" ]]; then
      set -- "${OUTPUT_DIR}/${safe_app}/"
    else
      err "Offline deploy: app folder not found for '$app_filter' (expected: ${OUTPUT_DIR}/${safe_app})"
      return 1
    fi
  else
    set -- "${OUTPUT_DIR}"/*/
  fi
  for app_dir in "$@"; do
    [[ -d "$app_dir" ]] || continue
    local app_label
    app_label="$(basename "${app_dir%/}")"
    local latest_ear="" latest_values="" latest_props=""
    # Nullglob-safe: collect glob matches into array first to avoid ls listing cwd
    local -a _g
    _g=("$app_dir"/*.ear);                    (( ${#_g[@]} > 0 )) && latest_ear=$(ls -t "${_g[@]}" | head -n1) || true
    _g=("$app_dir"/*-values.yaml);            (( ${#_g[@]} > 0 )) && latest_values=$(ls -t "${_g[@]}" | head -n1) || true
    _g=("$app_dir"/*-deployment-props-*.xml); (( ${#_g[@]} > 0 )) && latest_props=$(ls -t "${_g[@]}" | head -n1) || true
    if [[ -n "$latest_props" && -f "$latest_props" ]]; then
      if ! is_bw_properties_xml "$latest_props"; then
        log "[offline] Skipping $app_label: not BW deployment XML."
        add_report_row "$app_label" "OMITTED" "not BW deployment XML"
        continue
      fi
    fi
    if [[ "$ANALYZE_ONLY" == "true" ]]; then
      if [[ -z "$latest_ear" ]]; then
        log "Skipping $app_label: missing EAR."
        add_report_row "$app_label" "SKIPPED" "missing EAR"
        continue
      fi
      CURRENT_APP="$app_label"
      local _atmp="$app_dir/.analysis_tmp_$$"
      mkdir -p "$_atmp"
      analyze_ear_for_portability "$latest_ear" "$_atmp"
      rm -rf "$_atmp"
      if [[ "$GENERATE_CLI_REPORT" != "true" || -n "$CLI_REPORT_FILE" ]]; then
        print_analysis_summary "$app_label"
      fi
      local _ao_html_link=""
      if [[ "$GENERATE_REPORT" == "true" ]]; then
        local _html="${app_dir%/}/${app_label}-analysis-${TS}.html"
        generate_html_report "$app_label" "$_html"
        _ao_html_link="${app_label}/$(basename "$_html")"
      fi
      if [[ "$GENERATE_CLI_REPORT" == "true" ]]; then
        if [[ -n "$CLI_REPORT_FILE" ]]; then
          local _txt="${app_dir%/}/${app_label}-analysis-${TS}.txt"
          generate_cli_report "$app_label" "$_txt"
        else
          generate_cli_report "$app_label"
        fi
      fi
      local _bc=${#ANALYSIS_BLOCKERS[@]} _wc=${#ANALYSIS_WARNINGS[@]} _nc=${#ANALYSIS_NOTES[@]} _qc=${#ANALYSIS_QUALITY[@]}
      local _counts="${_bc}B/${_wc}W/${_nc}N/${_qc}Q"
      if   (( _bc > 0 )); then add_report_row "$app_label" "BLOCKED" "$_counts" "$_ao_html_link"
      elif (( _wc > 0 )); then add_report_row "$app_label" "CAUTION" "$_counts" "$_ao_html_link"
      elif (( _nc > 0 )); then add_report_row "$app_label" "REVIEW"  "$_counts" "$_ao_html_link"
      else                     add_report_row "$app_label" "READY"   "$_counts" "$_ao_html_link"
      fi
      CURRENT_APP=""
      continue
    fi
    if [[ -z "$latest_ear" || -z "$latest_values" ]]; then
      log "Skipping $app_label: missing EAR or values.yaml."
      add_report_row "$app_label" "SKIPPED" "missing EAR or values.yaml"
      continue
    fi
    log "[offline] Deploying $app_label via Platform API"
    CURRENT_APP="$app_label"
    if [[ "$NO_DEPLOY" == "true" ]]; then
      if ! platform_upload "$latest_ear" >/dev/null; then
        add_report_row "$app_label" "ERROR" "$LAST_ERROR_DETAILS"
      else
        add_report_row "$app_label" "UPLOADED" "no-deploy"
      fi
      continue
    fi
    if [[ "$NO_START" == "true" ]]; then
      set_replica_count_zero "$latest_values"
    fi
    # Resolve app name for platform lookup from values.yaml if present
    local lookup_name
    lookup_name="$(yq -r '.appConfig.appName // empty' "$latest_values" 2>/dev/null || true)"
    if [[ -z "$lookup_name" || "$lookup_name" == "null" ]]; then
      # Fallback to folder basename if appName not defined
      lookup_name="${app_dir%/}"; lookup_name="${lookup_name##*/}"
    fi
    local __rc=0
    platform_upload_and_deploy "$latest_ear" "$latest_values" "$app_dir" "$lookup_name" || __rc=$?
    case "$__rc" in
      0)
        if [[ "$NO_START" == "true" ]]; then
          add_report_row "$app_label" "DEPLOYED" "no-start (replicaCount=0)"
        else
          add_report_row "$app_label" "DEPLOYED" ""
        fi
        ;;
      2) add_report_row "$app_label" "OMITTED" "Application and version already deployed there" ;;
      *)
        add_report_row "$app_label" "ERROR" "$LAST_ERROR_DETAILS"
        ;;
    esac
    CURRENT_APP=""
  done
}

# Allow this script to be sourced (e.g. by BATS tests) to expose helper/analysis
# functions without executing the main argument-parsing and deployment flow.
[[ "${BASH_SOURCE[0]}" != "${0}" ]] && return 0

# =========
# Arguments
# =========

DOMAIN=""
APP_NAME=""

BWCE_NAMESPACE=""
NAMESPACE_SET="false"
PLATFORM_ENV=""
DEPLOY_OFFLINE="false"
BATCH_MODE="false"
OFFLINE_EXPORT="false"
NO_DEPLOY="false"
NO_START="false"
FORCE_UPGRADE="false"
ANALYZE_ONLY="false"
GENERATE_REPORT="false"
CURL_OPTS=()   # populated with -k only when --insecure-tls is passed
REPORT_FILE=""
GENERATE_CLI_REPORT="false"
CLI_REPORT_FILE=""
ALLOW_BLOCKERS="false"
# Custom artifacts mode (bypass AppManage export)
CUSTOM_MODE="false"
CUSTOM_APP_NAME=""
CUSTOM_EAR_FILE=""
CUSTOM_XML_FILE=""

# Collect positional args safely (domain/app) and parse flags flexibly
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --version) echo "$(basename "$0") v$VERSION - $AUTHOR"; exit 0 ;;
    --namespace) BWCE_NAMESPACE="${2:-}"; NAMESPACE_SET="true"; shift 2 ;;
    --offline) OFFLINE_EXPORT="true"; shift ;;
    --no-deploy) NO_DEPLOY="true"; shift ;;
    --no-start) NO_START="true"; shift ;;
    --force) FORCE_UPGRADE="true"; shift ;;
    --platform) PLATFORM_ENV="${2:-}"; shift 2 ;;
    --batch) BATCH_MODE="true"; shift ;;
    --deploy-offline) DEPLOY_OFFLINE="true"; shift ;;
    --app) CUSTOM_APP_NAME="${2:-}"; CUSTOM_MODE="true"; shift 2 ;;
    --ear) CUSTOM_EAR_FILE="${2:-}"; CUSTOM_MODE="true"; shift 2 ;;
    --xml) CUSTOM_XML_FILE="${2:-}"; CUSTOM_MODE="true"; shift 2 ;;
    --analyze-only) ANALYZE_ONLY="true"; shift ;;
    --allow-blockers) ALLOW_BLOCKERS="true"; shift ;;
    --insecure-tls) CURL_OPTS+=("-k"); shift ;;
    --report)
      GENERATE_REPORT="true"
      # Optional path argument: consume only if next token is not a flag
      if [[ $# -gt 1 && "${2:-}" != --* && -n "${2:-}" ]]; then
        REPORT_FILE="${2}"; shift 2
      else
        shift
      fi
      ;;
    --report-cli)
      GENERATE_CLI_REPORT="true"
      # Optional path argument
      if [[ $# -gt 1 && "${2:-}" != --* && -n "${2:-}" ]]; then
        CLI_REPORT_FILE="${2}"; shift 2
      else
        shift
      fi
      ;;
    --debug) DEBUG="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    --*) err "Unknown option: $1"; usage; exit 1 ;;
    *) POSITIONAL+=("$1"); shift ;;
  esac
done

if (( ${#POSITIONAL[@]} >= 1 )); then DOMAIN="${POSITIONAL[0]}"; fi
if (( ${#POSITIONAL[@]} >= 2 )); then APP_NAME="${POSITIONAL[1]}"; fi
if (( ${#POSITIONAL[@]} > 2 )); then err "Too many positional arguments: ${POSITIONAL[*]}"; usage; exit 1; fi



# Load optional config props
if [[ -f "$CONFIG_PROPS_FILE" ]]; then
  log "Loading config props: $CONFIG_PROPS_FILE"
  # shellcheck disable=SC2163
  while IFS='=' read -r k v; do
    # skip comments/blank
    [[ -z "$k" || "$k" =~ ^[[:space:]]*# ]] && continue
    k="${k%%[[:space:]]*}"
    v="${v#*=}"
    export "$k"="$v"
  done < "$CONFIG_PROPS_FILE"
fi

# Defaults for optional config flags
AUTOPROVISION="${AUTOPROVISION:-false}"

# Load path-related defaults if not set in config.props or environment
ENV_DIR="${ENV_DIR:-./env}"
WORK_DIR="${WORK_DIR:-./work}"
OUTPUT_DIR="${OUTPUT_DIR:-./output}"
VALUES_FILE="${VALUES_FILE:-./values.yaml}"

# Resolve AppManage paths after loading config (prefer TRA_HOME if provided)
if [[ -z "${APPMANAGE_BIN_FOLDER:-}" ]]; then
  if [[ -n "${TRA_HOME:-}" ]]; then
    APPMANAGE_BIN_FOLDER="${TRA_HOME%/}/bin"
  else
    APPMANAGE_BIN_FOLDER="/opt/tibco/tra/5.13/bin"
  fi
fi
if [[ -z "${APPMANAGE_BIN:-}" ]]; then
  APPMANAGE_BIN="${APPMANAGE_BIN_FOLDER}/AppManage"
fi

# ===========================
# Validate flag combinations
# ===========================

# --offline and --platform are mutually exclusive
if [[ "$OFFLINE_EXPORT" == "true" && -n "$PLATFORM_ENV" ]]; then
  die "--offline and --platform are mutually exclusive: --offline skips all upload and deploy steps"
fi

# --no-deploy and --no-start are mutually exclusive
if [[ "$NO_DEPLOY" == "true" && "$NO_START" == "true" ]]; then
  die "--no-deploy and --no-start are mutually exclusive: --no-deploy skips deployment entirely, --no-start deploys with replicaCount=0"
fi


# --deploy-offline and --offline are mutually exclusive
if [[ "$DEPLOY_OFFLINE" == "true" && "$OFFLINE_EXPORT" == "true" ]]; then
  die "--deploy-offline and --offline are mutually exclusive: --deploy-offline deploys existing artifacts, --offline exports without deploying"
fi

# --deploy-offline requires --platform (unless analyze-only, which needs no platform)
if [[ "$DEPLOY_OFFLINE" == "true" && -z "$PLATFORM_ENV" && "$ANALYZE_ONLY" != "true" ]]; then
  die "--deploy-offline requires --platform <ENV> for Platform API access"
fi

# --deploy-offline without an app name implies --batch (deploy all apps from output/)
if [[ "$DEPLOY_OFFLINE" == "true" && "$BATCH_MODE" != "true" && -z "$APP_NAME" && -z "$DOMAIN" ]]; then
  BATCH_MODE="true"
fi

# --no-deploy and --no-start require --platform
if [[ "$NO_DEPLOY" == "true" && -z "$PLATFORM_ENV" ]]; then
  die "--no-deploy requires --platform <ENV> to upload the EAR"
fi
if [[ "$NO_START" == "true" && -z "$PLATFORM_ENV" ]]; then
  die "--no-start requires --platform <ENV> to upload and deploy"
fi

# --force requires --platform (upgrade checks for an existing app on the Platform)
if [[ "$FORCE_UPGRADE" == "true" && -z "$PLATFORM_ENV" ]]; then
  die "--force requires --platform <ENV> (upgrade checks for an existing app on the Platform)"
fi

# Custom mode (--app/--ear/--xml) incompatibilities
if [[ "$CUSTOM_MODE" == "true" ]]; then
  if [[ "$BATCH_MODE" == "true" ]]; then
    die "--app/--ear/--xml cannot be used with --batch: custom mode bypasses AppManage"
  fi
  if [[ "$OFFLINE_EXPORT" == "true" ]]; then
    die "--app/--ear/--xml cannot be used with --offline"
  fi
  if [[ "$DEPLOY_OFFLINE" == "true" ]]; then
    die "--app/--ear/--xml cannot be used with --deploy-offline"
  fi
  [[ -n "$CUSTOM_APP_NAME" && -n "$CUSTOM_EAR_FILE" && -n "$CUSTOM_XML_FILE" ]] || { err "--app, --ear and --xml must all be provided together"; usage; exit 1; }
  [[ -f "$CUSTOM_EAR_FILE" ]] || die "EAR file not found: $CUSTOM_EAR_FILE"
  [[ -f "$CUSTOM_XML_FILE" ]] || die "XML file not found: $CUSTOM_XML_FILE"
else
  if [[ "$DEPLOY_OFFLINE" != "true" ]]; then
    if [[ "$BATCH_MODE" == "true" ]]; then
      [[ -n "$DOMAIN" ]] || { err "<DOMAIN> is required with --batch (unless combined with --deploy-offline)"; usage; exit 1; }
    else
      [[ -n "$DOMAIN" ]] || { err "<DOMAIN> is required"; usage; exit 1; }
      [[ -n "$APP_NAME" ]] || { err "<APP_NAME> is required"; usage; exit 1; }
    fi
  fi
fi

if [[ "$DEPLOY_OFFLINE" != "true" && "$BATCH_MODE" != "true" && "$CUSTOM_MODE" != "true" ]]; then
  # ==================
  # Load domain config
  # ==================

  DOMAIN_ENV_FILE="${ENV_DIR}/${DOMAIN}.env"
  [[ -f "$DOMAIN_ENV_FILE" ]] || die "Env file not found: $DOMAIN_ENV_FILE"

  # shellcheck source=/dev/null
  . "$DOMAIN_ENV_FILE"

  # Expected variables in the domain env file:
  : "${ADMIN_URL:?Missing ADMIN_URL in ${DOMAIN_ENV_FILE}}"
  : "${ADMIN_USER:?Missing ADMIN_USER in ${DOMAIN_ENV_FILE}}"
  : "${ADMIN_PASS:?Missing ADMIN_PASS in ${DOMAIN_ENV_FILE}}"

  # Optional overrides per domain removed; use --namespace flag explicitly if needed
fi

# ===========
# Pre-flight
# ===========

# ==========================
# Pre-flight: check all deps
# ==========================
_required_bins=()
if [[ "$DEPLOY_OFFLINE" != "true" ]]; then
  if [[ "$CUSTOM_MODE" != "true" ]]; then
    _required_bins+=("$APPMANAGE_BIN" "envsubst")
  fi
  _required_bins+=("yq" "xmlstarlet" "zip")
fi
if [[ (-n "$PLATFORM_ENV" || "$DEPLOY_OFFLINE" == "true") && "$ANALYZE_ONLY" != "true" ]]; then
  _required_bins+=("curl" "jq")
fi
check_prereqs "${_required_bins[@]}"
unset _required_bins

mkdir -p "$WORK_DIR" "$OUTPUT_DIR"

TS="$(date +%Y%m%d-%H%M%S)"

if [[ "$DEPLOY_OFFLINE" == "true" ]]; then
  if [[ "$ANALYZE_ONLY" != "true" ]]; then
    load_platform_env "$PLATFORM_ENV"
  fi
  log "Deploy-offline mode: using artifacts from $OUTPUT_DIR via Platform API"
  # --batch forces deploying all apps from output/ regardless of positional args
  if [[ "$BATCH_MODE" == "true" ]]; then
    log "Batch deploy-offline: deploying all apps from $OUTPUT_DIR"
    deploy_offline_from_output
    print_batch_report "${DOMAIN:-offline}" "$TS"
  else
    # Optional single-app filter: if one positional provided, treat it as app name
    app_filter=""
    if [[ -n "$APP_NAME" ]]; then
      app_filter="$APP_NAME"
    elif [[ -n "$DOMAIN" ]]; then
      app_filter="$DOMAIN"
    fi
    if [[ -n "$app_filter" ]]; then
      deploy_offline_from_output "$app_filter"
    else
      deploy_offline_from_output
    fi
  fi
  exit 0
fi

if [[ "$BATCH_MODE" == "true" ]]; then
  # Load domain config before batch
  DOMAIN_ENV_FILE="${ENV_DIR}/${DOMAIN}.env"
  [[ -f "$DOMAIN_ENV_FILE" ]] || die "Env file not found: $DOMAIN_ENV_FILE"
  # shellcheck source=/dev/null
  . "$DOMAIN_ENV_FILE"
  : "${ADMIN_URL:?Missing ADMIN_URL in ${DOMAIN_ENV_FILE}}"
  : "${ADMIN_USER:?Missing ADMIN_USER in ${DOMAIN_ENV_FILE}}"
  : "${ADMIN_PASS:?Missing ADMIN_PASS in ${DOMAIN_ENV_FILE}}"
  # Optional overrides per domain removed; use --namespace flag explicitly if needed

  log "Domain ............: $DOMAIN"
  log "Admin URL .........: ${ADMIN_URL:-}"
  log "Namespace .........: $BWCE_NAMESPACE"
  log "Output dir ........: $OUTPUT_DIR"

  TMP_DIR="$(mktemp -d "${WORK_DIR}/.${DOMAIN}_batch.XXXX")"
  cleanup() { rm -rf "$TMP_DIR"; }
  trap cleanup EXIT

  local_log="${OUTPUT_DIR}/batch-${DOMAIN}-${TS}.log"
  tsv="$(batch_export_apps "$TMP_DIR" "$local_log")"
  if [[ -z "$tsv" ]]; then
    die "No apps detected in batch export. See log: $local_log"
  fi
  log "Batch export log ....: $local_log"
  # Iterate over TSV lines
  while IFS=$'\t' read -r app_disp ear_path xml_path; do
    batch_process_app "$app_disp" "$ear_path" "$xml_path" "$TMP_DIR"
  done <<< "$tsv"
  print_batch_report "$DOMAIN" "$TS"
  log "Batch processing completed."
  exit 0
fi

# If using custom artifacts, align APP_NAME now
if [[ "$CUSTOM_MODE" == "true" ]]; then
  APP_NAME="$CUSTOM_APP_NAME"
fi

SAFE_APP="${APP_NAME//[^A-Za-z0-9._-]/_}"
TMP_DIR="$(mktemp -d "${WORK_DIR}/.${DOMAIN}_${SAFE_APP}.XXXX")"
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

# Per-app output folder
OUTPUT_APP_DIR="${OUTPUT_DIR}/${SAFE_APP}"
mkdir -p "${OUTPUT_APP_DIR}"

# Temp export paths
EAR_PATH="${TMP_DIR}/${SAFE_APP}.ear"
PROP_XML_PATH="${TMP_DIR}/${SAFE_APP}-deployment-props.xml"

# Final artifact paths (stored under per-app folder)
FINAL_EAR="${OUTPUT_APP_DIR}/${SAFE_APP}-${TS}.ear"
FINAL_PROPS="${OUTPUT_APP_DIR}/${SAFE_APP}-deployment-props-${TS}.xml"
PROPS_YAML="${OUTPUT_APP_DIR}/${SAFE_APP}-global-variables-${TS}.yaml"
OUT_VALUES="${OUTPUT_APP_DIR}/${SAFE_APP}-values.yaml"

log "Domain ............: $DOMAIN"
log "Application .......: $APP_NAME"
log "Admin URL .........: ${ADMIN_URL:-}"
log "Namespace .........: $BWCE_NAMESPACE"
log "Output dir ........: $OUTPUT_DIR"
log "App output dir ....: $OUTPUT_APP_DIR"

# =========================
# 1) Prepare artifacts
# =========================
  if [[ "$CUSTOM_MODE" == "true" ]]; then
    # Use provided EAR and XML, copy to output with the same naming convention
    APP_NAME="$CUSTOM_APP_NAME"
    SAFE_APP="${APP_NAME//[^A-Za-z0-9._-]/_}"
    mkdir -p "$OUTPUT_APP_DIR"
    cp -f "$CUSTOM_EAR_FILE" "$FINAL_EAR"
    cp -f "$CUSTOM_XML_FILE" "$FINAL_PROPS"
    log "Using custom artifacts for app '$APP_NAME'"
    log "  EAR ...............: $FINAL_EAR"
    log "  Properties (XML)...: $FINAL_PROPS"
  else
  # Ensure variables are exported so envsubst can see them
  # ADMIN_PASS is passed inline to envsubst only — not exported to the global environment
  export APPMANAGE_BIN APPMANAGE_BIN_FOLDER DOMAIN APP_NAME ADMIN_URL ADMIN_USER EAR_PATH PROP_XML_PATH
  EXPORT_EAR_CMD="$(ADMIN_PASS="$ADMIN_PASS" envsubst <<<"$APPMANAGE_EXPORT_EAR_TMPL")"
  log "Exporting EAR with AppManage..."
  log "CMD: ${EXPORT_EAR_CMD//"$ADMIN_PASS"/'***'}"
  if [[ "$DEBUG" == "true" ]]; then
    eval "$EXPORT_EAR_CMD"
  else
    eval "$EXPORT_EAR_CMD" >/dev/null 2>&1
  fi
  [[ -f "$EAR_PATH" ]] || die "EAR export failed; file not found: $EAR_PATH"

  # Move artifacts to per-app output folder
    cp -f "$EAR_PATH" "$FINAL_EAR"
    cp -f "${EAR_PATH}.xml" "$FINAL_PROPS"
    log "Artifacts ready:"
    log "  EAR ...............: $FINAL_EAR"
    log "  Properties (XML)...: $FINAL_PROPS"
  fi

  # Validate BW XML type; omit non-BW apps for all single-app modes
  # In analyze-only mode the props XML is optional (analysis only needs the EAR)
  if ! is_bw_properties_xml "$FINAL_PROPS"; then
    if [[ "$ANALYZE_ONLY" != "true" ]]; then
      log "Properties XML indicates non-BW application. Omitting further processing."
      summary "OMITTED: $APP_NAME (not BW deployment XML)"
      exit 0
    fi
    log "Properties XML is not BW format; proceeding with EAR-only analysis (--analyze-only)"
  fi

# =======================================
# 2b) Platform Portability Analysis
# =======================================
if [[ "$GENERATE_REPORT" == "true" && -z "$REPORT_FILE" ]]; then
  REPORT_FILE="${OUTPUT_APP_DIR}/${SAFE_APP}-analysis-${TS}.html"
fi
# CLI_REPORT_FILE intentionally left empty if no path given → stdout
analyze_ear_for_portability "$FINAL_EAR" "$TMP_DIR"
# Always print to console, unless --report-cli with no path (it will print via generate_cli_report)
if [[ "$GENERATE_CLI_REPORT" != "true" || -n "$CLI_REPORT_FILE" ]]; then
  print_analysis_summary "$APP_NAME"
fi
if [[ "$GENERATE_REPORT" == "true" ]]; then
  generate_html_report "$APP_NAME" "$REPORT_FILE"
fi
if [[ "$GENERATE_CLI_REPORT" == "true" ]]; then
  generate_cli_report "$APP_NAME" "$CLI_REPORT_FILE"
fi
if [[ "${#ANALYSIS_BLOCKERS[@]}" -gt 0 && "$ALLOW_BLOCKERS" != "true" && "$ANALYZE_ONLY" != "true" ]]; then
  printf 'Deployment blocked due to %d blocker(s). Use --allow-blockers to override.\n' "${#ANALYSIS_BLOCKERS[@]}" >&2
  exit 1
fi
if [[ "$ANALYZE_ONLY" == "true" ]]; then
  exit 0
fi

# =======================================
# 3) Generate YAML from Global Variables
# =======================================
generate_yaml_from_global_vars "$FINAL_PROPS" "$PROPS_YAML"
# Start from an untouched copy of the template for per-app customization
if [[ -f "$VALUES_FILE" ]]; then
  cp -f "$VALUES_FILE" "$OUT_VALUES"
else
  rm -f "$OUT_VALUES"
fi
# Update Helm values.yaml with the generated variables in the working copy only
update_values_yaml "$OUT_VALUES" "$PROPS_YAML"
# Also set appConfig.tags in the working copy
set_values_appconfig_tags "$OUT_VALUES" "$APP_NAME"

# Ensure fullnameOverride uses only the basename (no folder parts)
app_basename="${APP_NAME%/}"; app_basename="${app_basename##*/}"
k8s_name="$(to_k8s_name "$app_basename")"
yq_flavor=$(detect_yq_flavor)
if [[ "$yq_flavor" == "mf" ]]; then
  yq eval -i ".fullnameOverride = \"$k8s_name\"" "$OUT_VALUES"
  yq eval -i ".appConfig.appName = \"$app_basename\"" "$OUT_VALUES"
else
  yq -y -i ".fullnameOverride = \"$k8s_name\"" "$OUT_VALUES"
  yq -y -i ".appConfig.appName = \"$app_basename\"" "$OUT_VALUES"
fi
# Set appConfig.tags from the folder part(s) of the full APP_NAME
set_values_appconfig_tags "$OUT_VALUES" "$APP_NAME"
log "  fullnameOverride set to: $k8s_name"
log "  Values (YAML)......: $OUT_VALUES"
# =========================
# 4) Deployment phase
# =========================
# Mode: offline export only
if [[ "$OFFLINE_EXPORT" == "true" ]]; then
  log "Mode offline: skipping upload and deployment. Artifacts are in $OUTPUT_APP_DIR"
  summary "EXPORTED: $APP_NAME"
  exit 0
fi

if [[ -n "$PLATFORM_ENV" ]]; then
  log "Using Platform env: $PLATFORM_ENV"
  load_platform_env "$PLATFORM_ENV"

  if [[ "$NO_DEPLOY" == "true" ]]; then
    if platform_upload "$FINAL_EAR" >/dev/null; then
      log "Mode no-deploy: uploaded EAR only; skipping deployment."
      summary "UPLOADED: $APP_NAME"
      exit 0
    else
      summary "ERROR: $APP_NAME - $LAST_ERROR_DETAILS"
      exit 1
    fi
  fi

  if [[ "$NO_START" == "true" ]]; then
    set_replica_count_zero "$OUT_VALUES"
    if platform_upload_and_deploy "$FINAL_EAR" "$OUT_VALUES" "$OUTPUT_APP_DIR" "$APP_NAME"; then
      log "Mode no-start: deployed with replicaCount=0 (app not started)."
      summary "DEPLOYED (no-start): $APP_NAME"
      exit 0
    else
      case "$?" in
        2) summary "OMITTED: $APP_NAME (Application and version already deployed there)"; exit 0 ;;
        *) summary "ERROR: $APP_NAME - $LAST_ERROR_DETAILS"; exit 1 ;;
      esac
    fi
  fi

  # full platform flow
  if platform_upload_and_deploy "$FINAL_EAR" "$OUT_VALUES" "$OUTPUT_APP_DIR" "$APP_NAME"; then
    log "Platform deployment done."
    summary "DEPLOYED: $APP_NAME"
    exit 0
  else
    case "$?" in
      2) summary "OMITTED: $APP_NAME (Application and version already deployed there)"; exit 0 ;;
      *) summary "ERROR: $APP_NAME - $LAST_ERROR_DETAILS"; exit 1 ;;
    esac
  fi
fi

# No platform specified; skip deployment
log "No --platform supplied; skipping deployment. Artifacts available at $OUTPUT_APP_DIR"
summary "EXPORTED: $APP_NAME"
