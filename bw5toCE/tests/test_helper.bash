# Common BATS test setup sourced by all test files.
# Source this at the top of each .bats file:
#   load 'test_helper'

# Resolve absolute paths relative to this file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCRIPT="$REPO_DIR/bw5ToCE.sh"
FIXTURES_DIR="$SCRIPT_DIR/fixtures"

# Source the main script to expose its functions without running main logic.
# The sourcing guard ([[ BASH_SOURCE != $0 ]]) ensures the argument-parsing
# and deployment sections are skipped.
load_script() {
  # shellcheck source=../bw5ToCE.sh
  source "$SCRIPT"
}

# Create a temp directory cleaned up after each test
setup() {
  TEST_TMP="$(mktemp -d)"
}

teardown() {
  [[ -n "${TEST_TMP:-}" ]] && rm -rf "$TEST_TMP" || true
}

# Assert that a string contains a substring
assert_contains() {
  local haystack="$1"
  local needle="$2"
  if [[ "$haystack" != *"$needle"* ]]; then
    echo "Expected to contain: $needle" >&2
    echo "Actual output: $haystack" >&2
    return 1
  fi
}

# Assert that a string does NOT contain a substring
assert_not_contains() {
  local haystack="$1"
  local needle="$2"
  if [[ "$haystack" == *"$needle"* ]]; then
    echo "Expected NOT to contain: $needle" >&2
    echo "Actual output: $haystack" >&2
    return 1
  fi
}

# Check whether a fixture exists; skip the test if it doesn't
require_fixture() {
  local name="$1"
  local path="$FIXTURES_DIR/${name}.ear"
  if [[ ! -f "$path" ]]; then
    skip "Fixture not found: $path — run tests/fixtures/make_fixtures.sh"
  fi
  echo "$path"
}
