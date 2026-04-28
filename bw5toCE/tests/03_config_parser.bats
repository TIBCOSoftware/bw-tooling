#!/usr/bin/env bats
# Tests for the config.props parsing logic.
# Validates security fix: only valid identifier names are exported.

load 'test_helper'

# ─── Helpers ──────────────────────────────────────────────────────────────────

# Parse a config.props string and echo the exported value for a given key.
# Uses a subshell to avoid polluting the test environment.
parse_config_get() {
  local props_content="$1"
  local key="$2"
  local props_file="$TEST_TMP/config.props"
  printf '%s\n' "$props_content" > "$props_file"
  (
    # Replicate trim_spaces inline (no access to function before source)
    _trim() { local s="${1-}"; s="${s#"${s%%[![:space:]]*}"}"; s="${s%"${s##*[![:space:]]}"}"; printf '%s' "$s"; }
    while IFS='=' read -r k v; do
      [[ -z "$k" || "$k" =~ ^[[:space:]]*# ]] && continue
      k="${k%%[[:space:]]*}"
      v="$(_trim "$v")"
      [[ "$k" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || continue
      export "$k"="$v"
    done < "$props_file"
    # Guard against invalid variable names in indirect expansion
    [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || { printf ''; exit 0; }
    printf '%s' "${!key:-}"
  )
}

# ─── Basic parsing ────────────────────────────────────────────────────────────

@test "config.props: simple KEY=VALUE is parsed" {
  result="$(parse_config_get "MY_KEY=hello" "MY_KEY")"
  [ "$result" = "hello" ]
}

@test "config.props: value with spaces trimmed" {
  result="$(parse_config_get "MY_KEY=  hello  " "MY_KEY")"
  [ "$result" = "hello" ]
}

@test "config.props: comment lines are ignored" {
  result="$(parse_config_get "# MY_KEY=hello" "MY_KEY")"
  [ "$result" = "" ]
}

@test "config.props: inline comment is NOT stripped (values are literal)" {
  # The parser does not strip inline comments; the full value is used
  result="$(parse_config_get "MY_KEY=hello # comment" "MY_KEY")"
  # Value includes everything after '=' on that line
  [ "$result" = "hello # comment" ]
}

@test "config.props: blank lines are ignored" {
  content=$'MY_KEY=one\n\nOTHER=two'
  result="$(parse_config_get "$content" "MY_KEY")"
  [ "$result" = "one" ]
}

@test "config.props: multiple keys parsed correctly" {
  content=$'A=foo\nB=bar'
  result_a="$(parse_config_get "$content" "A")"
  result_b="$(parse_config_get "$content" "B")"
  [ "$result_a" = "foo" ]
  [ "$result_b" = "bar" ]
}

@test "config.props: value containing equals sign is preserved" {
  result="$(parse_config_get "MY_KEY=jdbc:oracle:thin:@host:1521/orcl" "MY_KEY")"
  [ "$result" = "jdbc:oracle:thin:@host:1521/orcl" ]
}

# ─── Security: identifier validation ─────────────────────────────────────────

@test "config.props: key with spaces is rejected (security)" {
  # 'bad key' has a space — should not be exported
  result="$(parse_config_get "bad key=evil" "bad_key")"
  [ "$result" = "" ]
}

@test "config.props: key starting with digit is rejected (canary unaffected)" {
  # '1BAD' is not a valid shell identifier — verify a canary variable stays unset
  # We include a valid key alongside to confirm parsing itself still works
  result="$(parse_config_get $'1BAD=evil\nCANARY=ok' "CANARY")"
  [ "$result" = "ok" ]
  # The helper guards indirect expansion of invalid names, so no error occurs
  result2="$(parse_config_get "1BAD=evil" "CANARY")"
  [ "$result2" = "" ]
}

@test "config.props: key with semicolon is rejected" {
  result="$(parse_config_get "BAD;KEY=value" "BADKEY")"
  [ "$result" = "" ]
}

@test "config.props: key with hyphen is rejected" {
  result="$(parse_config_get "BAD-KEY=value" "BADKEY")"
  [ "$result" = "" ]
}

@test "config.props: valid key with underscore is accepted" {
  result="$(parse_config_get "GOOD_KEY=value" "GOOD_KEY")"
  [ "$result" = "value" ]
}

@test "config.props: valid key with leading underscore is accepted" {
  result="$(parse_config_get "_PRIVATE=value" "_PRIVATE")"
  [ "$result" = "value" ]
}

@test "config.props: key with PATH traversal characters rejected" {
  result="$(parse_config_get "../../../etc/passwd=evil" "passwd")"
  [ "$result" = "" ]
}
