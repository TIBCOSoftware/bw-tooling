package com.tibco.bw.prometheus.monitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

@SuppressWarnings("serial")
public class CollectorServlet extends HttpServlet{
	
	/**
	 * 
	 */
	public static CollectorRegistry registry;
	public static PrometheusMeterRegistry systemRegistry;
	public static PrometheusMeterRegistry httpRegistry;
	
	public CollectorServlet(CollectorRegistry cr, PrometheusMeterRegistry sRegistry,
			PrometheusMeterRegistry hRegistry) {
		registry = cr;
		systemRegistry = sRegistry;
		httpRegistry = hRegistry;
	}

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		   final ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
		   response.setStatus(HttpServletResponse.SC_OK);
		   response.setContentType(TextFormat.CONTENT_TYPE_004);
		   Writer writer = response.getWriter();
		   try {
			    TextFormat.write004(writer, registry.metricFamilySamples());
			    writer.append(systemRegistry.scrape());
			    if(httpRegistry != null) {
			    	writer.append(httpRegistry.scrape());
			    }
			    writer.flush();
		   } finally {
		    writer.close();
		   }	   
	}
}