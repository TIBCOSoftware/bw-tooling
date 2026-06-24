#!/usr/bin/env bats
# Unit tests for pure helper functions in bw5ToCE.sh.
# These tests do not require a BW5 domain or AppManage.

load 'test_helper'

setup() {
  TEST_TMP="$(mktemp -d)"
  load_script
}

# ─── trim_spaces ───────────────────────────────────────────────────────────────

@test "trim_spaces: removes leading spaces" {
  result="$(trim_spaces "   hello")"
  [ "$result" = "hello" ]
}

@test "trim_spaces: removes trailing spaces" {
  result="$(trim_spaces "hello   ")"
  [ "$result" = "hello" ]
}

@test "trim_spaces: removes both sides" {
  result="$(trim_spaces "  hello world  ")"
  [ "$result" = "hello world" ]
}

@test "trim_spaces: empty string stays empty" {
  result="$(trim_spaces "")"
  [ "$result" = "" ]
}

@test "trim_spaces: no-op on clean string" {
  result="$(trim_spaces "hello")"
  [ "$result" = "hello" ]
}

# ─── to_k8s_name ──────────────────────────────────────────────────────────────

@test "to_k8s_name: lowercase conversion" {
  result="$(to_k8s_name "MyApp")"
  [ "$result" = "myapp" ]
}

@test "to_k8s_name: spaces become hyphens" {
  result="$(to_k8s_name "My App Name")"
  [ "$result" = "my-app-name" ]
}

@test "to_k8s_name: underscores become hyphens" {
  result="$(to_k8s_name "my_app_name")"
  [ "$result" = "my-app-name" ]
}

@test "to_k8s_name: consecutive hyphens collapsed" {
  result="$(to_k8s_name "My  App")"
  [ "$result" = "my-app" ]
}

@test "to_k8s_name: leading/trailing hyphens stripped" {
  result="$(to_k8s_name "-MyApp-")"
  [ "$result" = "myapp" ]
}

@test "to_k8s_name: slash becomes hyphen" {
  result="$(to_k8s_name "Folder/App")"
  [ "$result" = "folder-app" ]
}

# ─── to_k8s_label_value ───────────────────────────────────────────────────────

@test "to_k8s_label_value: slash becomes underscore" {
  result="$(to_k8s_label_value "Folder/App")"
  [ "$result" = "Folder_App" ]
}

@test "to_k8s_label_value: spaces stripped" {
  result="$(to_k8s_label_value "My App")"
  [ "$result" = "MyApp" ]
}

@test "to_k8s_label_value: preserves dots and hyphens" {
  result="$(to_k8s_label_value "v1.0-beta")"
  [ "$result" = "v1.0-beta" ]
}

# ─── compute_tags_vars ────────────────────────────────────────────────────────

@test "compute_tags_vars: no folder → empty tags" {
  result="$(compute_tags_vars "MyApp")"
  json="$(echo "$result" | cut -f1)"
  csv="$(echo "$result" | cut -f2)"
  [ "$json" = "[]" ]
  [ "$csv" = "" ]
}

@test "compute_tags_vars: one folder level" {
  result="$(compute_tags_vars "FolderA/MyApp")"
  json="$(echo "$result" | cut -f1)"
  csv="$(echo "$result" | cut -f2)"
  [ "$json" = '["FolderA"]' ]
  [ "$csv" = "FolderA" ]
}

@test "compute_tags_vars: nested folders" {
  result="$(compute_tags_vars "A/B/MyApp")"
  csv="$(echo "$result" | cut -f2)"
  [ "$csv" = "A/B" ]
}

# ─── repeat_char ──────────────────────────────────────────────────────────────

@test "repeat_char: repeats character n times" {
  result="$(repeat_char '-' 5)"
  [ "$result" = "-----" ]
}

@test "repeat_char: zero repetitions returns empty" {
  result="$(repeat_char '-' 0)"
  [ "$result" = "" ]
}

# ─── _analysis_is_supported_type ──────────────────────────────────────────────

@test "_analysis_is_supported_type: core bw prefix is supported" {
  _analysis_is_supported_type "com.tibco.bw.core.OnStartupEventSource"
}

@test "_analysis_is_supported_type: http is supported" {
  _analysis_is_supported_type "com.tibco.plugin.http.HttpRequestActivity"
}

@test "_analysis_is_supported_type: parse is supported (core)" {
  _analysis_is_supported_type "com.tibco.plugin.parse.ParseActivity"
}

@test "_analysis_is_supported_type: restjson plugin is supported" {
  _analysis_is_supported_type "com.tibco.plugin.restjson.RESTActivity"
}

@test "_analysis_is_supported_type: kafka plugin is supported" {
  _analysis_is_supported_type "com.tibco.plugin.kafka.KafkaPublishActivity"
}

@test "_analysis_is_supported_type: unknown type is not supported" {
  ! _analysis_is_supported_type "com.tibco.plugin.unknownvendor.SomeActivity"
}

@test "_analysis_is_supported_type: non-tibco type is not supported" {
  ! _analysis_is_supported_type "org.example.SomeActivity"
}

# ─── _analysis_check_tibco_xml ────────────────────────────────────────────────

@test "_analysis_check_tibco_xml: returns false for missing file" {
  result="$(_analysis_check_tibco_xml "/nonexistent/TIBCO.xml")"
  [ "$result" = "false" ]
}

@test "_analysis_check_tibco_xml: returns false when no checkpoint keywords" {
  local f="$TEST_TMP/TIBCO.xml"
  printf '<repository><globalvariables/></repository>\n' > "$f"
  result="$(_analysis_check_tibco_xml "$f")"
  [ "$result" = "false" ]
}

@test "_analysis_check_tibco_xml: returns true when 'Checkpoint Data Repository' present" {
  local f="$TEST_TMP/TIBCO.xml"
  printf '<repository><globalvariable><description>Checkpoint Data Repository</description></globalvariable></repository>\n' > "$f"
  result="$(_analysis_check_tibco_xml "$f")"
  [ "$result" = "true" ]
}

@test "_analysis_check_tibco_xml: returns true when 'bw.checkpoint' present" {
  local f="$TEST_TMP/TIBCO.xml"
  printf '<repository><prop>bw.checkpoint.storage=DB</prop></repository>\n' > "$f"
  result="$(_analysis_check_tibco_xml "$f")"
  [ "$result" = "true" ]
}

# ─── is_bw_properties_xml ─────────────────────────────────────────────────────

@test "is_bw_properties_xml: returns false for missing file" {
  ! is_bw_properties_xml "/nonexistent/file.xml"
}

@test "is_bw_properties_xml: returns true for BW product type" {
  local f="$TEST_TMP/props.xml"
  cat > "$f" <<'XML'
<?xml version="1.0" encoding="UTF-8"?>
<ApplicationManagement xmlns="http://www.tibco.com/xmlns/ApplicationManagement">
  <product><type>BW</type></product>
</ApplicationManagement>
XML
  is_bw_properties_xml "$f"
}

@test "is_bw_properties_xml: returns false for non-BW XML" {
  local f="$TEST_TMP/other.xml"
  printf '<root><data>hello</data></root>\n' > "$f"
  ! is_bw_properties_xml "$f"
}
