# Changelog

## Unreleased

### Compatibility

- **Bash 4.2 support** — the script no longer requires Bash 4.3+. Negative
  array subscripts and namerefs (`local -n`) were replaced with 4.2-compatible
  equivalents, and a startup guard now fails fast with a clear message on
  anything older than 4.2 (unblocks legacy hosts such as RHEL 7).

### New portability checks

- **Java Code — generic review (NOTE)** — any `com.tibco.plugin.java.*` activity
  raises a note to review embedded custom Java for container portability.
- **Java Code — APIs removed in Java 17 (WARNING)** — heuristic static scan of the
  embedded source for packages removed or disabled in Java 17 (JAXB, JAX-WS,
  `javax.activation`/`javax.annotation`, CORBA, Nashorn, RMI Activation,
  `sun.misc.*`). No compiler or extra dependency required.
- **Custom Adapter (NOTE)** — an adapter whose `componentSoftwareName` is not a
  recognized TIBCO adapter is now reported as a custom-adapter note ("ensure it
  is included in the image") instead of a blocker.

### Analysis fixes

- **Shared Variable / Module Shared Variable** — warn when the variable is
  multi-engine and/or persistent, indicating it must be backed by a database
  (the DB backing is decided at deployment and cannot be inferred from the EAR,
  so the warning is always raised for review).
- **Custom adapter detection** — TIBCO adapter AARs store entries with absolute
  paths, which makes `unzip` exit 1 (a warning). The analyzer treated that as a
  failure and skipped the adapter, so real custom adapters were missed and apps
  reported READY. Extraction now tolerates unzip's warning status (fails only on
  real errors), for EAR, PAR, SAR and AAR archives.
- Fixed the Wait/Notify test fixture (was missing its Notify Configuration
  shared resource) and a typo in a supported-type unit test.

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
