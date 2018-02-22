package com.tibco.bw.core.design.yml.create.extension;

import java.net.MalformedURLException;
import java.net.URL;

import com.tibco.bw.core.design.yml.model.YMLModel;

public interface IManifestYMLPlatformExtension {

	URL getURLToDefaultJSONFile() throws MalformedURLException;

	void setAppName(String appName, YMLModel model);

	void setEARPath(String pathString, YMLModel model);
}
