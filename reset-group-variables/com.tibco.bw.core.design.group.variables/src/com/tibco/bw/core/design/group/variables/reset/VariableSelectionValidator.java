/*Copyright © 2019. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.group.variables.reset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.bpel.model.Variable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.ISelectionValidator;

import com.tibco.bw.core.design.group.variables.Messages;

public class VariableSelectionValidator implements ISelectionValidator {

	@Override
	public String isValid(Object selection) {
		List<Variable> selectedVariables = new ArrayList<Variable>();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (Iterator<?> itor = structuredSelection.iterator(); itor.hasNext();) {
				Object element = itor.next();
				if (element instanceof TableColumnObject) {
					selectedVariables.add(((TableColumnObject) element).getVariable());
				}
			}
		}
		if (selectedVariables.isEmpty()) {
			return Messages.Select_Variables_To_Reset;
		}
		return null;
	}

}
