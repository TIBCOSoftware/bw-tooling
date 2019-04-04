package com.tibco.bw.core.design.group.variables.reset;

import org.eclipse.bpel.model.Variable;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.tibco.bw.core.design.group.variables.Activator;

public class VariableSelectionTableLabelProvider extends LabelProvider implements ITableLabelProvider {

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		Image image = null;
		if (columnIndex == 0) {
			if (element instanceof Variable) {
				image = Activator.getDefault().getBundledImageFromPlugin("/icons/variable_16x16.png", "com.tibco.bw.core.design.group.variables"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return image;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		String text = null;
		if(element instanceof TableColumnObject) {
			TableColumnObject tableColumnObject = (TableColumnObject) element;
			if(columnIndex == 0) {
				text = tableColumnObject.getVariable().getName();
			}
			if(columnIndex == 1) {
				text = tableColumnObject.getScope().getName();
			}
			if(columnIndex == 2) {
				text = tableColumnObject.getProcess().getName();
			}
		}
		return text;
	}

}
