/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf.editor.services;

import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.pde.internal.ui.editor.IContextPart;

import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFWrapper;

@SuppressWarnings("restriction")
public class ServicesEditingSupport extends EditingSupport {

	private final TableViewer viewer;
	private final CellEditor editor;
	private YMLEditingModel model;
	private YMLModelPCFWrapper wrapper;
	IContextPart part;

	public ServicesEditingSupport(TableViewer viewer, YMLEditingModel model,
			YMLModelPCFWrapper wrapper2, IContextPart part) {
		super(viewer);
		this.viewer = viewer;
		this.part = part;
		this.editor = new TextCellEditor(viewer.getTable());
		this.model = model;
		this.wrapper = wrapper2;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	@Override
	protected boolean canEdit(Object e) {
	        return true;
	}

	@Override
	protected Object getValue(Object element) {
		return element.toString();
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (element.equals(value))
			return;
		part.fireSaveNeeded();
		
		List<String> servicesList = wrapper.getServicesList();
		int index = servicesList.indexOf(element.toString());
		servicesList.set(index, value.toString());
		wrapper.putServicesList(servicesList);
		viewer.setInput(servicesList);
		model.setDirty(true);
		viewer.refresh();
	}
}
