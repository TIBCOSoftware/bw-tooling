package com.tibco.bw.prometheus.monitor;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static BundleContext context;
	private final static Logger logger = LoggerFactory.getLogger(Activator.class);
	private static final String BW_PROMETHEUS_ENABLE = "BW_PROMETHEUS_ENABLE";

	public static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		
		if (System.getenv(BW_PROMETHEUS_ENABLE).equalsIgnoreCase("true")) {
			logger.info("Starting the Prometheus Monitoring Bundle");
			
			//Register handlers
			String[] topics = new String[] {"com/tibco/audit/*"};
			EventHandler eventHandler = new BWEventHandler();
			Hashtable<String, String[]> config = new Hashtable<>();
			config.put(EventConstants.EVENT_TOPIC, topics);
			context.registerService(EventHandler.class.getName(), eventHandler, config);
			logger.info("Prometheus : Event monitoring service has been registered.");
			
			//Run Prometheus
			PrometheusCollector.run();
		} else {
			logger.info("Prometheus Monitoring Disabled");
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
