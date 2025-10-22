# Changelog

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
