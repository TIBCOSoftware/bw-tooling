package com.tibco.bw.prometheus.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationManager {
	
	private static ConfigurationManager INSTANCE;
	private static final Object monitor = new Object();
	private final static Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
	
	
	private static final String BW_PROMETHEUS_ENABLE = "BW_PROMETHEUS_ENABLE";
	private  static final String BW_PROMETHEUS_DETAILS_ENABLE = "BW_PROMETHEUS_DETAILS_ENABLE";
	
	
	private boolean isPrometheusEnabled = false;
	private boolean isActivityDetailedEnabled = true;
	private boolean isProcessDetailedEnabled = true;
	
	
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
		
		logger.warn("isPrometheusEnabled: "+isPrometheusEnabled);
		logger.warn("isProcessDetailedEnabled: "+isProcessDetailedEnabled);
		logger.warn("isActivityDetailedEnabled: "+isActivityDetailedEnabled);
		
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
	
	
	

}
