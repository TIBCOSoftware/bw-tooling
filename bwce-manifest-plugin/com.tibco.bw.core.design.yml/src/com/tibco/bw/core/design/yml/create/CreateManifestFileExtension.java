/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.yml.create;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com.tibco.bw.core.design.project.wizard.extension.IBWProjectPostCreationExtension;
import com.tibco.bw.core.design.resource.BWDesignConstant;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileWriter;
import com.tibco.zion.dc.util.ProductDescriptorRegistry;
import com.tibco.zion.project.core.ContainerPreferenceProject;

public class CreateManifestFileExtension implements IBWProjectPostCreationExtension{

	@Override
	public void execute(IProject project, String applicationName, Optional<String> moduleType) {
		//As of now the only supported platform is PCF.
		//This should be obtained from the project or workspace later on.
		if(!isSM(moduleType) && isPCF()){
			ManifestYMLFileWriter.createManifestYmlFile(project, applicationName);
		}
	}

	private boolean isSM(Optional<String> moduleType){
		if(!moduleType.isPresent()){
			return false; //consider it as SM if there's no value present.
		}
		return BWDesignConstant.SHARED_MODULE.equals(moduleType.get());
	}
	
	private boolean isPCF(){
		boolean isCloudContainer = ProductDescriptorRegistry.INSTANCE.supports(ProductDescriptorRegistry.FEATURE_CLOUD_CONTAINER );
		boolean isPCF = ContainerPreferenceProject.getCurrentContainer() == ContainerPreferenceProject.Container.PCF;
		return isCloudContainer && isPCF ;
	}
}
