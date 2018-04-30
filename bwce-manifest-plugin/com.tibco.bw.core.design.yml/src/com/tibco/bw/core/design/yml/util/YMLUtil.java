/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.tibco.bw.core.design.debug.utils.BWResourceUtils;
import com.tibco.bw.core.design.yml.Messages;
import com.tibco.bw.core.design.yml.create.extension.IManifestYMLPlatformExtension;
import com.tibco.bw.core.design.yml.create.extension.ManifestFileCreationExtFactory;
import com.tibco.bw.core.design.yml.model.YMLConstants;
import com.tibco.zion.dc.wizard.GenerateEarOperation;

public class YMLUtil {
	
	public static IPath getManifestYAMLFileFrom(IProject moduleProject){
		if(!isAccessible(moduleProject)){
			return Path.EMPTY;
		}
		
		IFile manifestYMLFile = moduleProject.getFile(YMLConstants.MANIFEST_YML);
		
		if (manifestYMLFile == null) {
			String msg =  MessageFormat.format(Messages.ManifestYAMLPackager_missingYMLFile,moduleProject.getName(),YMLConstants.MANIFEST_YML);
			Logger.getLogger(YMLUtil.class.getName()).info(msg);
			return Path.EMPTY;
		}
		
		return manifestYMLFile.getLocation();
	}

	private static boolean isAccessible(IProject moduleProject) {
		if( !moduleProject.isAccessible() || !moduleProject.isOpen()){
			String msg = MessageFormat.format(Messages.ManifestYAMLPackager_projectClosed,moduleProject.getName());
			Logger.getLogger(YMLUtil.class.getName()).info(msg);
			return false;
		}
		return true;
	}
	
	public static String getEARFileNameFor(IProject moduleProject) {
		IProject applicationByModule = BWResourceUtils.INSTANCE.getApplicationByModule(moduleProject);
		String defaultEarName = GenerateEarOperation.getDefaultEarName(applicationByModule);
		return defaultEarName;
	}
	
	public static InputStream getInputStream(IResource resource)
			throws UnsupportedEncodingException, FileNotFoundException {
		IPath location = resource.getLocation();
		File file = location.toFile();
		InputStream stream = new FileInputStream(file);
		return stream;
	}
	

	public static boolean isManifestPresentIn(IProject moduleProject){
		if(!isAccessible(moduleProject)){
			return false;
		}
		
		IPath manifestYAMLFilePath = YMLUtil.getManifestYAMLFileFrom(moduleProject);
		if(manifestYAMLFilePath == null){
			return false;
		}
		
		File file = manifestYAMLFilePath.toFile();
		boolean exists = file.exists();
		boolean canRead = file.canRead();
		if(exists && canRead){
			return true;
		}
		return false;
	}

	public static IManifestYMLPlatformExtension getExtensionFor(String platform) {
		ManifestFileCreationExtFactory factory = new ManifestFileCreationExtFactory();
		Map<String, IManifestYMLPlatformExtension> extensions = factory.getExtensions();
		
		IManifestYMLPlatformExtension iManifestYMLCreationExtension = extensions.get(platform);
		return iManifestYMLCreationExtension;
	}
}
