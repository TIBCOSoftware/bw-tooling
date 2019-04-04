package com.tibco.bw.core.design.group.variables.reset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class ResetVariablesHandler extends AbstractHandler {
	
	List<IProject> projects = new ArrayList<IProject>();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		projects.clear();
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;

			Iterator<?> iterator = structuredSelection.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof IProject) {
					IProject project = (IProject) element;
					projects.add(project);
				}
			}
		}
		ResetVariables resetVariables = new ResetVariables(projects);
		resetVariables.performReset();
		return null;
	}
	
}