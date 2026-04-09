#!/usr/bin/env bats
# Integration tests for the --analyze-only CLI option.
# Runs the script directly using --app/--ear/--xml (custom mode) so no
# BW5 domain or AppManage installation is required.

load 'test_helper'

SCRIPT_BIN="$REPO_DIR/bw5ToCE.sh"

setup() {
  TEST_TMP="$(mktemp -d)"
  # Minimal valid BW deployment properties XML (satisfies is_bw_properties_xml)
  BW_XML="$TEST_TMP/props.xml"
  cat > "$BW_XML" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<ApplicationManagement xmlns="http://www.tibco.com/xmlns/ApplicationManagement">
  <product><type>BW</type></product>
</ApplicationManagement>
XML
}

teardown() {
  [[ -n "${TEST_TMP:-}" ]] && rm -rf "$TEST_TMP" || true
}

# ─── Helpers ──────────────────────────────────────────────────────────────────

# Run script in --analyze-only mode with a fixture EAR; extra args forwarded.
run_analyze_only() {
  local fixture="$1"; shift
  run bash "$SCRIPT_BIN" --app TestApp \
    --ear "$(require_fixture "$fixture")" \
    --xml "$BW_XML" \
    --analyze-only "$@"
}

# Run script WITHOUT --analyze-only (full flow) with a fixture EAR.
run_full_flow() {
  local fixture="$1"; shift
  run bash "$SCRIPT_BIN" --app TestApp \
    --ear "$(require_fixture "$fixture")" \
    --xml "$BW_XML" \
    "$@"
}

# ─── Exit codes ───────────────────────────────────────────────────────────────

@test "--analyze-only: exits 0 for clean EAR (no issues)" {
  run_analyze_only "clean"
  [ "$status" -eq 0 ]
}

@test "--analyze-only: exits 0 for EAR with blockers (analysis never blocks)" {
  run_analyze_only "http_basic_auth"
  [ "$status" -eq 0 ]
}

@test "without --analyze-only: exits 1 when blockers are present" {
  run_full_flow "http_basic_auth" --offline
  [ "$status" -eq 1 ]
}

@test "--allow-blockers: analysis still shows BLOCKED in summary (flag only controls deployment gate)" {
  run_full_flow "http_basic_auth" --allow-blockers
  assert_contains "$output" "BLOCKED"
}

# ─── Summary output ───────────────────────────────────────────────────────────

@test "--analyze-only: shows READY for clean EAR" {
  run_analyze_only "clean"
  assert_contains "$output" "READY"
}

@test "--analyze-only: shows BLOCKED for EAR with blockers" {
  run_analyze_only "http_basic_auth"
  assert_contains "$output" "BLOCKED"
}

@test "--analyze-only: shows CAUTION for EAR with warnings only" {
  run_analyze_only "checkpoint_file"
  assert_contains "$output" "CAUTION"
}

@test "--analyze-only: shows REVIEW for EAR with notes only" {
  run_analyze_only "file_io"
  assert_contains "$output" "REVIEW"
}

@test "--analyze-only: includes blocker description in output" {
  run_analyze_only "http_basic_auth"
  assert_contains "$output" "HTTP Basic Auth"
}

@test "--analyze-only: includes app name in summary header" {
  run_analyze_only "clean"
  assert_contains "$output" "TestApp"
}

# ─── --report-cli ─────────────────────────────────────────────────────────────

@test "--report-cli with path: creates report file" {
  local report="$TEST_TMP/analysis.txt"
  run_analyze_only "http_basic_auth" --report-cli "$report"
  [ "$status" -eq 0 ]
  [ -f "$report" ]
}

@test "--report-cli with path: report file contains analysis content" {
  local report="$TEST_TMP/analysis.txt"
  run_analyze_only "http_basic_auth" --report-cli "$report"
  assert_contains "$(cat "$report")" "BLOCKED"
  assert_contains "$(cat "$report")" "HTTP Basic Auth"
}

@test "--report-cli without path: prints report to stdout" {
  run_analyze_only "clean" --report-cli
  [ "$status" -eq 0 ]
  assert_contains "$output" "READY"
}

# ─── All fixture EARs ─────────────────────────────────────────────────────────

@test "--analyze-only: unsupported_adapters.ear exits 0 and shows BLOCKED" {
  run_analyze_only "unsupported_adapters"
  [ "$status" -eq 0 ]
  assert_contains "$output" "BLOCKED"
}

@test "--analyze-only: unsupported_plugins.ear exits 0 and shows BLOCKED" {
  run_analyze_only "unsupported_plugins"
  [ "$status" -eq 0 ]
  assert_contains "$output" "BLOCKED"
}

@test "--analyze-only: unsupported_mainframe.ear exits 0 and shows BLOCKED" {
  run_analyze_only "unsupported_mainframe"
  [ "$status" -eq 0 ]
  assert_contains "$output" "BLOCKED"
}

@test "--analyze-only: wait_notify.ear exits 0 and shows CAUTION" {
  run_analyze_only "wait_notify"
  [ "$status" -eq 0 ]
  assert_contains "$output" "CAUTION"
}

@test "--analyze-only: ft_group.ear exits 0 and shows REVIEW" {
  run_analyze_only "ft_group"
  [ "$status" -eq 0 ]
  assert_contains "$output" "REVIEW"
}

@test "--analyze-only: rendezvous.ear exits 0 and shows REVIEW" {
  run_analyze_only "rendezvous"
  [ "$status" -eq 0 ]
  assert_contains "$output" "REVIEW"
}

# ─── Real application EAR ─────────────────────────────────────────────────────

@test "--analyze-only: ProjADB732rpc.ear is READY for migration" {
  local ear="$REPO_DIR/output/ProjADB732rpc/ProjADB732rpc.ear"
  if [[ ! -f "$ear" ]]; then
    skip "ProjADB732rpc.ear not found in output/ — run an export first"
  fi
  run bash "$SCRIPT_BIN" --app ProjADB732rpc \
    --ear "$ear" \
    --xml "$BW_XML" \
    --analyze-only
  [ "$status" -eq 0 ]
  assert_contains "$output" "READY"
}

@test "--analyze-only: ProjADB732rpc.ear has no blockers" {
  local ear="$REPO_DIR/output/ProjADB732rpc/ProjADB732rpc.ear"
  if [[ ! -f "$ear" ]]; then
    skip "ProjADB732rpc.ear not found in output/ — run an export first"
  fi
  run bash "$SCRIPT_BIN" --app ProjADB732rpc \
    --ear "$ear" \
    --xml "$BW_XML" \
    --analyze-only
  assert_not_contains "$output" "BLOCKED"
}
