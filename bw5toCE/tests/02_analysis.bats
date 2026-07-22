#!/usr/bin/env bats
# Integration tests for the platform portability analysis engine.
# Requires fixture EARs — run tests/fixtures/make_fixtures.sh first.

load 'test_helper'

setup() {
  TEST_TMP="$(mktemp -d)"
  load_script
  # Reset analysis state before each test
  ANALYSIS_BLOCKERS=()
  ANALYSIS_WARNINGS=()
  ANALYSIS_NOTES=()
  ANALYSIS_QUALITY=()
}

# ─── clean EAR ────────────────────────────────────────────────────────────────

@test "clean.ear: no blockers, warnings, or notes" {
  ear="$(require_fixture "clean")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -eq 0 ]
}

# ─── BLOCKERS ─────────────────────────────────────────────────────────────────

# HTTPReceiver (HTTPEventSource) with useHTTPAuthentication=true
@test "http_basic_auth.ear: HTTPReceiver with useHTTPAuthentication=true is BLOCKER" {
  ear="$(require_fixture "http_basic_auth")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_BLOCKERS[0]}" "HTTP Basic Auth"
}

@test "http_basic_auth.ear: no false warnings or notes from basic auth" {
  ear="$(require_fixture "http_basic_auth")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -eq 0 ]
}

# SOAPEventSource with useBasicAuthentication=true
@test "soap_event_source_basic_auth.ear: SOAPEventSource with useBasicAuthentication=true is BLOCKER" {
  ear="$(require_fixture "soap_event_source_basic_auth")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_BLOCKERS[0]}" "HTTP Basic Auth"
}

@test "soap_event_source_basic_auth.ear: no false warnings or notes" {
  ear="$(require_fixture "soap_event_source_basic_auth")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -eq 0 ]
}

# ServiceAgent with useBasicAuthentication=true
@test "service_agent_basic_auth.ear: ServiceAgent with useBasicAuthentication=true is BLOCKER" {
  ear="$(require_fixture "service_agent_basic_auth")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_BLOCKERS[0]}" "HTTP Basic Auth"
}

@test "service_agent_basic_auth.ear: no false warnings or notes" {
  ear="$(require_fixture "service_agent_basic_auth")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -eq 0 ]
}

@test "unsupported_activity.ear: detects unknown type as BLOCKER" {
  ear="$(require_fixture "unsupported_activity")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_BLOCKERS[0]}" "Not Yet Available"
}

@test "unsupported_activity.ear: blocker description mentions activity type" {
  ear="$(require_fixture "unsupported_activity")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  assert_contains "${ANALYSIS_BLOCKERS[0]}" "com.tibco.plugin.unknownvendor"
}

# Adapter (AAR) — supported: ADB (componentSoftwareName=adb)
@test "adapter_supported.ear: ADB adapter produces no blockers" {
  ear="$(require_fixture "adapter_supported")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
}

# Adapter (AAR) — unsupported: JD Edwards, PeopleSoft, OSIsoft PI, Tuxedo
@test "unsupported_adapters.ear: detects all 4 unsupported adapters as BLOCKERs" {
  ear="$(require_fixture "unsupported_adapters")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 4 ]
}

# Adapter (AAR) — unrecognized name is treated as a custom adapter (NOTE, no blocker)
@test "custom_adapter.ear: unknown adapter is a NOTE, not a blocker" {
  ear="$(require_fixture "custom_adapter")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -ge 1 ]
  local all_n="${ANALYSIS_NOTES[*]}"
  assert_contains "$all_n" "Custom Adapter"
}

# Adapter AAR with an absolute-path entry makes unzip exit 1 (warning); the
# analyzer must still detect the adapter instead of bailing on the warning.
@test "custom_adapter_abspath.ear: adapter detected despite unzip warning" {
  ear="$(require_fixture "custom_adapter_abspath")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -ge 1 ]
  local all_n="${ANALYSIS_NOTES[*]}"
  assert_contains "$all_n" "Custom Adapter"
}

@test "unsupported_adapters.ear: blocker labels use friendly adapter names" {
  ear="$(require_fixture "unsupported_adapters")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_blockers="${ANALYSIS_BLOCKERS[*]}"
  assert_contains "$all_blockers" "JD Edwards Adapter"
  assert_contains "$all_blockers" "PeopleSoft Adapter"
  assert_contains "$all_blockers" "OSIsoft PI Adapter"
  assert_contains "$all_blockers" "Tuxedo Adapter"
}

# Known-unsupported plugins: EJB, Mobile Integration, NetSuite, SmartMapper, ActiveSpaces
# Type strings taken verbatim from PluginActivityMap (Go extractor package)
@test "unsupported_plugins.ear: detects all 5 unsupported plugins as BLOCKERs" {
  ear="$(require_fixture "unsupported_plugins")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 5 ]
}

@test "unsupported_plugins.ear: blocker labels use friendly plugin names" {
  ear="$(require_fixture "unsupported_plugins")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_blockers="${ANALYSIS_BLOCKERS[*]}"
  assert_contains "$all_blockers" "EJB Plugin"
  assert_contains "$all_blockers" "Mobile Integration Plugin"
  assert_contains "$all_blockers" "NetSuite Plugin"
  assert_contains "$all_blockers" "SmartMapper Plugin"
  assert_contains "$all_blockers" "ActiveSpaces 1/2 Plugin"
}

# Known-unsupported mainframe: CICS, HL7
# (EDI has no PluginActivityMap entry — not detectable via <pd:type> scanning)
@test "unsupported_mainframe.ear: detects CICS and HL7 as BLOCKERs" {
  ear="$(require_fixture "unsupported_mainframe")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 2 ]
}

@test "unsupported_mainframe.ear: blocker labels use friendly mainframe names" {
  ear="$(require_fixture "unsupported_mainframe")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_blockers="${ANALYSIS_BLOCKERS[*]}"
  assert_contains "$all_blockers" "CICS Mainframe Plugin"
  assert_contains "$all_blockers" "HL7 Plugin"
}

# ─── WARNINGS ─────────────────────────────────────────────────────────────────

@test "checkpoint_file.ear: checkpoint without DB is WARNING" {
  ear="$(require_fixture "checkpoint_file")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Checkpoint"
}

@test "checkpoint_file.ear: no blockers for checkpoint" {
  ear="$(require_fixture "checkpoint_file")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
}

@test "checkpoint_db.ear: checkpoint with DB storage is WARNING (not blocker)" {
  ear="$(require_fixture "checkpoint_db")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Checkpoint"
  assert_contains "${ANALYSIS_WARNINGS[0]}" "DB"
}

@test "wait_notify.ear: Wait/Notify pattern is WARNING" {
  ear="$(require_fixture "wait_notify")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Wait"
}

# ─── NOTES ────────────────────────────────────────────────────────────────────

@test "file_io.ear: file activities are NOTE" {
  ear="$(require_fixture "file_io")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_NOTES[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_NOTES[0]}" "File I/O"
}

@test "file_io.ear: no blockers or warnings for plain file I/O" {
  ear="$(require_fixture "file_io")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
}

@test "ft_group.ear: FT Group reference is NOTE (not BLOCKER)" {
  ear="$(require_fixture "ft_group")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_NOTES[0]}" "Fault Tolerant"
}

@test "rendezvous.ear: RV activities are NOTE" {
  ear="$(require_fixture "rendezvous")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_NOTES[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_NOTES[0]}" "Rendezvous"
}

@test "java_activity.ear: Java activities are NOTE" {
  ear="$(require_fixture "java_activity")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_NOTES[@]}" -ge 1 ]
  local all_n="${ANALYSIS_NOTES[*]}"
  assert_contains "$all_n" "Java Code"
}

@test "java_activity.ear: clean Java code raises no Java-17 warning" {
  ear="$(require_fixture "java_activity")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_w="${ANALYSIS_WARNINGS[*]:-}"
  [[ "$all_w" != *"Removed in Java 17"* ]]
}

@test "java_removed_api.ear: Java-17-removed API is WARNING" {
  ear="$(require_fixture "java_removed_api")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  local all_w="${ANALYSIS_WARNINGS[*]}"
  assert_contains "$all_w" "Removed in Java 17"
  assert_contains "$all_w" "JAXB"
}

# ─── WARNINGS (continued) ────────────────────────────────────────────────────

@test "engine_command_lifecycle.ear: lifecycle EngineCommand is WARNING" {
  ear="$(require_fixture "engine_command_lifecycle")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Engine Command"
}

@test "engine_command_lifecycle.ear: warning includes the detected command names" {
  ear="$(require_fixture "engine_command_lifecycle")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Shutdown"
}

@test "engine_command_lifecycle.ear: no blockers for lifecycle EngineCommand" {
  ear="$(require_fixture "engine_command_lifecycle")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
}

@test "engine_command_safe.ear: GetActivityStats EngineCommand produces no warning" {
  ear="$(require_fixture "engine_command_safe")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
}

@test "external_command.ear: External Command Activity is WARNING" {
  ear="$(require_fixture "external_command")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_WARNINGS[0]}" "External Command"
}

@test "external_command.ear: no blockers for External Command Activity" {
  ear="$(require_fixture "external_command")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
}

@test "external_command.ear: no notes for External Command Activity" {
  ear="$(require_fixture "external_command")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_NOTES[@]}" -eq 0 ]
}

# ─── State reset between calls ────────────────────────────────────────────────

@test "analysis state is reset between analyze_ear_for_portability calls" {
  ear_blocker="$(require_fixture "http_basic_auth")"
  ear_clean="$(require_fixture "clean")"

  # Use separate work dirs to avoid leftover extracted files between calls
  analyze_ear_for_portability "$ear_blocker" "$TEST_TMP/run1"
  [ "${#ANALYSIS_BLOCKERS[@]}" -ge 1 ]

  # Second call with clean EAR must reset state (new work dir, no leftover artifacts)
  analyze_ear_for_portability "$ear_clean" "$TEST_TMP/run2"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
  [ "${#ANALYSIS_NOTES[@]}" -eq 0 ]
}

# ─── print_analysis_summary output ───────────────────────────────────────────

@test "print_analysis_summary: shows READY when no issues" {
  ANALYSIS_BLOCKERS=(); ANALYSIS_WARNINGS=(); ANALYSIS_NOTES=()
  output="$(print_analysis_summary "TestApp")"
  assert_contains "$output" "READY"
}

@test "print_analysis_summary: shows BLOCKED when blockers present" {
  ANALYSIS_BLOCKERS=("file.process"$'\t'"HTTP Basic Auth"$'\t'"Basic Auth not supported")
  ANALYSIS_WARNINGS=(); ANALYSIS_NOTES=()
  output="$(print_analysis_summary "TestApp")"
  assert_contains "$output" "BLOCKED"
  assert_contains "$output" "HTTP Basic Auth"
}

@test "print_analysis_summary: shows CAUTION when only warnings" {
  ANALYSIS_BLOCKERS=()
  ANALYSIS_WARNINGS=("file.process"$'\t'"Checkpoint"$'\t'"File-based checkpoint")
  ANALYSIS_NOTES=()
  output="$(print_analysis_summary "TestApp")"
  assert_contains "$output" "CAUTION"
}

@test "print_analysis_summary: shows REVIEW when only notes" {
  ANALYSIS_BLOCKERS=(); ANALYSIS_WARNINGS=()
  ANALYSIS_NOTES=("file.process"$'\t'"File I/O"$'\t'"Ephemeral FS")
  output="$(print_analysis_summary "TestApp")"
  assert_contains "$output" "REVIEW"
}

# ─── generate_cli_report ──────────────────────────────────────────────────────

@test "generate_cli_report: writes to file when path given" {
  ANALYSIS_BLOCKERS=(); ANALYSIS_WARNINGS=(); ANALYSIS_NOTES=()
  local out_file="$TEST_TMP/report.txt"
  generate_cli_report "TestApp" "$out_file"
  [ -f "$out_file" ]
  assert_contains "$(cat "$out_file")" "READY"
}

@test "generate_cli_report: prints to stdout when no path given" {
  ANALYSIS_BLOCKERS=(); ANALYSIS_WARNINGS=(); ANALYSIS_NOTES=()
  output="$(generate_cli_report "TestApp")"
  assert_contains "$output" "READY"
}

# ─── _analysis_scan_shared_resources ─────────────────────────────────────────

@test "_analysis_scan_shared_resources: flags multi-engine Module Shared Variable (must be DB-backed)" {
  local res_dir="$TEST_TMP/resources"
  mkdir -p "$res_dir"
  cat > "$res_dir/MyVar.moduleSharedVariable" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<sharedVariable>
  <resourceType>ae.shared.moduleSharedVariable</resourceType>
  <multi-engine>true</multi-engine>
  <persistent>false</persistent>
</sharedVariable>
XML
  ANALYSIS_WARNINGS=()
  _analysis_scan_shared_resources "$res_dir"
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Module Shared Var"
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Database"
}

@test "_analysis_scan_shared_resources: flags persistent Module Shared Variable (must be DB-backed)" {
  local res_dir="$TEST_TMP/resources"
  mkdir -p "$res_dir"
  cat > "$res_dir/MyVar.moduleSharedVariable" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<sharedVariable>
  <resourceType>ae.shared.moduleSharedVariable</resourceType>
  <multi-engine>false</multi-engine>
  <persistent>true</persistent>
</sharedVariable>
XML
  ANALYSIS_WARNINGS=()
  _analysis_scan_shared_resources "$res_dir"
  [ "${#ANALYSIS_WARNINGS[@]}" -ge 1 ]
  assert_contains "${ANALYSIS_WARNINGS[0]}" "Database"
}

@test "_analysis_scan_shared_resources: no warning for non-persistent single-engine Module Shared Variable" {
  local res_dir="$TEST_TMP/resources"
  mkdir -p "$res_dir"
  cat > "$res_dir/MyVar.moduleSharedVariable" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<sharedVariable>
  <resourceType>ae.shared.moduleSharedVariable</resourceType>
  <multi-engine>false</multi-engine>
  <persistent>false</persistent>
</sharedVariable>
XML
  ANALYSIS_WARNINGS=()
  _analysis_scan_shared_resources "$res_dir"
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
}

# ─── Best Practices: shared connection resources ──────────────────────────────

@test "hardcoded_sharedhttp.ear: hardcoded host flags QUALITY item" {
  ear="$(require_fixture "hardcoded_sharedhttp")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  assert_contains "$all_q" "Hard-coded Host"
}

@test "hardcoded_sharedhttp.ear: hardcoded port flags QUALITY item" {
  ear="$(require_fixture "hardcoded_sharedhttp")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  assert_contains "$all_q" "Hard-coded Port"
}

@test "hardcoded_sharedhttp.ear: no portability blockers or warnings" {
  ear="$(require_fixture "hardcoded_sharedhttp")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
  [ "${#ANALYSIS_WARNINGS[@]}" -eq 0 ]
}

@test "safe_sharedhttp.ear: GV-referenced host+port produce no QUALITY items" {
  ear="$(require_fixture "safe_sharedhttp")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  [[ "$all_q" != *"Hard-coded Host"* ]]
  [[ "$all_q" != *"Hard-coded Port"* ]]
}

@test "hardcoded_sharedjdbc.ear: hardcoded URL, user, and password all flagged" {
  ear="$(require_fixture "hardcoded_sharedjdbc")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  assert_contains "$all_q" "Hard-coded URL — Shared JDBC"
  assert_contains "$all_q" "Hard-coded User — Shared JDBC"
  assert_contains "$all_q" "Hard-coded Password — Shared JDBC"
}

@test "hardcoded_sharedjms.ear: hardcoded URL, user, and password all flagged" {
  ear="$(require_fixture "hardcoded_sharedjms")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  assert_contains "$all_q" "Hard-coded URL — Shared JMS"
  assert_contains "$all_q" "Hard-coded User — Shared JMS"
  assert_contains "$all_q" "Hard-coded Password — Shared JMS"
}

# ─── Best Practices: process-level ───────────────────────────────────────────

@test "render_xml_pretty.ear: render-xml pretty-print is QUALITY item" {
  ear="$(require_fixture "render_xml_pretty")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  assert_contains "$all_q" "render-xml"
}

@test "render_xml_pretty.ear: no portability blockers for render-xml" {
  ear="$(require_fixture "render_xml_pretty")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  [ "${#ANALYSIS_BLOCKERS[@]}" -eq 0 ]
}

@test "render_xml_pretty.ear: has description — no description QUALITY item" {
  ear="$(require_fixture "render_xml_pretty")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  [[ "$all_q" != *"No Process Description"* ]]
}

@test "clean.ear: missing description flags No Process Description QUALITY item" {
  ear="$(require_fixture "clean")"
  analyze_ear_for_portability "$ear" "$TEST_TMP"
  local all_q="${ANALYSIS_QUALITY[*]}"
  assert_contains "$all_q" "No Process Description"
}

@test "print_analysis_summary: shows BEST PRACTICES section when quality items present" {
  ANALYSIS_BLOCKERS=(); ANALYSIS_WARNINGS=(); ANALYSIS_NOTES=()
  ANALYSIS_QUALITY=("main.process"$'\t'"No Process Description"$'\t'"Add a description")
  output="$(print_analysis_summary "TestApp")"
  assert_contains "$output" "BEST PRACTICES"
  assert_contains "$output" "No Process Description"
}

@test "print_analysis_summary: quality items do not change READY status" {
  ANALYSIS_BLOCKERS=(); ANALYSIS_WARNINGS=(); ANALYSIS_NOTES=()
  ANALYSIS_QUALITY=("main.process"$'\t'"No Process Description"$'\t'"Add a description")
  output="$(print_analysis_summary "TestApp")"
  assert_contains "$output" "READY"
}
