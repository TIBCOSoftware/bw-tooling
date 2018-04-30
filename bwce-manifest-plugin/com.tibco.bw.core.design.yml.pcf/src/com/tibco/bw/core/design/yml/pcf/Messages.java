/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.tibco.bw.core.design.yml.pcf.messages"; //$NON-NLS-1$

	public static String Container_Dialog_Class;

	public static String ServicesSection_chooseBtnTooltip;

	public static String ApplicationConfigurationSection_buildpack;
	public static String ApplicationConfigurationSection_memory;
	public static String ApplicationConfigurationSection_name;
	public static String ApplicationConfigurationSection_timeout;
	public static String ApplicationConfigurationSection_timeoutTooltip;
	public static String ApplicationConfigurationSection_title;
	public static String EnvironmentConfigurationSection_addBtnTooltip;
	public static String EnvironmentConfigurationSection_chooseBtnTooltip;
	public static String EnvironmentConfigurationSection_deleteBtnTooltip;
	public static String EnvironmentConfigurationSection_envVarColTitle;
	public static String EnvironmentConfigurationSection_valueColTitle;
	public static String EnvironmentPropertiesLoader_infoMessage;
	public static String EnvironmentVariablesSelectionDialog_0;
	public static String EnvironmentVariablesSelectionDialog_1;
	public static String EnvironmentVariablesSelectionDialog_2;
	public static String EnvironmentVariablesSelectionDialog_3;
	public static String EnvironmentVariablesSelectionDialog_4;
	public static String EnvironmentVariablesSelectionDialog_5;
	public static String EnvironmentVariablesSelectionDialog_6;
	public static String EnvironmentVariablesSelectionDialog_7;
	public static String ServicesConfigurationSection_deleteBtnTooltip;
	public static String ServicesConfigurationSection_addBtnTooltip;
	public static String ServicesConfigurationSection_colTitle;

	public static String Source_Page_ID;
	public static String YMLconfigurationEditor_pageTitle;
	public static String YMLConfigurationPage_errorMsg;
	public static String YMLConfigurationPage_pageTitle;
	public static String YMLConfigurationPage_title;
	public static String YMLModelPCFWrapper_unrecognisedServices;
	public static String YMLModelPCFWrapper_unrecognizedApplications;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
