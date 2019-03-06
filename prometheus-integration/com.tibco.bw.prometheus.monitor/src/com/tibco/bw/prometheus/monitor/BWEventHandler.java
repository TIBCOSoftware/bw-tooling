/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibco.bw.prometheus.monitor.stats.ActivityStatsEventCollector;
import com.tibco.bw.prometheus.monitor.stats.ProcessInstanceStatsEventCollector;
import com.tibco.bw.runtime.AuditEvent;
import com.tibco.bw.runtime.event.ActivityAuditEvent;
import com.tibco.bw.runtime.event.ProcessAuditEvent;

public class BWEventHandler implements EventHandler {
	
	private final Logger logger = LoggerFactory.getLogger(BWEventHandler.class);
	private ActivityStatsEventCollector activityStatEventCollector = new ActivityStatsEventCollector();
	private ProcessInstanceStatsEventCollector processInstanceStatsEventCollector = new ProcessInstanceStatsEventCollector();
	
	@Override
	public void handleEvent(final Event event) {
		AuditEvent auditEvent = (AuditEvent) event.getProperty("eventData");
		if (auditEvent instanceof ProcessAuditEvent) {
			logger.info("Prometheus : BW Process Event Received");
			processInstanceStatsEventCollector.handleEvent(event);
		} else if (auditEvent instanceof ActivityAuditEvent) {
			logger.info("Prometheus : BW Audit Event Received");
			activityStatEventCollector.handleEvent(event);
		} 
	}
}
