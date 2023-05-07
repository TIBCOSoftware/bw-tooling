/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibco.bw.frwk.api.StatCollectionConstant;
import com.tibco.bw.prometheus.monitor.stats.ActivityStatsEventCollector;
import com.tibco.bw.prometheus.monitor.stats.ProcessInstanceStatsEventCollector;
import com.tibco.bw.runtime.event.AuditEventConstants;

public class Activator implements BundleActivator {

	private static BundleContext context;
	private final static Logger logger = LoggerFactory.getLogger(Activator.class);


	public static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;


		ConfigurationManager config = ConfigurationManager.getInstance(); 


		if (config.isPrometheusEnabled()) {
			logger.info("Starting the Prometheus Monitoring Bundle");

			Dictionary<String, Object> properties = new Hashtable<>();
			String[] processStatsServiceNames = new String[] {EventHandler.class.getName(), ProcessInstanceStatsEventCollector.class.getName()};
			String[] activityStatsServiceNames = new String[] {EventHandler.class.getName(), ActivityStatsEventCollector.class.getName()};
			String[] eventTopics = new String[] {AuditEventConstants.PROCESS_INSTANCE_AUDIT_EVENT_TYPE, AuditEventConstants.ACTIVITY_AUDIT_EVENT_TYPE};
			properties.put(EventConstants.EVENT_TOPIC, eventTopics);
			properties.put(StatCollectionConstant.BW_EVENT_TYPE_PROPERTY, StatCollectionConstant.BW_EVENT_TYPE_PROPERTY_VALUE);
			context.registerService(processStatsServiceNames, new ProcessInstanceStatsEventCollector(), properties);
			context.registerService(activityStatsServiceNames, new ActivityStatsEventCollector(), properties);

			PrometheusCollector.run();
		} 
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		logger.info("Prometheus Monitoring Bundle Stopped.");
		Activator.context = null;
	}

}


