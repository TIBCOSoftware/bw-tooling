package com.tibco.bw.core.design.yml.pcf.editor.environment;

import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.pde.internal.ui.editor.IContextPart;

import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFWrapper;

@SuppressWarnings("restriction")
public class VariableValueEditingSupport extends EditingSupport {

	private final TableViewer viewer;
	private final CellEditor editor;
	private YMLEditingModel model;
	private IContextPart part;
	private YMLModelPCFWrapper wrapper;

	public VariableValueEditingSupport(TableViewer viewer,YMLModelPCFWrapper wrapper,
			YMLEditingModel model, IContextPart part) {
		super(viewer);
		this.viewer = viewer;
		this.part = part;
		this.editor = new TextCellEditor(viewer.getTable());
		this.wrapper = wrapper;
		this.model = model;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return editor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		final String valueName = ((Map.Entry<String, String>) element)
				.getValue();
		return valueName;
	}

	@Override
	protected void setValue(Object element, Object value) {

		String oldvalue = ((Map.Entry<String, String>) element).getValue();
		String key = ((Map.Entry<String, String>) element).getKey();
		if ( oldvalue.equals(value.toString()))
			return;
		part.fireSaveNeeded();
		wrapper.getEnvironmentVariableMap().put(key, value.toString());
		model.setDirty(true);
		viewer.refresh();
	}
}
