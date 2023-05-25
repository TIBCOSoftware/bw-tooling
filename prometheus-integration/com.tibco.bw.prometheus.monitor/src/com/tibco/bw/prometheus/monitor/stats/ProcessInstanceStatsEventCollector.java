/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor.stats;

import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.END_TIME_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.EVAL_TIME_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.EVENT_DATA_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.START_TIME_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.STATUS_PROPERTY;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibco.bw.prometheus.monitor.ConfigurationManager;
import com.tibco.bw.prometheus.monitor.model.ProcessStats;
import com.tibco.bw.prometheus.monitor.util.Utils;
import com.tibco.bw.runtime.event.ProcessAuditEvent;
import com.tibco.bw.runtime.event.State;
import com.tibco.bw.thor.management.common.ContianerInfo;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class ProcessInstanceStatsEventCollector implements EventHandler {

	private final static Logger logger = LoggerFactory.getLogger(ProcessInstanceStatsEventCollector.class);
	private static final String BW_APPNODE_PROPERTY = "bw.appnode";
	private static final String BW_DOMAIN_PROPERTY = "bw.domain";
	private static final String BW_APPSPACE_PROPERTY = "bw.appspace";
	private ConfigurationManager config = ConfigurationManager.getInstance();

	private final Map<String, Map<String, Object>> statMaps = new HashMap<String, Map<String, Object>>();
	private ContianerInfo deploymentInfo = ContianerInfo.get();

    static final Counter processStatsTotalCounter = Counter.build().name("process_events_count").help("BWCE Process Events count by Process").labelNames("ProcessName", "StateName").register();
    static final Gauge processDurationCounter = Gauge.build().name("process_duration_milliseconds_total").help("BWCE Process Events duration by Process in milliseconds").labelNames("ProcessName").register();

	@Override
	public void handleEvent(final Event event) {

		if (config.isPrometheusEnabled()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Event Received. Event = {" + event.toString() + "}");
			}
			if (event.getProperty(EVENT_DATA_PROPERTY) instanceof ProcessAuditEvent) {
				ProcessAuditEvent processEvent = (ProcessAuditEvent) event.getProperty(EVENT_DATA_PROPERTY);
				String pId = processEvent.getProcessInstanceId();
				if (processEvent.getProcessInstanceState() != State.INFORMATIVE && processEvent.getProcessInstanceState() != State.SCHEDULED) {
					updateProcessCounter(processEvent);
	
					if (State.STARTED == processEvent.getProcessInstanceState()) {
	
						Map<String, Object> processStatMap = new HashMap<String, Object>();
						for (String propName : event.getPropertyNames()) {
							processStatMap.put(propName, event.getProperty(propName));
						}
						if (null == statMaps.get(pId)) {
							processStatMap.put(START_TIME_PROPERTY, processEvent.getProcessInstanceStartTime());
							statMaps.put(pId, processStatMap);
						} else {
							processStatMap = statMaps.remove(pId);
							processStatMap.put(START_TIME_PROPERTY, processEvent.getProcessInstanceStartTime());
							addStatsToMetrics(processStatMap, processEvent);
						}
					} else if (State.COMPLETED == processEvent.getProcessInstanceState()
							|| State.FAULTED == processEvent.getProcessInstanceState()
							|| State.CANCELLED == processEvent.getProcessInstanceState()) {
						Map<String, Object> processStatMap = new HashMap<String, Object>();
						processStatMap.put(STATUS_PROPERTY, processEvent.getProcessInstanceState().name());
						if (null == statMaps.get(pId)) {
							processStatMap.put(EVAL_TIME_PROPERTY, "null");
							processStatMap.put(END_TIME_PROPERTY, processEvent.getProcessInstanceEndTime());
							statMaps.put(pId, processStatMap);
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Statistics collected for process instance {" + pId + "}");
							}
							processStatMap = statMaps.remove(pId);
							addStatsToMetrics(processStatMap, processEvent);
						}
					}
				}
			}
		}
	}


	private void updateProcessCounter(ProcessAuditEvent audit) {
		processStatsTotalCounter.labels(audit.getProcessName(),audit.getProcessInstanceState().name()).inc();
		if (logger.isDebugEnabled()) {
			logger.debug(audit.getProcessName() + ":"+audit.getProcessInstanceState().name()+":"+processStatsTotalCounter.labels(audit.getProcessName(),audit.getProcessInstanceState().name()).get());
		}
	}

	private String getNonNullValue(final String value) {
		return value != null ? value : "-";
	}

	private void addStatsToMetrics(final Map<String, Object> pStatMap, final ProcessAuditEvent event) {
		try {
			ProcessStats pis = new ProcessStats();
			pis.setApplicationName(event.getApplicationName());
			pis.setApplicationVersion(event.getApplicationVersion());
			pis.setModuleName(event.getModuleName());
			pis.setModuleVersion(event.getModuleVersion());
			pis.setComponentProcessName(getNonNullValue(event.getComponentProcessName()));
			pis.setJobId(event.getJobId());
			pis.setProcessName(event.getProcessName());
			if (event.getParentProcessName() == null && event.getParentProcessInstanceId() == null) {
				pis.setParentProcessName("null");
				pis.setParentProcessInstanceId("null");
			} else {
				pis.setParentProcessName(event.getParentProcessName());
				pis.setParentProcessInstanceId(event.getParentProcessInstanceId());
			}
			pis.setProcessInstanceId(event.getProcessInstanceId());
			pis.setProcessInstanceState(event.getProcessInstanceState().name());
			if (State.STARTED == event.getProcessInstanceState()) {
				pis.setProcessInstanceStartTime(Utils.convertTimeToString(event.getProcessInstanceStartTime()));
				pis.setProcessInstanceEndTime(Utils.convertTimeToString((Long) pStatMap.get(END_TIME_PROPERTY)));
				if (event.getProcessInstanceEndTime() != null) {
					pis.setProcessInstanceDurationTime(
							(Long) pStatMap.get(END_TIME_PROPERTY) - event.getProcessInstanceEndTime());
				}
			} else if (State.COMPLETED == event.getProcessInstanceState()
					|| State.FAULTED == event.getProcessInstanceState()
					|| State.CANCELLED == event.getProcessInstanceState()) {
				pis.setProcessInstanceStartTime(Utils.convertTimeToString((Long) pStatMap.get(START_TIME_PROPERTY)));
				pis.setProcessInstanceEndTime(Utils.convertTimeToString(event.getProcessInstanceEndTime()));
				pis.setProcessInstanceDurationTime(
						(event.getProcessInstanceEndTime() - (Long) pStatMap.get(START_TIME_PROPERTY)));
				updateProcessDurationCounter(pis);
			}

			if (deploymentInfo.isBWCE()) {
				String containerName = deploymentInfo.getContainerName();
				if (containerName != null) {
					pis.setAppnodeName(containerName);
				} else {
					pis.setAppnodeName(System.getProperty(BW_APPNODE_PROPERTY));
				}
			} else {
				pis.setAppnodeName(System.getProperty(BW_APPNODE_PROPERTY));
			}
			pis.setAppspaceName(System.getProperty(BW_APPSPACE_PROPERTY));
			pis.setDomainName(System.getProperty(BW_DOMAIN_PROPERTY));
			pis.setActivityExecutionId("null");

		} catch (Throwable ex) {
			logger.warn("Exception error handling Process Metrics", ex);

		}
	}

	private void updateProcessDurationCounter(ProcessStats pis) {
		processDurationCounter.labels(pis.getProcessName()).inc(pis.getProcessInstanceDurationTime());
	}



}
