package com.tibco.bw.core.design.group.variables.reset;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import com.tibco.bw.core.design.project.core.util.BWManifestHelper;

public class ResetVariablesPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IProject) {
			if (BWManifestHelper.INSTANCE.isBWApplication((IProject) receiver)) {
				return false;
			}
			return true;
		}
		return false;
	}
	
}