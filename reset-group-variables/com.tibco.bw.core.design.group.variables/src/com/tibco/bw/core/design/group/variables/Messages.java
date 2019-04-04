package com.tibco.bw.core.design.group.variables;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.tibco.bw.core.design.group.variables.messages"; //$NON-NLS-1$
	public static String Content_Provider_Error;
	public static String Group_Start_Activity_Error;
	public static String Input_Error;
	public static String Label_Provider_Error;
	public static String Select_Variable;
	public static String Select_Variables_To_Reset;
	public static String Variables_To_Reset;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
