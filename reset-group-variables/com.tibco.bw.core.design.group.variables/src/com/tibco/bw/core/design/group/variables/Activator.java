package com.tibco.bw.core.design.group.variables;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.tibco.bw.core.design.group.variables"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Returns an image for the image file from the given plug-in at the given
	 * path. Clients do not need to dispose this image. Images will be disposed
	 * automatically.
	 * 
	 * @param path the path of the image within the given plug-in
	 * @return image instance of the Image
	 */
	public Image getBundledImageFromPlugin(String path, String pluginID) {
		Image image = getImageRegistry().get(path);
		if (image == null) {
			getImageRegistry().put(path, imageDescriptorFromPlugin(pluginID, path));
			image = getImageRegistry().get(path);
		}
		return image;
	}

	public void log(String message, Throwable t, int severityCode) {
		ILog log = getLog();
		if (null != log) {
			log.log(new Status(severityCode, PLUGIN_ID, severityCode, message, t));
		}
	}

	public void logError(String message, Throwable t) {
		log(message, t, IStatus.ERROR);
	}
}
