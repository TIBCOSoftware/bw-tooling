/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.yml.export;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import com.tibco.bw.core.design.yml.Messages;
import com.tibco.bw.core.design.yml.create.extension.IManifestYMLPlatformExtension;
import com.tibco.bw.core.design.yml.model.YMLConstants;
import com.tibco.bw.core.design.yml.model.YMLModel;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileReader;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileWriter;
import com.tibco.bw.core.design.yml.util.YMLUtil;
import com.tibco.schemas.tra.model.core.packaging.Module;
import com.tibco.zion.dc.ProductPackager;
import com.tibco.zion.dc.util.jar.JarUpdateException;
import com.tibco.zion.dc.util.jar.JarUpdater;
import com.tibco.zion.dc.util.jar.JarUpdaterAction;

public class ManifestYMLPackager extends ProductPackager {

	@Override
	public void updateDeploymentArtifact(Module module, IProject moduleProject,
			File jarFile) {
		
		//Delete the manifest.yml file from the jar
		//This file is already exported to same dir as the EAR file by this class.
		JarUpdater updater = new JarUpdater();
		updater.addUpdaterAction(new ManifestDeleteAction());
		try {
			updater.updateJarContent(new JarFile(jarFile));
		} catch (JarUpdateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean copy(IPath filepath, IPath destinationEARPath, String earFileName){
		IPath absoluteFilePath = filepath.makeAbsolute();
		java.nio.file.Path filePath = Paths.get(absoluteFilePath.toOSString());
		String fileName = filepath.lastSegment();
		
		if(destinationEARPath.toString().endsWith(YMLConstants.EAR_EXTENSION)){
			destinationEARPath = destinationEARPath.removeLastSegments(1);
		}
		
		java.nio.file.Path destinationPath = Paths.get(destinationEARPath.toString(), fileName);
		
		try {
			Files.copy(filePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
			updateEARPathInManifestFile(destinationEARPath,earFileName);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public void performPackaging(IProject moduleProject,
			IPath destinationEARPath) {
		IPath manifestYAMLFilePath = YMLUtil.getManifestYAMLFileFrom(moduleProject);
		
		if(manifestYAMLFilePath.isEmpty()){
			String msg = MessageFormat.format(Messages.ManifestYAMLPackager_fileNotExported,  YMLConstants.MANIFEST_YML , moduleProject.getName());
			Logger.getLogger(this.getClass().getName()).info(msg);
			return;
		}
		
		String msg = MessageFormat.format(Messages.ManifestYAMLPackager_CopyingFile, 
				YMLConstants.MANIFEST_YML , moduleProject.getName(), destinationEARPath.toString());
		Logger.getLogger(this.getClass().getName()).info(msg);
		String defaultEarName = YMLUtil.getEARFileNameFor(moduleProject);
		copy(manifestYAMLFilePath, destinationEARPath, defaultEarName);
	}

	
	private void updateEARPathInManifestFile(IPath destinationEARPath, String earFileName){
		//De-serialize the file.
		String str = System.getProperty( "file.separator" ); //$NON-NLS-1$
		String manifestFileFullPath = destinationEARPath.toOSString().concat(str).concat(YMLConstants.MANIFEST_YML);
		java.nio.file.Path path = Paths.get(manifestFileFullPath);
		try {
			YMLModel model = ManifestYMLFileReader.getModelFrom(path);
			
			String pathString =destinationEARPath.toOSString();
			pathString = pathString.concat(str).concat(earFileName);
			
			IManifestYMLPlatformExtension platformExtension = YMLUtil.getExtensionFor(YMLConstants.PLATFORM_PCF);
			platformExtension.setEARPath(pathString,model);
			
			//serialize the file.
			ManifestYMLFileWriter.writeManifest(model, path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class ManifestDeleteAction extends JarUpdaterAction {

		@Override
		public void performUpdate(String unjaredFilePath)
				throws JarUpdateException {
			java.nio.file.Path path = Paths.get(unjaredFilePath);
			try {
				Files.walkFileTree(path, new FindManifest());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private class FindManifest extends SimpleFileVisitor<java.nio.file.Path>{
			@Override
			public FileVisitResult visitFile(java.nio.file.Path file,
					BasicFileAttributes attrs) throws IOException {
				if(file == null){
					return FileVisitResult.CONTINUE;
				}
				
				java.nio.file.Path fileNamePath = file.getFileName();
				if(fileNamePath == null){
					return FileVisitResult.CONTINUE;
				}
				
				String fileName = fileNamePath.toString();
				if(fileName.endsWith(YMLConstants.MANIFEST_YML)){
					Files.delete(file);
				}
				return FileVisitResult.CONTINUE;
			}
		}
	}
}
