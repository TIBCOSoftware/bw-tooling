package com.tibco.bw.core.design.yml.model;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.tibco.bw.core.design.yml.model.messages"; //$NON-NLS-1$
	public static String YMLModel_missingKey;
	public static String ManifestYMLFileWriter_accessErrorMessage;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
