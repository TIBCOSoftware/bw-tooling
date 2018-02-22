package com.tibco.bw.core.design.yml.pcf.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;

@SuppressWarnings("restriction")
public class YMLContextManager extends InputContextManager {

	public YMLContextManager(PDEFormEditor editor) {
		super(editor);
	}

	public IBaseModel getAggregateModel() {
		return findBuildModel();
	}

	private IBaseModel findBuildModel() {
		InputContext bcontext = findContext(YMLInputContext.CONTEXT_ID);
		return (bcontext != null) ? bcontext.getModel() : null;
	}

	@Override
	public void save(IProgressMonitor monitor) {
		super.save(monitor);
	}
}
