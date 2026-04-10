# Portability Analysis Guide

The platform portability engine scans BW5 EAR files for patterns that are incompatible with or require attention in TIBCO BusinessWorks 5 (Containers). It is built into `bw5ToCE.sh` and runs automatically before every deployment.

## Running the Analysis

**Analyze only, print to terminal:**
```sh
./bw5ToCE.sh tibco516 MyApp --analyze-only
```

**Analyze with HTML report:**
```sh
./bw5ToCE.sh tibco516 MyApp --analyze-only --report output/MyApp-report.html
```

**Analyze and print a plain-text report file:**
```sh
./bw5ToCE.sh tibco516 MyApp --analyze-only --report-cli output/MyApp-report.txt
```

**Analyze a local EAR without a running BW5 domain:**
```sh
./bw5ToCE.sh --app MyApp --ear /path/to/MyApp.ear --analyze-only
```

**Export, analyze, and deploy (analysis runs automatically before deploy):**
```sh
./bw5ToCE.sh tibco516 MyApp --platform platform --namespace bwce-dev
```

Analysis results are always printed to the terminal (console summary). Additionally:
- `--report [<file>]` generates an HTML report (self-contained, no external deps)
- `--report-cli [<file>]` writes a plain-text report to a file, or to stdout if no path given

By default, blockers stop the deployment. Pass `--allow-blockers` to override and deploy anyway (at your own risk).

## Severity Levels

| Level | Prefix | Meaning |
|---|---|---|
| **BLOCKER** | `[B]` | Functionality requires attention before transitioning to TIBCO BusinessWorks 5 (Containers) |
| **WARNING** | `[W]` | Behavior differs from BW5 Classic; review and test carefully in the target environment |
| **NOTE** | `[N]` | Architectural consideration for cloud-native deployment; review and validate design |

## Checks Reference

### BLOCKERS

#### HTTP Basic Auth (server-mode)
**Trigger:** Any of the following server-side (inbound) resources have Basic Authentication enabled:

| Resource | XML element detected |
|---|---|
| `HTTPReceiver` (`com.tibco.plugin.http.HTTPEventSource`) in a `.process` file | `<useHTTPAuthentication>true</useHTTPAuthentication>` |
| `SOAPEventSource` (`com.tibco.plugin.soap.SOAPEventSource`) in a `.process` file | `<useBasicAuthentication>true</useBasicAuthentication>` |
| `ServiceAgent` (`.serviceagent` resource file) | `<useBasicAuthentication>true</useBasicAuthentication>` |

**Why it blocks:** HTTP Basic Auth in BW5 Classic relies on TIBCO Administrator domain users, which are not available in the Containers runtime. Cloud-native best practice is to externalize authentication outside the engine, using API Gateway, Ingress, or TIBCO Cloud API Management instead.

**Remediation:**
- Remove Basic Auth from the endpoint and enforce authentication at the API gateway or ingress layer
- Use TIBCO Cloud API Management or an equivalent gateway solution
- Switch to token-based authentication (Bearer/OAuth2) enforced at the gateway
- Use mutual TLS (client certificates) at the platform level

---

#### Not Yet Available: Plugin or Adapter
**Trigger:** Any `com.tibco.*` activity type prefix not in the supported core or plugin list, or an adapter (`componentSoftwareName`) in the known-unsupported list.

**Why it blocks:** TIBCO is continuously expanding the capabilities of TIBCO BusinessWorks 5 (Containers). Some plugins and adapters present in BW5 Classic are not yet available in the current version of the Containers runtime.

**Remediation:**
- Contact your TIBCO representative for detailed timelines on when the plugin or adapter may become available
- Check for availability in a future release
- Review the [supported plugin list](#supported-plugins) below for currently available options

---

### WARNINGS

#### Wait & Notify — Review Scope
**Trigger:** `WaitForNotif`, `WaitNotif`, or `NotifyActivity` type found.

**Why it warns:** TIBCO BusinessWorks 5 (Containers) follows standard Kubernetes practices where each instance is independent. If the Wait/Notify pattern is scoped to a single instance, it works as expected. For cross-instance scenarios, there is no out-of-the-box inter-instance communication for this feature.

**Remediation:**
- If single-instance scope is acceptable, set `replicaCount: 1` — no changes needed
- For cross-instance scenarios, replace with a JMS-based publish/subscribe pattern (e.g., TIBCO EMS topics)

---

#### Checkpoint — DB Storage
**Trigger:** `CheckpointActivity` found and TIBCO.xml references a database-backed checkpoint repository.

**Why it warns:** Checkpoint with database storage is fully supported in TIBCO BusinessWorks 5 (Containers). We recommend validating behavior under autoscaling and multi-replica deployments to ensure checkpoint consistency.

**Remediation:**
- Test checkpoint behavior with `replicaCount > 1`
- Ensure only one replica writes to a given checkpoint key if using auto-scaling

---

#### Checkpoint — File Storage
**Trigger:** `CheckpointActivity` found and TIBCO.xml does not reference a database-backed checkpoint repository.

**Why it warns:** File-based checkpoint storage requires additional persistent storage such as a PersistentVolumeClaim (PVC) and volume mount. For the best experience in a containerized environment, switching to a JDBC-based Checkpoint Data Repository is recommended.

**Remediation:**
- Configure checkpoint storage to use a JDBC database
- Verify the `BWDatabase*` global variables in TIBCO.xml point to a live DB
- Add a PVC and volume mount if file-based storage must be retained

---

#### Engine Command — Operational Lifecycle Review
**Trigger:** `com.tibco.pe.core.EngineCommandActivity` with `<command>` set to any of: `Shutdown`, `SuspendProcessInstance`, `SuspendProcessStarter`, `ResumeProcessInstance`, `ResumeProcessStarter`.

**Why it warns:** In BW5 Classic, these lifecycle commands are typically triggered by external operational tooling such as Hawk Microagents or RedTail. In TIBCO BusinessWorks 5 (Containers), runtime lifecycle management is handled natively by Kubernetes and the TIBCO Platform — through pod lifecycle management, health probes, and the Control Plane. The external tooling that drives these commands may not be available, or may operate differently, in the Platform environment.

Non-lifecycle commands such as `GetActivityStats` are not flagged.

**Remediation:**
- Identify any operational workflows or external tools (Hawk, RedTail, custom scripts) that invoke these commands
- Map those workflows to equivalent Kubernetes or TIBCO Platform capabilities (e.g., pod termination, readiness probes, Control Plane APIs)
- Remove or replace the Engine Command Activities with the appropriate cloud-native lifecycle management approach

---

#### External Command Activity — Review Base Image
**Trigger:** Any `com.tibco.plugin.generalactivities.ExternalCommandActivity` type found.

**Why it warns:** External Command Activities execute OS-level commands and rely on binaries being available inside the container image. The TIBCO BusinessWorks 5 (Containers) base image may not include all required commands or utilities, which could cause runtime failures if the expected binary is not present.

**Remediation:**
- Review each External Command Activity and identify the OS commands it invokes
- Verify that the required binaries are present in the TIBCO BusinessWorks 5 (Containers) base image
- If binaries are missing, build a custom base image that includes the additional dependencies
- Consider replacing OS-level commands with native BW activities (JDBC, REST, File, etc.) where possible

---

#### Module Shared Variable — Review Scope
**Trigger:** A process file references a `.moduleSharedVariable` resource.

**Why it warns:** Module Shared Variables are in-memory and local to each engine instance. If the scope is single-instance, this works as expected. For cross-instance scenarios, values are not shared across pods, and the design should be reviewed.

**Remediation:**
- If single-instance scope is acceptable, set `replicaCount: 1` — no changes needed
- For cross-instance scenarios, switch to a DB-persisted Shared Variable (JDBC)

---

#### Module Shared Variable — Non-DB Persistence
**Trigger:** A `.moduleSharedVariable` shared resource file has `persistence` not set to `database` or `jdbc`.

**Why it warns:** Module Shared Variable with non-database persistence detected. File-based storage requires a PersistentVolumeClaim (PVC) and volume mount. For cross-instance sharing, switching to JDBC-based persistence is recommended to ensure consistency across replicas.

**Remediation:** Configure JDBC persistence in the shared variable resource definition, or add a PVC and volume mount if file-based storage must be retained.

---

### NOTES

#### File I/O — Review Storage Design
**Trigger:** Any `com.tibco.plugin.file.*` activity type.

**Why it's noted:** Temporary or internal files work as expected, though container storage is ephemeral. Read-only content may require a volume mount. If files need to be shared outside the application scope or with other parties, the design should be reviewed.

**Remediation:**
- Temporary or internal files: no action needed by default, though pod restart will clear them
- Read-only content: mount a ConfigMap or PersistentVolumeClaim
- Files shared with other applications or parties: use a PersistentVolumeClaim (PVC) or object storage (e.g., S3 via REST)
- Document which directories are read/written and ensure they are externalized in `values.yaml`

---

#### TIBCO Rendezvous — Review Deployment Design
**Trigger:** Any `com.tibco.plugin.rendezvous.*` activity type.

**Why it's noted:** TIBCO Rendezvous activities detected. RV in a cloud environment may require TIBCO TRNS software or additional configuration. Re-evaluating the design to determine if a TIBCO Messaging alternative can be used is recommended.

**Remediation:**
- Test carefully and account for TIBCO TRNS software or additional network configuration in your Kubernetes environment
- Re-evaluate the design: TIBCO EMS or TIBCO Cloud Messaging are functionally equivalent for most messaging use cases and are cloud-native friendly
- If RV is mandatory, explore a sidecar `rvd` container pattern (advanced configuration)

---

#### Fault Tolerant Group — Cloud-Native HA
**Trigger:** `FaultTolerant`, `ftgroup`, or `FTGroup` references in process XML.

**Why it's noted:** TIBCO BusinessWorks 5 (Containers) leverages Kubernetes built-in high availability through Deployment replicas, health probes, and self-healing — providing equivalent resilience natively. Reviewing your design to take full advantage of these cloud-native HA capabilities is recommended.

**Remediation:**
- Remove FT Group configuration; Kubernetes handles failover automatically
- Use `replicaCount > 1` with Kubernetes liveness/readiness probes for active-active HA
- Ensure processes are stateless (see Shared Variables and Checkpoint notes above)

---

## Supported Plugins

The following plugin namespaces are considered supported. Activities outside these prefixes are flagged as requiring attention.

**Core (always in the base image):**
- `com.tibco.bw.*` — BW engine core
- `com.tibco.pe.*` — Process engine
- `com.tibco.plugin.file.*` — File
- `com.tibco.plugin.ftp.*` — FTP
- `com.tibco.plugin.soap.*` — SOAP
- `com.tibco.plugin.http.*` — HTTP
- `com.tibco.plugin.jdbc.*` — JDBC
- `com.tibco.plugin.jms.*` — JMS
- `com.tibco.plugin.ems.*` — EMS
- `com.tibco.plugin.mail.*` — Mail
- `com.tibco.plugin.rendezvous.*` — Rendezvous
- `com.tibco.plugin.timer.*` — Timer
- `com.tibco.plugin.java.*` — Java
- `com.tibco.plugin.xml.*` — XML
- `com.tibco.plugin.xslt.*` — XSLT
- `com.tibco.plugin.mapper.*` — Mapper
- `com.tibco.plugin.generalactivities.*` — General Activities
- `com.tibco.plugin.shared.*` — Shared resources
- `com.tibco.plugin.noop.*` — No-op
- `com.tibco.plugin.log.*` — Log
- `com.tibco.plugin.parse.*` — Parse (core BW5)

**Supported adapters/plugins (require separate plugin images):**
- `com.tibco.plugin.adb.*` — ADB
- `com.tibco.plugin.sap.*` — SAP
- `com.tibco.plugin.filesadapter.*` — Files Adapter
- `com.tibco.plugin.ae.fileadapter.*` — AE File Adapter
- `com.tibco.plugin.siebel.*` — Siebel
- `com.tibco.plugin.ldap.*` — LDAP
- `com.tibco.plugin.sp.*` — SFTP (`com.tibco.plugin.sp.SFTP*`)
- `com.tibco.plugin.bwlx.*` — Large XML
- `com.tibco.plugin.json.*` — REST/JSON (`com.tibco.plugin.json.activities.*`)
- `com.tibco.bw.palette.rest.*` — BW REST Palette
- `com.tibco.plugin.salesforce.*` — Salesforce
- `com.tibco.plugin.mongodb.*` — MongoDB
- `com.tibco.plugin.kafka.*` — Kafka
- `com.tibco.plugin.pulsar.*` — Pulsar
- `com.tibco.plugin.ax.bc.*` — B2B Connector
- `com.tibco.plugin.iProcessForms.*` — iProcess
- `com.tibco.plugin.staffware.*` — iProcess (alternate namespace)
- `com.tibco.plugin.dataconversion.*` — Data Conversion
- `com.tibco.plugin.bwmq.*` — IBM MQ
- `com.tibco.plugin.workday.*` — Workday
- `com.tibco.plugin.oracleebs.*` — Oracle E-Business Suite
- `com.tibco.plugin.pdf.*` — PDF
- `com.tibco.plugin.sharepoint.*` — SharePoint
- `com.tibco.swift2.bwplugin.*` — SWIFT

## Not Yet Available

The following plugins and adapters are **not yet available** in the current version of TIBCO BusinessWorks 5 (Containers). They are reported with a specific label. TIBCO is continuously expanding the platform's capabilities — contact your TIBCO representative for timelines.

**Plugins (detectable via `<pd:type>` in process files):**

| Display label | Type prefix |
|---|---|
| EJB Plugin | `com.tibco.plugin.ejb.*` |
| Mobile Integration Plugin | `com.tibco.plugin.bwmi.*` |
| NetSuite Plugin | `com.tibco.plugin.netsuite.*` |
| SmartMapper Plugin | `com.tibco.solution.xref.plugin.activity.*` |
| ActiveSpaces 1/2 Plugin | `com.tibco.plugin.firefly.activities.*` |
| CICS Mainframe Plugin | `com.tibco.plugin.cicspi.*` |
| HL7 Plugin | `com.tibco.plugin.hl7.*` |

**Adapters not detectable via process scanning:**

JD Edwards (`adjdexe`), PeopleSoft (`adpsft8`), OSIsoft PI (`adpi`), Tuxedo (`adtuxedo`), and EDI have no entries in `PluginActivityMap` — they are pure Adapter SDK resources detected via adapter descriptor files.

## How the Analysis Works

1. The EAR is extracted to a temp directory
2. `TIBCO.xml` is inspected for `BWDatabase*` global variables with a "Checkpoint Data Repository" description to infer checkpoint storage type
3. Each `.par` (process archive) is extracted; every `.process` XML file is scanned:
   - Activity types are extracted via `<pd:type>TYPE</pd:type>` elements
   - Checks run against server-mode HTTP Basic Auth, unsupported types, wait/notify, checkpoints, shared variables, file I/O, RV, FT groups
   - Every `.serviceagent` resource file is also scanned for server-mode HTTP Basic Auth (`useBasicAuthentication=true`)
4. Each `.sar` (shared archive) is extracted; `.moduleSharedVariable` files are inspected for persistence configuration
5. Results are collected in `ANALYSIS_BLOCKERS`, `ANALYSIS_WARNINGS`, `ANALYSIS_NOTES` arrays
6. Summary is printed; HTML and/or CLI reports are generated if requested
