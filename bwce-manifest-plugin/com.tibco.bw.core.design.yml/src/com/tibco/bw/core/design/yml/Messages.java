package com.tibco.bw.core.design.yml;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.tibco.bw.core.design.yml.messages"; //$NON-NLS-1$
	
	public static String ManifestYAMLPackager_CopyingFile;
	public static String ManifestYAMLPackager_fileNotExported;
	public static String ManifestYAMLPackager_missingYMLFile;
	public static String ManifestYAMLPackager_projectClosed;
	public static String RenameApplicationUpdateYMLFileParticipant_name;
	public static String RenameApplicationUpdateYMLFileChange_name;
	public static String Refactoring_invalidArguments;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
