/*Copyright © 2019. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.group.variables.reset;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class VariableSelectionTableContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {
		
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		
	}

	@Override
	public Object[] getElements(Object inputElement) {
		Object[] elements = null;
		if (inputElement instanceof List) {
			List<?> variables = (List<?>) inputElement;
			elements = variables.toArray(new Object[variables.size()]);
		}
		return elements;
	}

}
