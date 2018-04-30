/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.yml.create;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

import com.tibco.zion.dc.util.DeploymentConfigurationsHelper;

public class ModuleProjectPropertyTester extends PropertyTester {

	public ModuleProjectPropertyTester() {
		
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (receiver instanceof IProject) {
			IProject project = (IProject) receiver;
			if (project.isAccessible()) {
				try {
					if (project.hasNature(DeploymentConfigurationsHelper.BW_MODULE_NATURE) ) {
						IPluginModelBase pluginModelBase = PluginRegistry.findModel(project);
						if (pluginModelBase != null)
							return true;
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}

			}
		}
		
		return false;
	}

}
