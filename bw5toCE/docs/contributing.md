# Contributing

## Development Setup

No build step required. The project is a single Bash script plus supporting files.

Required tools for development:
```sh
# macOS
brew install xmlstarlet yq jq curl bats-core
brew install gettext && brew link --force gettext
```

On Linux (Debian/Ubuntu):
```sh
apt-get install -y xmlstarlet jq curl gettext unzip
# Install yq (mikefarah): https://github.com/mikefarah/yq/releases
# Install bats-core: https://github.com/bats-core/bats-core
```

## Running Tests

The test suite uses [BATS (Bash Automated Testing System)](https://github.com/bats-core/bats-core).

```sh
# Install bats-core if not already installed
brew install bats-core   # macOS
# or: npm install -g bats

# Generate test fixture EARs (run once)
bash tests/fixtures/make_fixtures.sh

# Run all tests
bats tests/

# Run a specific test file
bats tests/01_utilities.bats

# Run with verbose output
bats --verbose-run tests/
```

## Project Structure

```
bw5ToCE.sh   # Main script (all logic lives here)
values.yaml            # Base Helm values updated by the script
env/                   # Environment files (not committed; see .gitignore)
  <DOMAIN>.env
  <PLATFORM>.env
config.props           # Optional local overrides (not committed)
output/                # Generated artifacts (not committed)
work/                  # Temp work area (not committed)
docs/                  # Additional documentation
tests/                 # BATS test suite
  test_helper.bash     # Common setup sourced by all test files
  fixtures/
    make_fixtures.sh   # Script to generate minimal test EAR fixtures
    *.ear              # Generated test fixtures (not committed)
  01_utilities.bats    # Pure function unit tests
  02_analysis.bats     # EAR analysis integration tests
  03_config_parser.bats # config.props parser tests
```

## Script Architecture

The script is structured as follows (in order):

1. **Configuration** — default constants and AppManage command template
2. **Helper functions** — `log`, `err`, `die`, `trim_spaces`, `yq_set`, `to_k8s_name`, etc.
3. **Platform API functions** — `load_platform_env`, `platform_upload_and_deploy`, etc.
4. **Batch utilities** — `add_report_row`, `print_batch_report`
5. **Portability analysis engine** — `BWCE_CORE_PREFIXES`, `BWCE_PLUGIN_PREFIXES`, `_analysis_*` helpers, `analyze_ear_for_portability`, `print_analysis_summary`, `generate_cli_report`, `generate_html_report`
6. **Single-app export/deploy flow** — `export_app`, `generate_values`, `platform_upload_and_deploy`
7. **Batch flow** — `batch_export_apps`, `batch_process_app`
8. **Sourcing guard** — `[[ "${BASH_SOURCE[0]}" != "${0}" ]] && return 0`
9. **Argument parsing** — all CLI flag handling
10. **Main execution** — mode dispatch (deploy-offline, batch, single-app, custom)

The sourcing guard at step 8 allows BATS tests to `source` the script and call individual functions without triggering the main flow.

## Adding a New Analysis Check

1. Open `bw5ToCE.sh`
2. Find the appropriate scan function:
   - `_analysis_scan_process()` — for per-process-file checks
   - `_analysis_scan_shared_resources()` — for shared resource files
   - `analyze_ear_for_portability()` — for EAR-level checks (e.g. TIBCO.xml)
3. Add your check using one of:
   ```bash
   _analysis_add_blocker "$fname" "Short label" "Explanation and remediation"
   _analysis_add_warning "$fname" "Short label" "Explanation and remediation"
   _analysis_add_note    "$fname" "Short label" "Explanation and remediation"
   ```
4. Update [docs/portability-analysis.md](portability-analysis.md) with the new check
5. Add a corresponding BATS test in `tests/02_analysis.bats`

## Adding a Supported Plugin Prefix

If a new BWCE-supported plugin namespace should no longer be flagged as a BLOCKER:

1. Add the prefix to `BWCE_PLUGIN_PREFIXES` in `bw5ToCE.sh`:
   ```bash
   BWCE_PLUGIN_PREFIXES=(
     ...
     "com.tibco.plugin.newplugin."
   )
   ```
2. Update the supported plugin list in [docs/portability-analysis.md](portability-analysis.md)

## Code Style

- Use `printf` instead of `echo` for portable output
- Quote all variable expansions: `"$var"`, `"${array[@]}"`
- Use `[[ ... ]]` for conditions; avoid `[ ... ]`
- Use `local` for all function variables
- Avoid global `export` for sensitive values (passwords); prefer inline env: `VAR=val command`
- Avoid `grep -P` (Perl regex) — use `-E` for portability on macOS BSD grep
- Use `find ... -print0 | while IFS= read -r -d ''` for filenames with spaces

## Commit Messages

Follow conventional commits style:
- `feat:` new feature
- `fix:` bug fix
- `docs:` documentation only
- `refactor:` code change with no functional difference
- `test:` test changes

Example: `feat: add SWIFT plugin prefix to supported list`
