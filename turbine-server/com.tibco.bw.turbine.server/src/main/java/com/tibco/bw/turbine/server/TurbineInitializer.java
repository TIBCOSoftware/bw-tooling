/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.turbine.server;

import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.netflix.turbine.plugins.PluginsFactory;
import com.netflix.turbine.init.TurbineInit;

public class TurbineInitializer implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(TurbineInitializer.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    	logger.info("Starting Turbine server");
        PluginsFactory.setInstanceDiscovery(new OpenShiftDiscovery());
        TurbineInit.init();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Stopping Turbine server");
        TurbineInit.stop();
    }
}
