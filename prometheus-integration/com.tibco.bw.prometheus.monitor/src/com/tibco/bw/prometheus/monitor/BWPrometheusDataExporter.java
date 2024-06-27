package com.tibco.bw.prometheus.monitor;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibco.bw.sharedresource.http.runtime.montr.dependencies.BWMicrometerRegistryProvider;

import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;


public class BWPrometheusDataExporter {
	private final static Logger logger = LoggerFactory.getLogger(BWPrometheusDataExporter.class);
	public static boolean isHTTPMetricsEnabled;	
	public static boolean isSystemMetricsEnabled;
	public static PrometheusMeterRegistry httpRegistry = null; 
	public static int port = 9095;
	private static Server server;

	
	public static void exportMetrics() throws Exception{
		if(System.getenv("BW_PROMETHEUS_PORT") != null) {
			port = Integer.parseInt(System.getenv("BW_PROMETHEUS_PORT"));
		}
		
		server = new Server(port);
	    ServletContextHandler context = new ServletContextHandler();
	    context.setContextPath("/");
	    server.setHandler(context);
	    initMetrics();
		
		CollectorRegistry cr = CollectorRegistry.defaultRegistry;
		cr.register(new PrometheusCollector());
		DefaultExports.initialize();
		
		// enable http endpoint metrics
		if(isHTTPMetricsEnabled) {
			httpRegistry = BWMicrometerRegistryProvider.getInstance().prometheusRegistry;
		}

		PrometheusMeterRegistry systemRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);	    
		initSystemMetrics(systemRegistry);
		context.addServlet(new ServletHolder(new CollectorServlet(cr,systemRegistry,httpRegistry)), "/metrics");	
		server.start();	
	}


	private static void initMetrics() {
		if(System.getenv("BW_PROMETHEUS_HTTP_METRICS") != null) {
			isHTTPMetricsEnabled=true;
		}
	}
	
	private static void initSystemMetrics(PrometheusMeterRegistry systemRegistry) {
		ProcessorMetrics metrics = new ProcessorMetrics();
	    metrics.bindTo(systemRegistry);
	}
	
	static void shutdownServer() {
		try {
			server.stop();
		} catch (Exception e) {
			logger.error("Failed to stop the Jetty server in Prometheus plugin due to the exception : " + e.getMessage());
		}
		server.destroy();
	}
}
