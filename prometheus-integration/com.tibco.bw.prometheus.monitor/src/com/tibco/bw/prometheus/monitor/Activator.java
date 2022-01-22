/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			
			Dictionary<String, String> properties = new Hashtable<>();
			BWEventHandler handler = new BWEventHandler();
			properties.put("event.topics" , "com/tibco/audit/*");
			properties.put("event.type", "bw");
			context.registerService(BWEventHandler.class.getName(), handler, properties);
			
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


