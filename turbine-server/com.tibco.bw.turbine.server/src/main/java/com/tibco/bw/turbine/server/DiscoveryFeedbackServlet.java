/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.turbine.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.netflix.turbine.discovery.Instance;

public class DiscoveryFeedbackServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final OpenShiftDiscovery DISCOVERY = OpenShiftDiscovery.getOpenShiftDiscovery();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {        
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<h1>Hystrix Endpoints: "+OpenShiftDiscovery.getNamespace() +"</h1>");
        try {
            for (Instance instance : DISCOVERY.getInstanceList()) {           	
                out.println("<h3>" + instance.getHostname() + " : " + (instance.isUp()?"Running":"Unreachable") + "</h3>");
            }
        } catch (Throwable t) {
            t.printStackTrace(out);
        }
        out.flush();
    }

}
