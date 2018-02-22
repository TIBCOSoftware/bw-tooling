package com.tibco.bw.core.design.yml.model.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.tibco.bw.core.design.yml.create.extension.IManifestYMLPlatformExtension;
import com.tibco.bw.core.design.yml.model.Messages;
import com.tibco.bw.core.design.yml.model.YMLConstants;
import com.tibco.bw.core.design.yml.model.YMLModel;
import com.tibco.bw.core.design.yml.util.YMLUtil;

public class ManifestYMLFileWriter {
	
	private ManifestYMLFileWriter(){}
	
	public static void createManifestYmlFile(IProject project, String appName){
		ByteArrayOutputStream stream = null;
		try {
			
			IFile manifestYMLFile = project.getFile(YMLConstants.MANIFEST_YML);
			if (manifestYMLFile == null) {
				String message = MessageFormat.format(Messages.ManifestYMLFileWriter_accessErrorMessage, project.getName(),YMLConstants.MANIFEST_YML );
				Logger.getLogger(ManifestYMLFileWriter.class.getName()).info(message);
				return ;
			}
			stream = new ByteArrayOutputStream();
			
			IManifestYMLPlatformExtension iManifestPlatformExtension = YMLUtil.getExtensionFor(YMLConstants.PLATFORM_PCF);
			URL urlToDefaultJSONFile = iManifestPlatformExtension.getURLToDefaultJSONFile();
			YMLModel model = ManifestYMLFileReader.getDefaultModelFrom(urlToDefaultJSONFile);
			iManifestPlatformExtension.setAppName(appName,model);

			YAMLMapper mapper = new YAMLMapper();
			mapper.writeValue(stream, model);
			manifestYMLFile.create(new ByteArrayInputStream(stream.toByteArray()),
					false, null);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} finally{
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static String writeManifestToStream(YMLModel model){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		YAMLMapper mapper = new YAMLMapper();
		try {
			mapper.writeValue(stream, model);
			return stream.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ""; //$NON-NLS-1$
	}
	
	public static void writeManifest(YMLModel model, Path pathToManifestFile){
		File file = pathToManifestFile.toFile();
		FileOutputStream fstream = null;
		try {
			fstream = new FileOutputStream(file);
			YAMLMapper mapper = new YAMLMapper();
			mapper.writeValue(fstream, model);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if (fstream != null) {
				try {
					fstream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
