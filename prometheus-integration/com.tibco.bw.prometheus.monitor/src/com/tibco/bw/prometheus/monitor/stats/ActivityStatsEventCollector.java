/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor.stats;

import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.BW_APPNODE_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.BW_APPSPACE_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.BW_DOMAIN_PROPERTY;
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
import com.tibco.bw.prometheus.monitor.model.ActivityStats;
import com.tibco.bw.prometheus.monitor.util.Utils;
import com.tibco.bw.runtime.event.ActivityAuditEvent;
import com.tibco.bw.runtime.event.ActivityInputDataException;
import com.tibco.bw.runtime.event.ActivityOutputDataException;
import com.tibco.bw.runtime.event.State;
import com.tibco.bw.thor.management.common.ContianerInfo;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class ActivityStatsEventCollector implements EventHandler {

	private final static Logger logger = LoggerFactory.getLogger(ActivityStatsEventCollector.class);
	private final Map<String, Map<String, Object>> statMaps = new HashMap<String, Map<String, Object>>();
	static Map<String, Map<String, Map<String, Double>>> durationStatsMap = new HashMap<String, Map<String, Map<String, Double>>>();

	private ContianerInfo deploymentInfo = ContianerInfo.get();
	private ConfigurationManager config = ConfigurationManager.getInstance();
	
    static final Counter activityStatsTotalCounter = Counter.build().name("activity_events_count").help("BWCE All Activity Events count by Process, Activity State").labelNames("ProcessName", "ActivityName", "StateName").register();
    static final Gauge activityDurationCounter = Gauge.build().name("activity_duration_count").help("BWCE Activity DurationTime by Process and Activity").labelNames("ProcessName", "ActivityName").register();
    static final Gauge activityEvaltimeCounter = Gauge.build().name("activity_evaltime_count").help("BWCE Activity EvalTime  by Process and Activity ").labelNames("ProcessName", "ActivityName").register();

	
	@Override
	public void handleEvent(final Event event) {

		if (config.isPrometheusEnabled() && config.isActivityEnabled()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Event Received. Event = {" + event.toString() + "}");
			}
			
		

			if (event.getProperty(EVENT_DATA_PROPERTY) instanceof ActivityAuditEvent) {
				ActivityAuditEvent activityEvent = (ActivityAuditEvent) event.getProperty(EVENT_DATA_PROPERTY);

				
				String pId = activityEvent.getProcessInstanceId();
				String activityName = activityEvent.getActivityName();
				String activityExecutionId = activityEvent.getActivityExecutionId();
				String key = activityName + pId + activityExecutionId;

				updateActivityCounter(activityEvent);
				
				if (State.STARTED == activityEvent.getActivityState()) {
					Map<String, Object> activityStatMap = new HashMap<String, Object>();
					for (String proprtyName : event.getPropertyNames()) {
						activityStatMap.put(proprtyName, event.getProperty(proprtyName));
					}
					if (null == statMaps.get(key)) {
						activityStatMap.put(START_TIME_PROPERTY, (activityEvent.getActivityStartTime()));
						try {
							activityStatMap.put("activityInput", activityEvent.getSerializedInputData());
						} catch (ActivityInputDataException e) {
							logger.error("Error while accessing activity input data " + e.getMessage());
						}
						statMaps.put(key, activityStatMap);
					} else {
						activityStatMap = statMaps.remove(key);
						activityStatMap.put(START_TIME_PROPERTY, (activityEvent.getActivityStartTime()));
						addStatsToMetrics(activityStatMap, activityEvent);
					}
				} else if (State.COMPLETED == activityEvent.getActivityState()
						|| State.FAULTED == activityEvent.getActivityState()
						|| State.CANCELLED == activityEvent.getActivityState()) {
					Map<String, Object> activityStatMap = new HashMap<String, Object>();
					if (null == statMaps.get(key)) {
						activityStatMap.put(END_TIME_PROPERTY, (activityEvent.getActivityEndTime()));
						activityStatMap.put(EVAL_TIME_PROPERTY, (activityEvent.getActivityEvalTime()));
						activityStatMap.put(STATUS_PROPERTY, activityEvent.getActivityState().name());
						try {
							activityStatMap.put("activityOutput", activityEvent.getSerializedOutputData());
						} catch (ActivityOutputDataException e) {
							logger.error("Error while accessing activity output data " + e.getMessage());
						}
						statMaps.put(key, activityStatMap);
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("Statistics collected for Activity {" + activityName
									+ "} in Process Instance {" + pId + "}");
						}
						activityStatMap = statMaps.remove(key);
						activityStatMap.put(STATUS_PROPERTY, activityEvent.getActivityState().name());
						addStatsToMetrics(activityStatMap, activityEvent);
					}
				}

			}
		}
	}

	private void updateTotalAcitivtyDurationCounter(ActivityStats activityStats2) {		
		activityDurationCounter.labels(activityStats2.getProcessName(),activityStats2.getActivityName()).inc(activityStats2.getActivityDurationTime());
		activityEvaltimeCounter.labels(activityStats2.getProcessName(),activityStats2.getActivityName()).inc(activityStats2.getActivityEvalTime());
	}

	private void updateActivityCounter(ActivityAuditEvent activityEvent) {
		activityStatsTotalCounter.labels(activityEvent.getProcessName(),activityEvent.getActivityName(),activityEvent.getActivityState().name()).inc();
	}





	private void addStatsToMetrics(final Map<String, Object> pStatMap, final ActivityAuditEvent event) {
		ActivityStats activityStats = new ActivityStats();
		activityStats.setApplicationName(event.getApplicationName());
		activityStats.setApplicationVersion(event.getApplicationVersion());
		activityStats.setModuleName(event.getModuleName());
		activityStats.setModuleVersion(event.getModuleVersion());
		activityStats.setProcessName(event.getProcessName());
		activityStats.setActivityName(event.getActivityName());
		activityStats.setProcessInstanceId(event.getProcessInstanceId());
		activityStats.setActivityStartTime(Utils.convertTimeToString((Long) pStatMap.get(START_TIME_PROPERTY)));

		if (State.COMPLETED == event.getActivityState() || State.FAULTED == event.getActivityState()
				|| State.CANCELLED == event.getActivityState()) {
			activityStats.setActivityEndTime(Utils.convertTimeToString(event.getActivityEndTime()));
			activityStats
					.setActivityDurationTime((event.getActivityEndTime() - (Long) pStatMap.get(START_TIME_PROPERTY)));
			activityStats.setActivityEvalTime(event.getActivityEvalTime());
		} else if (State.STARTED == event.getActivityState()) {
			activityStats.setActivityEndTime(Utils.convertTimeToString((Long) pStatMap.get(END_TIME_PROPERTY)));
			activityStats
					.setActivityDurationTime(((Long) pStatMap.get(END_TIME_PROPERTY) - event.getActivityStartTime()));
			activityStats.setActivityEvalTime((Long) pStatMap.get(EVAL_TIME_PROPERTY));
		}
		activityStats.setActivityState((String) pStatMap.get(STATUS_PROPERTY));
		if (deploymentInfo.isBWCE()) {
			String containerName = deploymentInfo.getContainerName();
			if (containerName != null) {
				activityStats.setAppnodeName(containerName);
			} else {
				activityStats.setAppnodeName(System.getProperty(BW_APPNODE_PROPERTY));
			}
		} else {
			activityStats.setAppnodeName(System.getProperty(BW_APPNODE_PROPERTY));
		}
		activityStats.setAppspaceName(System.getProperty(BW_APPSPACE_PROPERTY));
		activityStats.setDomainName(System.getProperty(BW_DOMAIN_PROPERTY));
		activityStats.setActivityExecutionId(event.getActivityExecutionId());

		// Add Activity in Metrics
		updateTotalAcitivtyDurationCounter(activityStats);
	}

}
