/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf.create;

import java.net.MalformedURLException;
import java.net.URL;

import com.tibco.bw.core.design.yml.create.extension.IManifestYMLPlatformExtension;
import com.tibco.bw.core.design.yml.model.YMLModel;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFWrapper;

public class ManifestYMLPCFExtension implements
		IManifestYMLPlatformExtension {

	@Override
	public void setAppName(String appName, YMLModel model) {
		YMLModelPCFWrapper.setApplicationName(appName, model);
	}
	
	@Override
	public URL getURLToDefaultJSONFile(){
		StringBuilder urlBuilder = new StringBuilder();
		urlBuilder = urlBuilder.append("platform:/plugin"); //$NON-NLS-1$
		urlBuilder = urlBuilder.append("/com.tibco.bw.core.design.yml.pcf"); //$NON-NLS-1$
		urlBuilder = urlBuilder.append("/com/tibco/bw/core/design/yml/pcf/create"); //$NON-NLS-1$
		urlBuilder = urlBuilder.append("/defaults.json"); //$NON-NLS-1$
		
		String urlString = urlBuilder.toString();
		try {
			return new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void setEARPath(String path, YMLModel model) {
		YMLModelPCFWrapper.setPath(path, model);
	}
}
