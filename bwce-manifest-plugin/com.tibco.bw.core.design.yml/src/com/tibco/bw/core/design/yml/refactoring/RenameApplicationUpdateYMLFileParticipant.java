package com.tibco.bw.core.design.yml.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import com.tibco.zion.dc.util.DeploymentConfigurationsHelper;
import com.tibco.bw.core.design.yml.Messages;

public class RenameApplicationUpdateYMLFileParticipant extends RenameParticipant {
	
	protected IProject sourceProject;
	protected IProject targetProject;

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IProject) {
			sourceProject = (IProject) element;
			if(DeploymentConfigurationsHelper.INSTANCE.isApplication(sourceProject)){
				return true;
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return Messages.RenameApplicationUpdateYMLFileParticipant_name;
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		RefactoringStatus result = new RefactoringStatus();

		RenameArguments arguments = getArguments();
		if (arguments == null) {
			result.addFatalError(Messages.Refactoring_invalidArguments);
			return result;
		}

		String newProjectName = arguments.getNewName();
		targetProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newProjectName);

		return result;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (sourceProject == null || targetProject == null || getArguments() == null || !getArguments().getUpdateReferences()) {
			return null;
		}
		
		CompositeChange rootChange = new CompositeChange(Messages.RenameApplicationUpdateYMLFileChange_name);
		rootChange.markAsSynthetic();
		
		//Add code here
		rootChange.add(new RenameApplicationUpdateYMLFileChange(sourceProject, targetProject));
		return rootChange;
	}

}
