package com.tibco.bw.core.design.yml.refactoring;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

import com.tibco.bw.core.design.project.core.util.BWManifestHelper;
import com.tibco.bw.core.design.resource.util.BWProjectHelper;
import com.tibco.bw.core.design.yml.Messages;
import com.tibco.bw.core.design.yml.create.extension.IManifestYMLPlatformExtension;
import com.tibco.bw.core.design.yml.model.YMLConstants;
import com.tibco.bw.core.design.yml.model.YMLModel;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileReader;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileWriter;
import com.tibco.bw.core.design.yml.util.YMLUtil;
import com.tibco.schemas.tra.model.core.packaging.Module;
import com.tibco.zion.dc.util.DeploymentConfigurationsHelper;

public class RenameApplicationUpdateYMLFileChange extends ResourceChange {
	
	protected IProject sourceProject;
	protected IProject targetProject;
	
	public RenameApplicationUpdateYMLFileChange(IProject sourceProject, IProject targetProject) {
		this.sourceProject = sourceProject;
		this.targetProject = targetProject;
	}

	@Override
	protected IResource getModifiedResource() {
		return sourceProject;
	}

	@Override
	public String getName() {
		return Messages.RenameApplicationUpdateYMLFileChange_name;
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		IProject myProject = null;
		
		 Module[] modules;
		try {
			modules = DeploymentConfigurationsHelper.INSTANCE.getModules(targetProject);
			for (Module module : modules) {
				IProject project = BWProjectHelper.INSTANCE.getProject(module.getSymbolicName());
				boolean bwSharedModule = BWManifestHelper.INSTANCE.isBWSharedModule(project) || BWManifestHelper.INSTANCE.isBWBinarySharedModule(project);
				if(!bwSharedModule){
					myProject = project;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(myProject== null){
			return new RenameApplicationUpdateYMLFileChange(sourceProject, targetProject);
		}
		
		IPath manifestYAMLFileFrom = YMLUtil.getManifestYAMLFileFrom(myProject);
		
		try {
			java.nio.file.Path path = Paths.get(manifestYAMLFileFrom.toOSString());
			YMLModel model = ManifestYMLFileReader.getModelFrom(path);
			
			IManifestYMLPlatformExtension platformExtension = YMLUtil.getExtensionFor(YMLConstants.PLATFORM_PCF);
			platformExtension.setAppName(targetProject.getName(), model);
			
			//serialize the file.
			ManifestYMLFileWriter.writeManifest(model, path);
		}catch(IOException ex){
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, ex.getLocalizedMessage());
		}
		return new RenameApplicationUpdateYMLFileChange(sourceProject, targetProject);
	}

}
