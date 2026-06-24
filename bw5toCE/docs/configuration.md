# Configuration Reference

All configurable behaviour is controlled through a combination of CLI flags, environment files, and an optional `config.props` file.

## CLI Flags

| Flag | Default | Description |
|---|---|---|
| `--namespace <ns>` | _(none)_ | Target Kubernetes namespace for deploy; omit to use platform default |
| `--platform <name>` | _(none)_ | Load `env/<name>.env` for Platform API; required for upload/deploy |
| `--offline` | `false` | Export EAR + values only; skip upload and deploy |
| `--no-deploy` | `false` | Upload to Platform but do not trigger deploy |
| `--no-start` | `false` | Deploy with `replicaCount=0`; app is deployed but not started |
| `--force` | `false` | Force upgrade even if an app with the same buildId is already deployed |
| `--batch` | `false` | Export all apps in the domain via AppManage `-batchExport` |
| `--deploy-offline` | `false` | Deploy all apps found under `output/` using the Platform API |
| `--app <NAME>` | _(none)_ | Override app name; used with `--ear` and `--xml` to bypass AppManage |
| `--ear <PATH>` | _(none)_ | Path to a pre-built EAR file |
| `--xml <PATH>` | _(none)_ | Path to a deployment properties XML file |
| `--analyze-only` | `false` | Run portability analysis only; no export or deploy |
| `--report [<path>]` | _(auto)_ | Generate an HTML platform portability report |
| `--report-cli [<path>]` | _(stdout)_ | Print/write plain-text portability report |
| `--allow-blockers` | `false` | Continue deployment even when BLOCKER issues are found |
| `--insecure-tls` | `false` | Skip TLS certificate verification for Platform API calls |
| `--debug` | `false` | Enable verbose logging with timestamps |
| `--version` | — | Print version and author |
| `-h`, `--help` | — | Print usage |

## Domain Environment File

Located at `env/<DOMAIN>.env`. Loaded automatically when `<DOMAIN>` is provided as the first positional argument.

```env
ADMIN_URL=http://bw-admin-host:8080
ADMIN_USER=admin
ADMIN_PASS=secret
```

| Variable | Required | Description |
|---|---|---|
| `ADMIN_URL` | Yes | Base URL of the TIBCO Administrator server |
| `ADMIN_USER` | Yes | Administrator username |
| `ADMIN_PASS` | Yes | Administrator password |

## Platform Environment File

Located at `env/<PLATFORM>.env`. Loaded when `--platform <name>` is passed.

```env
PLATFORM_BW5CE_BASE_URL=https://platform.example.com/tibco/bw5ce/d1234abcd
PLATFORM_BW5CE_BASE_VERSION=preview-0.1
PLATFORM_BW5CE_BASE_IMAGE_TAG=88-5.16.2-V9-5.13.1-V10-debian
PLATFORM_TOKEN=CIC~...
```

| Variable | Required | Description |
|---|---|---|
| `PLATFORM_BW5CE_BASE_URL` | Yes | Platform API base URL (includes tenant/subscription path) |
| `PLATFORM_BW5CE_BASE_VERSION` | Yes | Chart/app version used for deployment |
| `PLATFORM_BW5CE_BASE_IMAGE_TAG` | Yes | Base image tag applied to the deployed app |
| `PLATFORM_TOKEN` | Yes | Bearer token for Platform API authentication |

## config.props

Optional file at `./config.props` (or set `CONFIG_PROPS_FILE=<path>` in the environment). `KEY=VALUE` format; lines starting with `#` are ignored.

```properties
# Path overrides
ENV_DIR=./env
WORK_DIR=./work
OUTPUT_DIR=./output
VALUES_FILE=./values.yaml

# AppManage location
TRA_HOME=/opt/tibco/tra/5.13
# APPMANAGE_BIN_FOLDER=/opt/tibco/tra/5.13/bin
# APPMANAGE_BIN=/opt/tibco/tra/5.13/bin/AppManage

# Platform behavior
AUTOPROVISION=true
```

| Key | Default | Description |
|---|---|---|
| `ENV_DIR` | `./env` | Directory where `<DOMAIN>.env` and `<PLATFORM>.env` are resolved |
| `WORK_DIR` | `./work` | Temp directory for AppManage exports and intermediate files |
| `OUTPUT_DIR` | `./output` | Directory where exported artifacts and reports are saved |
| `VALUES_FILE` | `./values.yaml` | Base Helm values file updated with Global Variables |
| `TRA_HOME` | _(none)_ | Path to TRA installation; sets AppManage location if `APPMANAGE_BIN*` not set |
| `APPMANAGE_BIN_FOLDER` | `$TRA_HOME/bin` or `/opt/tibco/tra/5.13/bin` | Folder containing AppManage binary and `.tra` file |
| `APPMANAGE_BIN` | `$APPMANAGE_BIN_FOLDER/AppManage` | Full path to AppManage executable |
| `AUTOPROVISION` | `false` | If `true`, adds `autoProvision=true` to the Platform upload request |

## AppManage Command Template

The AppManage export command is rendered via `envsubst` from:

```
APPMANAGE_EXPORT_EAR_TMPL='${APPMANAGE_BIN} --propFile ${APPMANAGE_BIN_FOLDER}/AppManage.tra -export \
  -domain "${DOMAIN}" -app "${APP_NAME}" \
  -user "${ADMIN_USER}" -pw "${ADMIN_PASS}" \
  -out "${EAR_PATH}.xml" -ear "${EAR_PATH}" -genEar'
```

To adapt for BW5 versions that use `-host`/`-port` instead of the default flags, override this template by setting `APPMANAGE_EXPORT_EAR_TMPL` in `config.props` or the environment.

## values.yaml

The script reads `values.yaml` as the base Helm values and updates:

- `appConfig.appId` — set to the normalized app name
- `appConfig.buildId` — set to the buildId returned by the Platform upload
- `appConfig.bwProfile` — fixed to `default.substvar`
- `appConfig.tags` — derived from folder hierarchy in the BW5 app display name
- `appProps.default.substvar` — populated with all BW5 Global Variables
- `fullnameOverride` — set to the k8s-normalized app name

These fields are updated in-place in the repo `values.yaml` and also saved to `output/<app>/<app>-values.yaml`.

## Output Files

| File | Description |
|---|---|
| `output/<app>/<app>-<ts>.ear` | Exported EAR |
| `output/<app>/<app>-deployment-props-<ts>.xml` | Deployment properties XML |
| `output/<app>/<app>-global-variables-<ts>.yaml` | Extracted Global Variables as YAML |
| `output/<app>/<app>-values.yaml` | Merged Helm values snapshot for this app |
| `output/<app>/<app>-report-<ts>.html` | HTML portability report (if `--report` used) |
| `output/<app>/<app>-report-<ts>.txt` | Plain-text portability report (if `--report-cli <file>` used) |
| `output/batch-report-<domain>-<ts>.txt` | Batch mode summary table |
