package com.tibco.bw.core.design.yml.pcf.editor.environment;

import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.pde.internal.ui.editor.IContextPart;

import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFWrapper;

@SuppressWarnings("restriction")
public class VariableKeyEditingSupport extends EditingSupport {

	 private final TableViewer viewer;
	 private final CellEditor editor;
	 private YMLEditingModel model;
	 private IContextPart part;
	private YMLModelPCFWrapper wrapper;
	 
	public VariableKeyEditingSupport(TableViewer viewer , YMLModelPCFWrapper ymlWrapper,YMLEditingModel model, IContextPart part,Properties properties) {
		super(viewer);
		this.viewer = viewer;
		this.part=part;
		this.wrapper= ymlWrapper;
		this.model = model;
		this.editor = new TextCellEditor(viewer.getTable());
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
		final String valueName = ((Map.Entry<String, String>)element).getKey();
        return valueName;
	}

	@Override
	protected void setValue(Object element, Object value) {
		String key = ((Map.Entry<String, String>)element).getKey();
		if (isInvalid(value, key)) 
			return;
		
		final String val = wrapper.getEnvironmentVariableMap().remove(key).toString();
		wrapper.getEnvironmentVariableMap().put((String)value,val);
		part.fireSaveNeeded();
		model.setDirty(true);
		viewer.refresh();
	}

	private boolean isInvalid(Object value, String key) {
		return value==null || "".equals(value) || key.equals(value); //$NON-NLS-1$
	}
	
}
