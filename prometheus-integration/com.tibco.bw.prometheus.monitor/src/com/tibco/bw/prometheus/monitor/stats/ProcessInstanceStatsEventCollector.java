/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor.stats;

import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.END_TIME_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.EVAL_TIME_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.EVENT_DATA_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.START_TIME_PROPERTY;
import static com.tibco.bw.prometheus.monitor.util.StatCollectionConstant.STATUS_PROPERTY;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Collector.Type;
import io.prometheus.client.CounterMetricFamily;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.tibco.bw.prometheus.monitor.model.ProcessStats;
import com.tibco.bw.prometheus.monitor.util.Utils;
import com.tibco.bw.runtime.event.ProcessAuditEvent;
import com.tibco.bw.runtime.event.State;
import com.tibco.bw.thor.management.common.ContianerInfo;

public class ProcessInstanceStatsEventCollector implements EventHandler {

	private final static Logger logger = LoggerFactory.getLogger(ProcessInstanceStatsEventCollector.class);
	private static final String BW_APPNODE_PROPERTY = "bw.appnode";
	private static final String BW_DOMAIN_PROPERTY = "bw.domain";
	private static final String BW_APPSPACE_PROPERTY = "bw.appspace";
	private ConfigurationManager config = ConfigurationManager.getInstance();

	private final ConcurrentMap<String, Map<String, Object>> statMaps = new ConcurrentHashMap<String, Map<String, Object>>();
	private ContianerInfo deploymentInfo = ContianerInfo.get();

	private static List<Sample> processSampleList = new ArrayList<Collector.MetricFamilySamples.Sample>();
	private static List<Sample> processCounterSampleList = new ArrayList<Collector.MetricFamilySamples.Sample>();
	static Map<String,Integer> processStateCounterMap = new HashMap<String,Integer>();
	static {
		processStateCounterMap.put(State.STARTED.name(), 0);
		processStateCounterMap.put(State.COMPLETED.name(), 0);
		processStateCounterMap.put(State.FAULTED.name(), 0);
		processStateCounterMap.put(State.CANCELLED.name(), 0);
	}
	
	static Map<String, HashMap<String, Integer>> processCounterMap = new HashMap<String,HashMap<String,Integer>>();
	
	
	@Override
	public void handleEvent(final Event event) {
		if (logger.isDebugEnabled()) {
			logger.debug("Event Received. Event = {"+ event.toString() + "}");
		}
		
		ProcessAuditEvent processEvent = (ProcessAuditEvent) event.getProperty(EVENT_DATA_PROPERTY);
		String pId = processEvent.getProcessInstanceId();
		if (processEvent.getProcessInstanceState() != State.INFORMATIVE) {
			if(config.isProcessDetailedEnabled()){
				processCounterSampleList.add(new Sample("process_state_count",ProcessStats.getProcessCounterKeyList(), getProcessStateCounterList(processEvent), 1));
			}
		}
		updateProcessEventCounter(processEvent.getProcessInstanceState().name());
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
				processStatMap.put(START_TIME_PROPERTY,processEvent.getProcessInstanceStartTime());
				addStatsToMetrics(processStatMap, processEvent);
			}
		} else if (State.COMPLETED == processEvent.getProcessInstanceState()
				|| State.FAULTED == processEvent.getProcessInstanceState()
				|| State.CANCELLED == processEvent.getProcessInstanceState()) {
			Map<String, Object> processStatMap = new HashMap<String, Object>();
			processStatMap.put(STATUS_PROPERTY, processEvent.getProcessInstanceState().name());
			if (null == statMaps.get(pId)) {
				processStatMap.put(EVAL_TIME_PROPERTY, "null");
				processStatMap.put(END_TIME_PROPERTY,processEvent.getProcessInstanceEndTime());
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

	private List<String> getProcessStateCounterList(
			ProcessAuditEvent event) {
		List<String> stateList = new ArrayList<>();
		stateList.add(event.getApplicationName());
		stateList.add(event.getProcessName());
		stateList.add(event.getProcessInstanceId());
		if (event.getProcessInstanceStartTime() != null)
			stateList.add(Utils.convertTimeToString(event.getProcessInstanceStartTime()));
		else
			stateList.add(Utils.convertTimeToString(event.getProcessInstanceEndTime()));
		stateList.add(event.getProcessInstanceState().name());
		return stateList;
	}

	private void updateProcessEventCounter(String name) {
		if(processStateCounterMap.containsKey(name)){
			processStateCounterMap.put(name, processStateCounterMap.get(name) + 1);
		}else{
			processStateCounterMap.put(name, 1);
		}
	}
	
	
	private void updateProcessCounter(ProcessAuditEvent audit) {
		HashMap<String, Integer> map = processCounterMap.get(audit.getProcessName());
		if(map == null){
			map = new HashMap<String,Integer>();
		}
		
		Integer value = map.get(audit.getProcessInstanceState().name());
		if(value == null){
			value = 0;

		}
		map.put(audit.getProcessInstanceState().name(), value + 1);
		processCounterMap.put(audit.getProcessName(), map);
		
	}
	
	private String getNonNullValue(final String value) {
		return value != null ? value : "-";
	}

	private void addStatsToMetrics(final Map<String, Object> pStatMap,
			final ProcessAuditEvent event) {
		try{
		ProcessStats pis = new ProcessStats();
		pis.setApplicationName(event.getApplicationName());
		pis.setApplicationVersion(event.getApplicationVersion());
		pis.setModuleName(event.getModuleName());
		pis.setModuleVersion(event.getModuleVersion());
		pis.setComponentProcessName(getNonNullValue(event
				.getComponentProcessName()));
		pis.setJobId(event.getJobId());
		pis.setProcessName(event.getProcessName());
		if(event.getParentProcessName() == null && event.getParentProcessInstanceId() == null){
			pis.setParentProcessName("null");
			pis.setParentProcessInstanceId("null");
		} else {
			pis.setParentProcessName(event.getParentProcessName());
			pis.setParentProcessInstanceId(event.getParentProcessInstanceId());
		}
		pis.setProcessInstanceId(event.getProcessInstanceId());
		if (State.STARTED == event.getProcessInstanceState()) {
			pis.setProcessInstanceStartTime(Utils.convertTimeToString(event.getProcessInstanceStartTime()));
			pis.setProcessInstanceEndTime(Utils.convertTimeToString((Long) pStatMap.get(END_TIME_PROPERTY)));
			pis.setProcessInstanceDurationTime((Long) pStatMap.get(END_TIME_PROPERTY) - event.getProcessInstanceEndTime());
		} else if (State.COMPLETED == event.getProcessInstanceState()
				|| State.FAULTED == event.getProcessInstanceState()
				|| State.CANCELLED == event.getProcessInstanceState()) {
			pis.setProcessInstanceStartTime(Utils.convertTimeToString((Long) pStatMap.get(START_TIME_PROPERTY)));
			pis.setProcessInstanceEndTime(Utils.convertTimeToString(event.getProcessInstanceEndTime()));
			pis.setProcessInstanceDurationTime((event.getProcessInstanceEndTime() - (Long) pStatMap.get(START_TIME_PROPERTY)));
		}
		pis.setProcessInstanceState(event.getProcessInstanceState().name());
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
		
		//setting null as subprocess details are not required.
		pis.setActivityExecutionId("null");
		
//		if (event.getParentProcessInstanceId() != null) {
//			pis.setActivityExecutionId(subProcessInfoMap.remove(event.getParentProcessInstanceId()
//					+ event.getProcessInstanceId()));
//		} else {
//			pis.setActivityExecutionId("null");
//		}
		
		// Add Metrics

		if(config.isProcessDetailedEnabled()){
			processSampleList.add(new Sample("process_stats_total", ProcessStats.getProcessStatsKeyList(), pis.getProcessStatsValueList(), 1));
			processCounterSampleList.add(new Sample("process_duration_count",ProcessStats.getProcessCounterKeyList(), pis.getProcessCounterValueList(), pis.getProcessInstanceDurationTime()));
		}

		}catch(Throwable ex){
			logger.warn("Exception error handling Process Metrics", ex);
			
		}
	}
	
	public static List<MetricFamilySamples> getCollection() {
		
		
	
		
		List<Sample> copyProcessSampleList = new ArrayList<>();
		copyProcessSampleList.addAll(processSampleList);
		
		MetricFamilySamples processMFS = new MetricFamilySamples("bwce_process_stats", 
				Type.GAUGE, "BWCE Process Statistics list", copyProcessSampleList);
				
		
		
		List<Sample> copyProcessCounterSampleList = new ArrayList<>();
		copyProcessCounterSampleList.addAll(processCounterSampleList);
		
		MetricFamilySamples processCountersMFS = new MetricFamilySamples("bwce_process_counter_list", 
				Type.GAUGE, "BWCE Process related Counters list", copyProcessCounterSampleList);
		
			
	
		CounterMetricFamily allProcessEventCounter = new CounterMetricFamily("all_process_events_count", "BWCE All Process Events count by State",Arrays.asList("StateName"));
		allProcessEventCounter.addMetric(Arrays.asList(State.CANCELLED.name()), processStateCounterMap.get(State.CANCELLED.name()));
		allProcessEventCounter.addMetric(Arrays.asList(State.COMPLETED.name()), processStateCounterMap.get(State.COMPLETED.name()));
		allProcessEventCounter.addMetric(Arrays.asList(State.STARTED.name()), processStateCounterMap.get(State.STARTED.name()));
		allProcessEventCounter.addMetric(Arrays.asList(State.FAULTED.name()), processStateCounterMap.get(State.FAULTED.name()));
		
		
		CounterMetricFamily processEventCounter = new CounterMetricFamily("process_events_count", "BWCE Process Events count by Process",Arrays.asList("ProcessName","StateName"));
		for(String entry : processCounterMap.keySet()){
			for(String state : processCounterMap.get(entry).keySet()){
				processEventCounter.addMetric(Arrays.asList(entry,state) , processCounterMap.get(entry).get(state));
			}
		}
		
		
		List<MetricFamilySamples> mfs = new ArrayList<>();
		mfs.add(processMFS);
		mfs.add(processCountersMFS);
		mfs.add(allProcessEventCounter);
		mfs.add(processEventCounter);
		return mfs;
	}
	
	public static void reset(){
		processCounterSampleList.clear();
		processSampleList.clear();
	}

}
