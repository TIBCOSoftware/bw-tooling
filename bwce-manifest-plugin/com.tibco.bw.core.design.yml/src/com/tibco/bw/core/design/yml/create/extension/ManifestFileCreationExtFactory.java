package com.tibco.bw.core.design.yml.create.extension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;


public class ManifestFileCreationExtFactory {
	private static final String EXTENSION_POINT = "com.tibco.bw.core.design.yml.platformExtension"; //$NON-NLS-1$
	private static final String EXTENSION_ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String EXTENSION_ATTRIBUTE_CLASSNAME = "class"; //$NON-NLS-1$
	
	protected Map<String, IManifestYMLPlatformExtension> extensions 
									= new HashMap<String, IManifestYMLPlatformExtension>();
	
	public Map<String, IManifestYMLPlatformExtension> getExtensions() {
		loadExtensions();
		return Collections.unmodifiableMap(extensions);
	}
	
	public synchronized void loadExtensions() {
		if (!extensions.isEmpty()) {
			return;
		}
		IExtension[] allExtensions = getExtensionsInternal();
		if(allExtensions == null){
			return;
		}
		
		for (IExtension extension : allExtensions) {
			if (!extension.isValid()) {
				continue;
			}
			
			IConfigurationElement[] extensionElements = extension.getConfigurationElements();
			for (IConfigurationElement extensionElement : extensionElements) {
				String id = extensionElement.getAttribute(EXTENSION_ATTRIBUTE_ID);
				try {
					IManifestYMLPlatformExtension postCreateExtension = 
							(IManifestYMLPlatformExtension) extensionElement
							.createExecutableExtension(EXTENSION_ATTRIBUTE_CLASSNAME);
					extensions.put(id, postCreateExtension);
				} catch (CoreException ignore) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, ignore.getLocalizedMessage());
				}
			}
		}
	}

	private IExtension[] getExtensionsInternal() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_POINT);
		if(extensionPoint==null){
			return null; //should not happen.
		}
		
		IExtension[] allExtensions = extensionPoint.getExtensions();
		return allExtensions;
	}
}
