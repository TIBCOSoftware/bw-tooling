/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.xml.namespace.QName;

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibco.bw.prometheus.monitor.stats.ActivityStatsEventCollector;
import com.tibco.bw.prometheus.monitor.stats.ProcessInstanceStatsEventCollector;
import com.tibco.bw.prometheus.monitor.util.Utils;
import com.tibco.bw.sharedresource.http.inbound.api.HttpConnector;
import com.tibco.bw.sharedresource.http.inbound.api.HttpServletApplicationModel;
import com.tibco.bw.sharedresource.runtime.ResourceReference;
import com.tibco.neo.exception.BaseException;

public class PrometheusCollector extends Collector {
	private static Logger logger = LoggerFactory.getLogger(PrometheusCollector.class);
	public static HTTPServer server;
	private final static InetSocketAddress DEFAULT_PROMETHEUS_MONITOR_PORT = new InetSocketAddress("0.0.0.0",9095);
	
	private static final QName HTTPCONNECTOR_TYPE = new QName("http://xsd.tns.tibco.com/bw/models/sharedresource/httpconnector","HttpConnectorConfiguration");
	private final static CountDownLatch proxyInitLatch = new CountDownLatch(1);
	private static final String APPLICATION_NAME = "application_name";
	
	public static void run() {
		logger.info("Prometheus Collector started");
		try {
			CollectorRegistry cr = CollectorRegistry.defaultRegistry;
			cr.register(new PrometheusCollector());
			server = new HTTPServer(DEFAULT_PROMETHEUS_MONITOR_PORT, cr);	
			DefaultExports.initialize();
			
			if (Utils.isPCF()) {
				try {
					boolean holdThread = registerProxyServlet();
					if (holdThread)
						proxyInitLatch.await();
				} catch (Exception e) {
					logger.error("Failed to register Proxy Servlet for PCF. Prometheus Monitoring will not work",e);
				}
			}
			
		} catch (Exception e) {
			logger.error("Exception while creating the Prometheus collector server " + e.toString(), e);
		}
	}

	@Override
	public List<MetricFamilySamples> collect() {
		List<MetricFamilySamples> mfs = new ArrayList<Collector.MetricFamilySamples>();
		mfs.addAll(ActivityStatsEventCollector.getCollection());
		mfs.addAll(ProcessInstanceStatsEventCollector.getCollection());
		return mfs;
	}
	
	// Called only when running in PCF so that we can forward requests to the AppNode.
	// One Port limitation of PCF needs this kind of a Proxy
	private static boolean registerProxyServlet() throws BaseException {
		ServiceTracker<ResourceReference, ResourceReference> tracker = new ServiceTracker<>(
				Activator.getContext(), ResourceReference.class, null);
		tracker.open();
		if (tracker.getServiceReferences() != null) {
			for (ServiceReference<ResourceReference> serviceRef : tracker.getServiceReferences()) {
				if (serviceRef != null && serviceRef.getProperty(".type").equals(HTTPCONNECTOR_TYPE.toString())) {
					ResourceReference reference = tracker.getService(serviceRef);
					if (reference != null) {
						HttpConnector connector = (HttpConnector) reference.getResource();
						if (connector != null) {
							HashMap<String, String> initParams = new HashMap<>();
							initParams.put(ProxyServlet.P_TARGET_URI, "http://localhost:9095/metrics");
							HttpServletApplicationModel model = new HttpServletApplicationModel("/metrics", "/*", APPLICATION_NAME, initParams, new ProxyServlet(proxyInitLatch));
							connector.deployServletApplication(model);
							logger.info("Prometheus : Proxy server created in PCF");
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
