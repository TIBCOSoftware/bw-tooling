/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.yml.create;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.tibco.bw.core.design.debug.utils.BWResourceUtils;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileWriter;
import com.tibco.bw.core.design.yml.util.YMLUtil;

public class CreateManifestYMLHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection != null && selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			
			Iterator<?> iterator = structuredSelection.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof IProject) {
					IProject moduleProject = (IProject) element;
					try {
						IProject applicationProject = BWResourceUtils.INSTANCE.getApplicationByModule(moduleProject);

						boolean manifestPresentIn = YMLUtil.isManifestPresentIn(moduleProject);
						if(!manifestPresentIn){
							ManifestYMLFileWriter.createManifestYmlFile(moduleProject, applicationProject.getName());
						}
					} catch (Exception ignore) {
						ignore.printStackTrace();
					}
				}
			}
		}
		return null;
	}
}
