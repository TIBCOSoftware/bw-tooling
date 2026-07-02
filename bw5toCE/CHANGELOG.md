# Changelog

## 0.2

### New features

- **Verb-style subcommands** — `migrate`, `export`, `analyze`, and `deploy` replace the flag-only syntax. Old flags (`--offline`, `--analyze-only`, `--deploy-offline`) remain supported for backward compatibility.

### AppManage error detection overhaul

- Reads `$TRA_HOME/domain/<DOMAIN>/logs/ApplicationManagement.log` after each AppManage invocation for richer, exception-level error messages (e.g. `EntityException: Application not found`).
- Scans both the log file and stdout so auth failures (reported only to stdout) and app-not-found exceptions (reported only to the log) are both caught.
- New `_am_scan_lines` helper processes output line-by-line without crashing under `set -euo pipefail`.
- Filters Java stack trace lines (`\tat `) and strips log4j timestamp/class prefixes from displayed messages.
- Categorised error messages for: authentication failure, domain not found, connection refused, application not found, generic AppManage failures.
- Fixed race condition where `tail -f --pid` could exit before reading all AppManage output; now captures to a temp file and reads after process completion.

### Other improvements

- ShellCheck (`-S style`) passes with zero findings (SC2012, SC2015, SC2016, SC2034, SC2059, SC2155, SC2317 addressed).
- README updated with verb-syntax examples, per-verb flag table, and legacy-to-new syntax comparison table.

## 0.1 — Initial release

- Initial script version with support for:
  - Exporting EAR and deployment properties via AppManage
  - Generating YAML from BW5 Global Variables and updating `values.yaml`
  - Deploying via Platform API (upload + deploy), including offline and batch modes
  - yq v4 compatibility (`eval`/`eval-all`) and python yq support
  - Environment variable rename to `PLATFORM_BW5CE_*` with backward-compat shim
  - Helper refactors for maintainability (yq wrappers, fullnameOverride helper)
  - Safer command execution and latest-file detection
  - Custom artifacts mode: `--app <NAME> --ear <PATH> --xml <PATH>`
