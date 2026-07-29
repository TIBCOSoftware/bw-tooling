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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class ActivityStatsEventCollector implements EventHandler {

	private final static Logger logger = LoggerFactory.getLogger(ActivityStatsEventCollector.class);
	private final ConcurrentMap<String, Map<String, Object>> statMaps = new ConcurrentHashMap<String, Map<String, Object>>();

	static Map<String, Map<String, Map<String, Double>>> durationStatsMap = new HashMap<String, Map<String, Map<String, Double>>>();

	private ContianerInfo deploymentInfo = ContianerInfo.get();
	private ConfigurationManager config = ConfigurationManager.getInstance();
	
    static final Counter activityStatsTotalCounter = Counter.build().name("activity_events_count").help("BWCE All Activity Events count by Process, Activity State").labelNames("ProcessName", "ActivityName", "StateName").register();
    static final Gauge activityDurationCounter = Gauge.build().name("activity_duration_count").help("BWCE Activity DurationTime by Process and Activity").labelNames("ProcessName", "ActivityName").register();
    static final Gauge activityEvaltimeCounter = Gauge.build().name("activity_evaltime_count").help("BWCE Activity EvalTime  by Process and Activity ").labelNames("ProcessName", "ActivityName").register();

	static Map<String,Integer> activityStateCounterMap = new HashMap<String,Integer>();
	static {
		activityStateCounterMap.put(State.STARTED.name(), 0);
		activityStateCounterMap.put(State.COMPLETED.name(), 0);
		activityStateCounterMap.put(State.FAULTED.name(), 0);
		activityStateCounterMap.put(State.CANCELLED.name(), 0);
	}

		
	private static List<Sample> activitySampleList = new ArrayList<Collector.MetricFamilySamples.Sample>();
	private static List<Sample> activityCounterSampleList = new ArrayList<Collector.MetricFamilySamples.Sample>();
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

		if(config.isActivityDetailedEnabled()){
			activityCounterSampleList.add(new Sample("activity_state_count",ActivityStats.getActivityCounterKeyList(), getActivityStateCounterList(activityEvent), 1));
		}
		updateTotalActivityEventCounter(activityEvent.getActivityState().name());
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
					logger.debug("Statistics collected for Activity {" + activityName + "} in Process Instance {" + pId + "}");
						}
						activityStatMap = statMaps.remove(key);
						activityStatMap.put(STATUS_PROPERTY, activityEvent.getActivityState().name());
						addStatsToMetrics(activityStatMap, activityEvent);
					}
				}

			}
		}
	}
	

	private List<String> getActivityStateCounterList(ActivityAuditEvent event) {
		List<String> stateList = new ArrayList<>();
		stateList.add(event.getApplicationName());
		stateList.add(event.getActivityName());
		if (event.getActivityStartTime() != null)
			stateList.add(Utils.convertTimeToString(event.getActivityStartTime()));
		else
			stateList.add(Utils.convertTimeToString(event.getActivityEndTime()));
		stateList.add(event.getActivityState().name());
		stateList.add(event.getProcessInstanceId());
		return stateList;
	}

	private void updateTotalAcitivtyDurationCounter(ActivityStats activityStats2) {		
		activityDurationCounter.labels(activityStats2.getProcessName(),activityStats2.getActivityName()).inc(activityStats2.getActivityDurationTime());
		activityEvaltimeCounter.labels(activityStats2.getProcessName(),activityStats2.getActivityName()).inc(activityStats2.getActivityEvalTime());
	}

	private void updateTotalActivityEventCounter(String name) {
		if(activityStateCounterMap.containsKey(name)){
			activityStateCounterMap.put(name, activityStateCounterMap.get(name) + 1);
		}
	}

	private void updateActivityCounter(ActivityAuditEvent activityEvent) {
		activityStatsTotalCounter.labels(activityEvent.getProcessName(),activityEvent.getActivityName(),activityEvent.getActivityState().name()).inc();
	}





	private void addStatsToMetrics(final Map<String, Object> pStatMap,
			final ActivityAuditEvent event) {
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
			activityStats.setActivityDurationTime((event.getActivityEndTime() - (Long) pStatMap.get(START_TIME_PROPERTY)));
			activityStats.setActivityEvalTime(event.getActivityEvalTime());
		} else if (State.STARTED == event.getActivityState()){
			activityStats.setActivityEndTime(Utils.convertTimeToString((Long) pStatMap.get(END_TIME_PROPERTY)));
			activityStats.setActivityDurationTime(((Long) pStatMap.get(END_TIME_PROPERTY) - event.getActivityStartTime()));
			activityStats.setActivityEvalTime((Long) pStatMap.get(EVAL_TIME_PROPERTY));
		}
		activityStats.setActivityState((String)pStatMap.get(STATUS_PROPERTY));
		if (deploymentInfo.isBWCE()) {
			String containerName = deploymentInfo.getContainerName();
			if (containerName != null) {
				activityStats.setAppnodeName(containerName);
			} else {
				activityStats.setAppnodeName(System.getProperty(BW_APPNODE_PROPERTY));			
			}	
		}
		else {
			activityStats.setAppnodeName(System.getProperty(BW_APPNODE_PROPERTY));
		}
		activityStats.setAppspaceName(System.getProperty(BW_APPSPACE_PROPERTY));
		activityStats.setDomainName(System.getProperty(BW_DOMAIN_PROPERTY));
		activityStats.setActivityExecutionId(event.getActivityExecutionId());

		// Add Activity in Metrics
		if(config.isActivityDetailedEnabled()){
			activitySampleList.add(new Sample("activity_stats_total", ActivityStats.getActivityStatsKeyList(), activityStats.getActivityStatsValueList(), 1));
			activityCounterSampleList.add(new Sample("activity_duration_count",ActivityStats.getActivityCounterKeyList(), activityStats.getActivityCounterValueList(), activityStats.getActivityDurationTime()));
			activityCounterSampleList.add(new Sample("activity_evaltime_count",ActivityStats.getActivityCounterKeyList(), activityStats.getActivityCounterValueList(), activityStats.getActivityEvalTime()));
		}
	
		updateTotalAcitivtyDurationCounter(activityStats);
	}

}
