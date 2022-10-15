package com.tibco.bw.prometheus.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationManager {
	
	private static ConfigurationManager INSTANCE;
	private static final Object monitor = new Object();
	private final static Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
	
	
	private static final String BW_PROMETHEUS_ENABLE = "BW_PROMETHEUS_ENABLE";
	private  static final String BW_PROMETHEUS_DETAILS_ENABLE = "BW_PROMETHEUS_DETAILS_ENABLE";
	private  static final String BW_PROMETHEUS_ACTIVITY_ENABLE = "BW_PROMETHEUS_ACTIVITY_ENABLE";
	private  static final String BW_PROMETHEUS_PROMETHEUS_PORT = "BW_PROMETHEUS_PROMETHEUS_PORT";
	
	
	private boolean isPrometheusEnabled = false;
	private boolean isActivityDetailedEnabled = true;
	private boolean isProcessDetailedEnabled = true;
	private boolean isActivityEnabled = true;
	private int prometheusPort = 9095;
	
	private ConfigurationManager(){
		init();		
	}
	
	private void init() {
		if ((System.getenv(BW_PROMETHEUS_ENABLE) != null && System.getenv(BW_PROMETHEUS_ENABLE).equalsIgnoreCase("true")) || (System.getProperty(BW_PROMETHEUS_ENABLE) != null && System.getProperty(BW_PROMETHEUS_ENABLE).equalsIgnoreCase("true"))) {
			isPrometheusEnabled = true;
		}
		
		if ((System.getenv(BW_PROMETHEUS_DETAILS_ENABLE) != null && System.getenv(BW_PROMETHEUS_DETAILS_ENABLE).equalsIgnoreCase("false")) || (System.getProperty(BW_PROMETHEUS_DETAILS_ENABLE) != null && System.getProperty(BW_PROMETHEUS_DETAILS_ENABLE).equalsIgnoreCase("false"))) {
			isActivityDetailedEnabled = false;
			isProcessDetailedEnabled = false;
		}
		
		if ((System.getenv(BW_PROMETHEUS_ACTIVITY_ENABLE) != null && System.getenv(BW_PROMETHEUS_ACTIVITY_ENABLE).equalsIgnoreCase("false")) || (System.getProperty(BW_PROMETHEUS_ACTIVITY_ENABLE) != null && System.getProperty(BW_PROMETHEUS_ACTIVITY_ENABLE).equalsIgnoreCase("false"))) {
			isActivityDetailedEnabled = false;
			isActivityEnabled = false;
		}
		
		try {
			if (System.getenv(BW_PROMETHEUS_PROMETHEUS_PORT) != null){
				prometheusPort = Integer.parseInt(System.getenv(BW_PROMETHEUS_PROMETHEUS_PORT));
				
			}
			
			if (System.getProperty(BW_PROMETHEUS_PROMETHEUS_PORT) != null){
				prometheusPort = Integer.parseInt(System.getProperty(BW_PROMETHEUS_PROMETHEUS_PORT));
				
			}
		}catch(NumberFormatException ex ) {
			if(logger.isWarnEnabled()) {
				logger.warn("Prometheus port will be established as :"+prometheusPort);
			}
		}
		

		if (logger.isDebugEnabled()) {
			logger.debug("isPrometheusEnabled: "+isPrometheusEnabled);
			logger.debug("isProcessDetailedEnabled: "+isProcessDetailedEnabled);
			logger.debug("isActivityDetailedEnabled: "+isActivityDetailedEnabled);
			logger.debug("isActivityEnabled: "+isActivityEnabled);
		}
		

		
	}

	public static ConfigurationManager getInstance(){
		if(INSTANCE == null){
			synchronized (monitor) {
				if(INSTANCE == null){
					INSTANCE = new ConfigurationManager();
				}
				}
		}
		return INSTANCE;
		
	}

	public boolean isPrometheusEnabled() {
		return isPrometheusEnabled;
	}

	public void setPrometheusEnabled(boolean isPrometheusEnable) {
		this.isPrometheusEnabled = isPrometheusEnable;
	}

	public boolean isActivityDetailedEnabled() {
		return isActivityDetailedEnabled;
	}

	public void setActivityDetailedEnabled(boolean isActivityDetailedEnable) {
		this.isActivityDetailedEnabled = isActivityDetailedEnable;
	}

	public boolean isProcessDetailedEnabled() {
		return isProcessDetailedEnabled;
	}

	public void setProcessDetailedEnabled(boolean isProcessDetailedEnabled) {
		this.isProcessDetailedEnabled = isProcessDetailedEnabled;
	}

	public boolean isActivityEnabled() {
		return isActivityEnabled;
	}

	public void setActivityEnabled(boolean isActivityEnabled) {
		this.isActivityEnabled = isActivityEnabled;
	}

	public int getPrometheusPort() {
		return prometheusPort;
	}


	
	

}
