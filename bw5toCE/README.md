# BW5 Classic to CE

Utilities to move TIBCO BusinessWorks 5 (BW5) applications from TIBCO Administrator to CE (Control Plane). The main script automates EAR export, platform portability analysis, variable extraction, values injection, and deployment via the Platform API.

Main entry: `bw5ToCE.sh`

## Features

- Export single app EAR and deployment properties via AppManage
- **Platform portability analysis** — scan EAR for issues before transitioning (blockers, warnings, notes)
- Generate YAML from BW5 Global Variables and update Helm `values.yaml`
- Deploy via Platform API (upload + deploy)
- Batch export all apps in a domain using AppManage `-batchExport`
- Offline export/deploy workflows (produce artifacts only or deploy from `output/`)
- Optional `config.props` to override behavior

## Prerequisites

Install and have in `PATH`:

- AppManage (TIBCO BW5 utility)
- xmlstarlet
- yq (mikefarah v4 or python yq are supported)
- jq
- curl
- unzip (required for portability analysis)
- envsubst (from gettext) — for rendering command templates

Example (macOS/Homebrew):
```sh
brew install xmlstarlet yq jq curl
# envsubst is provided by gettext
brew install gettext && brew link --force gettext
# AppManage must be downloaded from TIBCO and added to your PATH.
```

## Environment Files

Place env files under `env/`.

Domain env file (`env/<DOMAIN>.env`):
```env
ADMIN_URL=http://admin-host:8080
ADMIN_USER=admin
ADMIN_PASS=admin
```

Platform env file (`env/<PLATFORM>.env`):
```env
PLATFORM_BW5CE_BASE_URL=https://platform-api.example.com
PLATFORM_BW5CE_BASE_VERSION=1.2.3
PLATFORM_BW5CE_BASE_IMAGE_TAG=ubi9-java17
PLATFORM_TOKEN=eyJhbGciOi...
```

Optional config file (`config.props`):
```properties
# KEY=VALUE entries; all optional
# AUTOPROVISION=true   # adds autoProvision=true to Platform upload
# ENV_DIR=./env        # base folder for env files
# WORK_DIR=./work      # temp work area
# OUTPUT_DIR=./output  # where artifacts are written
# VALUES_FILE=./values.yaml
# TRA_HOME=/opt/tibco/tra/5.13  # used to locate AppManage if APPMANAGE_* not set
# APPMANAGE_BIN_FOLDER=/opt/tibco/tra/5.13/bin
# APPMANAGE_BIN=/opt/tibco/tra/5.13/bin/AppManage
```

## Usage

The script uses **verb-style subcommands** so the intent is always clear at a glance.

```
./bw5ToCE.sh <verb> [DOMAIN] [APP_NAME] [flags...]
```

| Verb | What it does |
|------|-------------|
| `migrate` | Export from TIBCO Admin + analyze + deploy to Platform (full flow) |
| `export` | Export EAR and artifacts from TIBCO Admin only (no deploy) |
| `deploy` | Deploy pre-existing artifacts from `output/` to Platform |
| `analyze` | Run platform portability analysis only |

### Examples

Full migration (export → analyze → deploy)
```sh
./bw5ToCE.sh migrate tibco516 DynamicHeaders \
  --platform platform --namespace bwce-dev
```

Batch migration of all apps in a domain
```sh
./bw5ToCE.sh migrate tibco516 --batch --platform platform
```

Export only — artifacts go to `output/`, no deploy
```sh
./bw5ToCE.sh export tibco516 DynamicHeaders
./bw5ToCE.sh export tibco516 --batch
```

Deploy pre-existing artifacts from `output/` to Platform
```sh
./bw5ToCE.sh deploy --platform platform
./bw5ToCE.sh deploy --app DynamicHeaders --ear /path/to/DynamicHeaders.ear --xml /path/to/DynamicHeaders.xml --platform platform --namespace bwce-dev
```

Analyze portability without deploying
```sh
./bw5ToCE.sh analyze tibco516 DynamicHeaders
./bw5ToCE.sh analyze tibco516 DynamicHeaders --report output/report.html
./bw5ToCE.sh analyze --ear /path/to/MyApp.ear --app MyApp
```

### Behavior
- Loads `ADMIN_URL`/`ADMIN_USER`/`ADMIN_PASS` from `env/<DOMAIN>.env` (`migrate` and `export` only)
- Exports EAR and deployment properties (XML) using AppManage
- Generates YAML from Global Variables; updates repo `values.yaml` and saves a per-app copy
- If `--platform` is provided, uploads and deploys via Platform API using `env/<PLATFORM>.env`
- `--namespace` is optional; if omitted, no namespace is passed to the Platform API

### Flags

| Flag | Applies to | Description |
|------|-----------|-------------|
| `--platform <name>` | `migrate`, `deploy` | Platform environment (uses `env/<name>.env`) |
| `--namespace <ns>` | `migrate`, `deploy` | Target namespace for deploy |
| `--batch` | `migrate`, `export` | Export all apps in the domain |
| `--no-deploy` | `migrate` | Upload EAR to Platform but do not deploy |
| `--no-start` | `migrate`, `deploy` | Deploy with `replicaCount=0` |
| `--force` | `migrate`, `deploy` | Upgrade if app already exists |
| `--no-analyze` | `migrate` | Skip portability analysis |
| `--allow-blockers` | `migrate` | Deploy even if BLOCKER issues found |
| `--no-best-practices` | `migrate`, `analyze` | Suppress quality suggestions in report |
| `--report [<path>]` | `migrate`, `analyze` | Generate HTML readiness report |
| `--report-cli [<path>]` | `migrate`, `analyze` | Generate plain-text readiness report |
| `--app <name>` | `deploy`, `analyze` | Application name (with `--ear`/`--xml`) |
| `--ear <path>` | `deploy`, `analyze` | Path to existing EAR file |
| `--xml <path>` | `deploy` | Path to deployment properties XML |
| `--insecure-tls` | any | Disable TLS certificate verification |
| `--debug` | any | Verbose logging |

### Legacy flag syntax (still supported)

The original flag-only syntax continues to work without changes.

| Old syntax | Equivalent new syntax |
|-----------|----------------------|
| `./bw5ToCE.sh DOMAIN APP --platform P` | `./bw5ToCE.sh migrate DOMAIN APP --platform P` |
| `./bw5ToCE.sh DOMAIN APP --offline` | `./bw5ToCE.sh export DOMAIN APP` |
| `./bw5ToCE.sh DOMAIN APP --analyze-only` | `./bw5ToCE.sh analyze DOMAIN APP` |
| `./bw5ToCE.sh --deploy-offline --platform P` | `./bw5ToCE.sh deploy --platform P` |
| `./bw5ToCE.sh --app N --ear E --xml X --platform P` | `./bw5ToCE.sh deploy --app N --ear E --xml X --platform P` |
| `./bw5ToCE.sh DOMAIN --batch --platform P` | `./bw5ToCE.sh migrate DOMAIN --batch --platform P` |
| `./bw5ToCE.sh DOMAIN --batch --offline` | `./bw5ToCE.sh export DOMAIN --batch` |

## Portability Analysis

The script includes a built-in platform portability engine that scans EAR files for issues before transitioning. Findings are grouped by severity:

| Level | Meaning |
|---|---|
| **BLOCKER** | Must be resolved before platform onboarding can proceed |
| **WARNING** | Behavior will differ in containers; review required |
| **NOTE** | Items to plan for cloud-native adaptation |

By default, blockers halt the deployment. Use `--allow-blockers` to override.

See [docs/portability-analysis.md](docs/portability-analysis.md) for the full list of checks and remediation guidance.

## Output Structure

```
output/
  <app>/
    <app>-<timestamp>.ear
    <app>-deployment-props-<timestamp>.xml   # not produced when --offline
    <app>-global-variables-<timestamp>.yaml  # not produced when --offline
    <app>-values.yaml                         # includes fullnameOverride
    <app>-report-<timestamp>.html             # if --report used
    <app>-report-<timestamp>.txt              # if --report-cli <file> used
```

The repo `values.yaml` is used as a base and updated in place with Global Variables on each run; a per‑app copy is stored alongside the exported EAR.

## Notes

- **AppManage path resolution**: if `TRA_HOME` is set, the script uses `"$TRA_HOME/bin/AppManage"`. Otherwise defaults to `/opt/tibco/tra/5.13/bin/AppManage`. Override with `APPMANAGE_BIN_FOLDER` or `APPMANAGE_BIN` (env or `config.props`).
- **yq compatibility**: supports both mikefarah/yq v4 and python yq; auto-detected at runtime.
- **Batch mode**: parses AppManage output to derive per-app names and sets `fullnameOverride` accordingly. A batch report is written to `output/batch-report-<domain>-<timestamp>.txt`.

## Documentation

- [Portability Analysis Guide](docs/portability-analysis.md) — detailed analysis checks and remediation
- [Configuration Reference](docs/configuration.md) — all flags, env vars, and config.props options
- [Contributing](docs/contributing.md) — how to develop and contribute to the project

## License

This project is licensed under the MIT License. See LICENSE for details.
