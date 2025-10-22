# BW5 Classic to CE

Utilities to migrate TIBCO BusinessWorks 5 (BW5) applications from TIBCO Administrator to CE (Control Plane). The main script automates EAR export, variable extraction, values injection, and deployment via the Platform API.

Main entry: `bw5-classic-to-ce.sh`

## Features

- Export single app EAR and deployment properties
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

Single app export → values → optional deploy
```sh
./bw5-classic-to-ce.sh <DOMAIN> <APP_NAME> \
  [--namespace <ns>] \
  [--platform <PLATFORM_ENV>] \
  [--offline | --no-deploy | --no-start] \
  [--debug]
```

Batch export all apps in a domain
```sh
./bw5-classic-to-ce.sh <DOMAIN> --batch \
  [--offline] [--namespace <ns>] [--platform <PLATFORM_ENV>] \
  [--no-deploy | --no-start] [--debug]
```

Deploy offline from existing artifacts under `output/`
```sh
./bw5-classic-to-ce.sh [<DOMAIN>] [<APP_NAME>] --deploy-offline --platform <PLATFORM_ENV>
```

Use custom artifacts (bypass AppManage)
```sh
./bw5-classic-to-ce.sh --app <NAME> --ear <PATH> --xml <PATH> \
  [--namespace <ns>] [--platform <PLATFORM_ENV>] \
  [--no-deploy | --no-start] [--debug]
```

### Behavior
- Loads `ADMIN_URL`/`ADMIN_USER`/`ADMIN_PASS` from `env/<DOMAIN>.env` (except `--deploy-offline`)
- Exports EAR and deployment properties (XML) using AppManage
- Generates YAML from Global Variables; updates repo `values.yaml` and saves a per‑app copy
- If `--platform` is provided, uploads and deploys via Platform API using `env/<PLATFORM>.env`
- Without `--platform`, deployment is skipped (artifacts are produced in `output/`)
- `--namespace` is optional; if omitted, no namespace is passed to the Platform API

### Modes and flags
- --namespace <ns>: target namespace for deploy (only sent if provided)
- --platform <name>: selects env/<name>.env for Platform API
- `--offline`: export artifacts only (EAR + props + values); no upload or deploy
- `--no-deploy`: upload EAR to Platform (requires `--platform`), but do not deploy
- `--no-start`: deploy with `replicaCount=0` (requires `--platform`)
- `--batch`: export all apps from a domain using AppManage `-batchExport`
- `--deploy-offline`: deploy using only artifacts from `output/` via Platform API (requires `--platform`)
- `--app <NAME> --ear <PATH> --xml <PATH>`: use provided EAR and deployment properties XML instead of exporting via AppManage. The script will place them under `output/<app>/` with the same naming convention, generate values from the XML, and proceed with the same deployment logic. Incompatible with `--batch` and `--offline`.
- `--debug`: verbose logging

### Quick examples

Single app export and deploy to platform
```sh
./bw5-classic-to-ce.sh tibco516 DynamicHeaders \
  --platform platform \
  --namespace bwce-dev
```

Export only (no deployment), artifacts go to output/
```sh
./bw5-classic-to-ce.sh tibco516 DynamicHeaders --offline
```

Batch export all apps from domain, then deploy offline from output/
```sh
./bw5-classic-to-ce.sh tibco516 --batch --offline
./bw5-classic-to-ce.sh --deploy-offline --platform platform
```

Deploy from custom artifacts
```sh
./bw5-classic-to-ce.sh \
  --app DynamicHeaders \
  --ear /path/to/DynamicHeaders.ear \
  --xml /path/to/DynamicHeaders.xml \
  --platform platform --namespace bwce-dev
```

## Output Structure

```
output/
  <app>/
    <app>-<timestamp>.ear
    <app>-deployment-props-<timestamp>.xml   # not produced when --offline
    <app>-global-variables-<timestamp>.yaml  # not produced when --offline
    <app>-values.yaml                         # includes fullnameOverride
```

The repo `values.yaml` is used as a base and updated in place with Global Variables on each run; a per‑app copy is stored alongside the exported EAR.

## Notes
- AppManage path resolution: if `TRA_HOME` is set, the script uses `"$TRA_HOME/bin/AppManage"`. Otherwise it defaults to `/opt/tibco/tra/5.13/bin/AppManage`. You can override with `APPMANAGE_BIN_FOLDER` or `APPMANAGE_BIN` (env or `config.props`).
- The script supports both mikefarah/yq v4 and python yq; it auto‑detects which is installed.
- In batch mode, the script parses AppManage output to derive per‑app names and sets `fullnameOverride` accordingly. A batch report is written to `output/batch-report-<domain>-<timestamp>.txt`.

## License

This project is licensed under the MIT License. See LICENSE for details.
