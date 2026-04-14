#!/usr/bin/env bash
# Generate minimal BW5 EAR fixtures for use by the BATS test suite.
# Run once before running tests: bash tests/fixtures/make_fixtures.sh
set -euo pipefail

FIXTURES_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

require_bin() { command -v "$1" >/dev/null 2>&1 || { echo "ERROR: $1 not in PATH" >&2; exit 1; }; }
require_bin zip
require_bin unzip

make_process_xml() {
  local types_xml="$1"   # pre-built <pd:type>...</pd:type> blocks
  local extra="$2"       # extra XML content appended inside <pd:ProcessDefinition>
  cat <<XML
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>TestProcess</pd:name>
  <pd:startName>Start</pd:startName>
  <pd:activity>
    <pd:name>Start</pd:name>
    <pd:type>com.tibco.pe.core.OnStartupEventSource</pd:type>
  </pd:activity>
${types_xml}
${extra}
</pd:ProcessDefinition>
XML
}

# Build a minimal .aar for testing: just a TIBCO.xml with <componentSoftwareName>
make_aar() {
  local sw_name="$1"   # componentSoftwareName value
  local out_aar="$2"   # output .aar path
  local tmp_aar
  tmp_aar="$(mktemp -d)"
  trap 'rm -rf "$tmp_aar"' RETURN
  cat > "$tmp_aar/TIBCO.xml" <<AAREOF
<?xml version="1.0" encoding="UTF-8"?>
<DeploymentDescriptors xmlns="http://www.tibco.com/xmlns/dd">
  <StartAsOneOf>
    <ComponentSoftwareReference>
      <componentSoftwareName>${sw_name}</componentSoftwareName>
      <keyword>Adapter</keyword>
    </ComponentSoftwareReference>
  </StartAsOneOf>
</DeploymentDescriptors>
AAREOF
  (cd "$tmp_aar" && zip -qr "$out_aar" .)
}

make_ear() {
  local name="$1"     # fixture name, no extension
  local par_dir="$2"  # directory containing *.process files
  local sar_dir="${3:-}"
  local tibco_xml="${4:-}"  # optional TIBCO.xml content
  local aar_dir="${5:-}"    # optional directory containing pre-built *.aar files
  local out_ear="$FIXTURES_DIR/${name}.ear"

  local tmp
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN

  # Build PAR
  local par_tmp="$tmp/par"
  mkdir -p "$par_tmp"
  cp "$par_dir"/*.process "$par_tmp/" 2>/dev/null || true
  cp "$par_dir"/*.serviceagent "$par_tmp/" 2>/dev/null || true
  cp "$par_dir"/*.sharedhttp "$par_tmp/" 2>/dev/null || true
  cp "$par_dir"/*.sharedjdbc "$par_tmp/" 2>/dev/null || true
  cp "$par_dir"/*.jms "$par_tmp/" 2>/dev/null || true
  (cd "$par_tmp" && zip -qr "$tmp/${name}.par" .)

  # Build EAR
  local ear_tmp="$tmp/ear"
  mkdir -p "$ear_tmp"
  cp "$tmp/${name}.par" "$ear_tmp/"

  if [[ -n "$tibco_xml" ]]; then
    printf '%s\n' "$tibco_xml" > "$ear_tmp/TIBCO.xml"
  else
    cat > "$ear_tmp/TIBCO.xml" <<'TXEOF'
<?xml version="1.0" encoding="UTF-8"?>
<repository xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <globalvariables/>
</repository>
TXEOF
  fi

  if [[ -n "$sar_dir" ]]; then
    local sar_tmp="$tmp/sar"
    mkdir -p "$sar_tmp"
    cp "$sar_dir"/* "$sar_tmp/" 2>/dev/null || true
    (cd "$sar_tmp" && zip -qr "$tmp/${name}.sar" .)
    cp "$tmp/${name}.sar" "$ear_tmp/"
  fi

  if [[ -n "$aar_dir" ]]; then
    cp "$aar_dir"/*.aar "$ear_tmp/" 2>/dev/null || true
  fi

  (cd "$ear_tmp" && zip -qr "$out_ear" .)
  echo "Created: $out_ear"
}

# ------------------------------------------------------------------ #
# Fixture: clean.ear — no issues, only core activities
# ------------------------------------------------------------------ #
PROC_DIR="$(mktemp -d)"
trap 'rm -rf "$PROC_DIR"' EXIT

cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Start</pd:name>
    <pd:type>com.tibco.pe.core.OnStartupEventSource</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>Log</pd:name>
    <pd:type>com.tibco.pe.core.WriteToLogActivity</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>RESTCall</pd:name>
    <pd:type>com.tibco.plugin.restjson.RESTActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "clean" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: http_basic_auth.ear — HTTPReceiver with Basic Auth (BLOCKER)
# Mirrors: Process Definition.process from TestHTTPBasic sample
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:startName>HTTP Receiver</pd:startName>
  <pd:starter name="HTTP Receiver">
    <pd:type>com.tibco.plugin.http.HTTPEventSource</pd:type>
    <pd:resourceType>httppalette.httpEventSource</pd:resourceType>
    <config>
      <outputMode>String</outputMode>
      <useHTTPAuthentication>true</useHTTPAuthentication>
    </config>
  </pd:starter>
</pd:ProcessDefinition>
EOF
make_ear "http_basic_auth" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: soap_event_source_basic_auth.ear — SOAPEventSource with Basic Auth (BLOCKER)
# Mirrors: Process Definition (1).process from TestHTTPBasic sample
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:startName>SOAPEventSource</pd:startName>
  <pd:starter name="SOAPEventSource">
    <pd:type>com.tibco.plugin.soap.SOAPEventSource</pd:type>
    <pd:resourceType>ae.activities.SOAPEventSourceUI</pd:resourceType>
    <config>
      <sharedChannel>/HTTP Connection.sharedhttp</sharedChannel>
      <useBasicAuthentication>true</useBasicAuthentication>
    </config>
  </pd:starter>
</pd:ProcessDefinition>
EOF
make_ear "soap_event_source_basic_auth" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: service_agent_basic_auth.ear — ServiceAgent with Basic Auth (BLOCKER)
# Mirrors: Service.serviceagent from TestHTTPBasic sample
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/Service.serviceagent" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<serviceResource>
  <config>
    <class>com.tibco.bw.service.serviceAgent.ServiceServiceAgent</class>
    <implType>bw</implType>
    <interfaceImpl>
      <tab>
        <tabName>TestService</tabName>
        <tabType>bw</tabType>
        <config>
          <epBindings>
            <row epName="TestEndpoint1" epType="soap">
              <config>
                <epDetail>
                  <tab>
                    <tabType>transport</tabType>
                    <config>
                      <transport>
                        <config>
                          <useBasicAuthentication>true</useBasicAuthentication>
                          <httpURI>/Service.serviceagent/TestEndpoint1</httpURI>
                        </config>
                      </transport>
                    </config>
                  </tab>
                </epDetail>
              </config>
            </row>
          </epBindings>
        </config>
      </tab>
    </interfaceImpl>
  </config>
</serviceResource>
EOF
make_ear "service_agent_basic_auth" "$PROC_DIR"
rm -f "$PROC_DIR"/*.serviceagent

# ------------------------------------------------------------------ #
# Fixture: unsupported_activity.ear — unknown activity type (BLOCKER)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>CustomThing</pd:name>
    <pd:type>com.tibco.plugin.unknownvendor.SomeWeirdActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "unsupported_activity" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: checkpoint_file.ear — checkpoint with no DB in TIBCO.xml (WARNING)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Chk</pd:name>
    <pd:type>com.tibco.pe.core.CheckpointActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "checkpoint_file" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: checkpoint_db.ear — checkpoint with DB-backed storage (WARNING, milder)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Chk</pd:name>
    <pd:type>com.tibco.pe.core.CheckpointActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
TIBCO_XML='<?xml version="1.0" encoding="UTF-8"?>
<repository>
  <globalvariables>
    <globalvariable>
      <name>BWDatabaseURL</name>
      <value>jdbc:oracle:thin:@localhost:1521:orcl</value>
      <description>Checkpoint Data Repository</description>
    </globalvariable>
  </globalvariables>
</repository>'
make_ear "checkpoint_db" "$PROC_DIR" "" "$TIBCO_XML"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: wait_notify.ear — Wait/Notify pattern (WARNING)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Wait</pd:name>
    <pd:type>com.tibco.plugin.waitnotify.WaitActivity</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>Notify</pd:name>
    <pd:type>com.tibco.plugin.waitnotify.NotifyActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "wait_notify" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: file_io.ear — file activities (NOTE)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>ReadFile</pd:name>
    <pd:type>com.tibco.plugin.file.FileReadActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "file_io" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: ft_group.ear — FT Group reference (NOTE)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Start</pd:name>
    <pd:type>com.tibco.pe.core.OnStartupEventSource</pd:type>
  </pd:activity>
  <!-- FTGroup configuration reference -->
  <pd:faultTolerantGroup>primary-ftgroup</pd:faultTolerantGroup>
</pd:ProcessDefinition>
EOF
make_ear "ft_group" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: rendezvous.ear — RV activity (NOTE)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>RVPub</pd:name>
    <pd:type>com.tibco.plugin.tibrv.RVPubActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "rendezvous" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixtures: known-unsupported plugins (BLOCKER with label)
# Activity type strings taken verbatim from PluginActivityMap (Go extractor).
# Note: JD Edwards, PeopleSoft, OSIsoft PI, Tuxedo, and EDI are adapter-SDK
# resources with no <pd:type> entries — not detectable via process scanning.
# ------------------------------------------------------------------ #

# Plugins: EJB, Mobile Integration, NetSuite, SmartMapper, ActiveSpaces
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>EJBHome</pd:name>
    <pd:type>com.tibco.plugin.ejb.EJBHomeActivity</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>MobilePush</pd:name>
    <pd:type>com.tibco.plugin.bwmi.SendPushNotificationActivity</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>NSAdd</pd:name>
    <pd:type>com.tibco.plugin.netsuite.activities.crud.NetSuiteAddRecordActivity</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>SMLookup</pd:name>
    <pd:type>com.tibco.solution.xref.plugin.activity.LookupActivity</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>ASPut</pd:name>
    <pd:type>com.tibco.plugin.firefly.activities.PutActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "unsupported_plugins" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# Mainframe: CICS, HL7  (EDI has no PluginActivityMap entry)
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>CICSCall</pd:name>
    <pd:type>com.tibco.plugin.cicspi.CicsPiActivity</pd:type>
  </pd:activity>
  <pd:activity>
    <pd:name>HL7Translate</pd:name>
    <pd:type>com.tibco.plugin.hl7.bwactivities.HL7TranslateActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "unsupported_mainframe" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: adapter_supported.ear — ADB adapter (componentSoftwareName=adb)
# Should produce 0 blockers.
# ------------------------------------------------------------------ #
AAR_DIR="$(mktemp -d)"
make_aar "adb" "$AAR_DIR/ActiveDatabaseAdapterConfiguration.aar"
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>DBCall</pd:name>
    <pd:type>com.tibco.plugin.ae.AERPCRequestReplyActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "adapter_supported" "$PROC_DIR" "" "" "$AAR_DIR"
rm -f "$PROC_DIR"/*.process
rm -rf "$AAR_DIR"

# ------------------------------------------------------------------ #
# Fixture: unsupported_adapters.ear — four unsupported adapters via AAR
# componentSoftwareName values: jdexe, psft8, pi, tuxedo
# ------------------------------------------------------------------ #
AAR_DIR="$(mktemp -d)"
make_aar "jdexe"  "$AAR_DIR/JDEdwardsAdapterConfiguration.aar"
make_aar "psft8"  "$AAR_DIR/PeopleSoftAdapterConfiguration.aar"
make_aar "pi"     "$AAR_DIR/OSIsoftPIAdapterConfiguration.aar"
make_aar "tuxedo" "$AAR_DIR/TuxedoAdapterConfiguration.aar"
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>AdapterCall</pd:name>
    <pd:type>com.tibco.plugin.ae.AERPCRequestReplyActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "unsupported_adapters" "$PROC_DIR" "" "" "$AAR_DIR"
rm -f "$PROC_DIR"/*.process
rm -rf "$AAR_DIR"

# ------------------------------------------------------------------ #
# Fixture: engine_command_lifecycle.ear — EngineCommand with flagged
# lifecycle operations (WARNING): Shutdown + SuspendProcessStarter
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>ShutdownEngine</pd:name>
    <pd:type>com.tibco.pe.core.EngineCommandActivity</pd:type>
    <pd:resourceType>ae.activities.enginecommand</pd:resourceType>
    <config>
      <command>Shutdown</command>
    </config>
  </pd:activity>
  <pd:activity>
    <pd:name>SuspendStarter</pd:name>
    <pd:type>com.tibco.pe.core.EngineCommandActivity</pd:type>
    <pd:resourceType>ae.activities.enginecommand</pd:resourceType>
    <config>
      <command>SuspendProcessStarter</command>
    </config>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "engine_command_lifecycle" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: engine_command_safe.ear — EngineCommand with a non-flagged
# command (GetActivityStats) — should produce no warning
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>GetStats</pd:name>
    <pd:type>com.tibco.pe.core.EngineCommandActivity</pd:type>
    <pd:resourceType>ae.activities.enginecommand</pd:resourceType>
    <config>
      <command>GetActivityStats</command>
    </config>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "engine_command_safe" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: external_command.ear — External Command Activity (WARNING)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>RunScript</pd:name>
    <pd:type>com.tibco.plugin.cmdexec.CmdExecActivity</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "external_command" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

# ------------------------------------------------------------------ #
# Fixture: hardcoded_sharedhttp.ear — sharedhttp with hardcoded host+port
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Start</pd:name>
    <pd:type>com.tibco.pe.core.OnStartupEventSource</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
cat > "$PROC_DIR/MyHttpConn.sharedhttp" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<httpSharedChannel>
  <config>
    <Host>192.168.1.100</Host>
    <Port>8080</Port>
  </config>
</httpSharedChannel>
EOF
make_ear "hardcoded_sharedhttp" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process "$PROC_DIR"/*.sharedhttp

# ------------------------------------------------------------------ #
# Fixture: safe_sharedhttp.ear — sharedhttp using GV references (no warning)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Start</pd:name>
    <pd:type>com.tibco.pe.core.OnStartupEventSource</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
cat > "$PROC_DIR/MyHttpConn.sharedhttp" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<httpSharedChannel>
  <config>
    <Host>%%myapp.http.host%%</Host>
    <Port>%%myapp.http.port%%</Port>
  </config>
</httpSharedChannel>
EOF
make_ear "safe_sharedhttp" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process "$PROC_DIR"/*.sharedhttp

# ------------------------------------------------------------------ #
# Fixture: hardcoded_sharedjdbc.ear — sharedjdbc with hardcoded URL/user/pass
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Start</pd:name>
    <pd:type>com.tibco.pe.core.OnStartupEventSource</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
cat > "$PROC_DIR/MyJDBC.sharedjdbc" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<jdbcSharedResource>
  <config>
    <location>jdbc:oracle:thin:@mydb.corp.com:1521:ORCL</location>
    <user>app_user</user>
    <password>s3cr3t</password>
  </config>
</jdbcSharedResource>
EOF
make_ear "hardcoded_sharedjdbc" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process "$PROC_DIR"/*.sharedjdbc

# ------------------------------------------------------------------ #
# Fixture: hardcoded_sharedjms.ear — jms with hardcoded URL/user/pass
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:activity>
    <pd:name>Start</pd:name>
    <pd:type>com.tibco.pe.core.OnStartupEventSource</pd:type>
  </pd:activity>
</pd:ProcessDefinition>
EOF
cat > "$PROC_DIR/MyJMS.jms" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<JMSConnection>
  <NamingEnvironment>
    <ProviderURL>tibjmsnaming://ems.corp.com:7222</ProviderURL>
  </NamingEnvironment>
  <ConnectionAttributes>
    <username>jms_user</username>
    <password>jms_pass</password>
  </ConnectionAttributes>
</JMSConnection>
EOF
make_ear "hardcoded_sharedjms" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process "$PROC_DIR"/*.jms

# ------------------------------------------------------------------ #
# Fixture: render_xml_pretty.ear — render-xml with pretty-print (QUALITY)
# ------------------------------------------------------------------ #
cat > "$PROC_DIR/main.process" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<pd:ProcessDefinition xmlns:pd="http://xmlns.tibco.com/bw/process/2003">
  <pd:name>Main</pd:name>
  <pd:description>A process that uses render-xml pretty-print</pd:description>
  <pd:activity>
    <pd:name>Mapper</pd:name>
    <pd:type>com.tibco.plugin.mapper.MapperActivity</pd:type>
    <config>
      <element>tib:render-xml($input/root, "UTF-8", true()</element>
    </config>
  </pd:activity>
</pd:ProcessDefinition>
EOF
make_ear "render_xml_pretty" "$PROC_DIR"
rm -f "$PROC_DIR"/*.process

echo "All fixtures created in $FIXTURES_DIR"
