package com.tibco.bw.core.design.yml.pcf.editor;

import org.dadacoalition.yedit.editor.YEdit;
import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.core.build.IBuildObject;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import com.tibco.bw.core.design.yml.pcf.Messages;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFConstants;

@SuppressWarnings("restriction")
public class SimplifiedEditor extends MultiSourceEditor {

	private IFile file;
	private SimplifiedEditorPage page;

	@Override
	public boolean monitoredFileRemoved(IFile monitoredFile) {

		return false;
	}

	@Override
	protected InputContextManager createInputContextManager() {
		YMLContextManager manager = new YMLContextManager(this);
		return manager;
	}

	@Override
	protected void createResourceContexts(InputContextManager contexts,
			IFileEditorInput input) {
		 file = input.getFile();

		contexts.putContext(input, new YMLInputContext(this, input, true));
		contexts.monitorFile(file);
	}

	@Override
	protected void createSystemFileContexts(InputContextManager contexts,
			FileStoreEditorInput input) {
	}

	@Override
	protected void createStorageContexts(InputContextManager contexts,
			IStorageEditorInput input) {
	}

	@Override
	protected String getEditorID() {
		return YMLModelPCFConstants.BW_MANIFESTYML_EDITOR_ID;
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return null;
	}

	@Override
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IBuildObject) {
			context = fInputContextManager
					.findContext(YMLInputContext.CONTEXT_ID);
		}
		return context;
	}

	@Override
	protected void addEditorPages() {
		try {
			addPage(new SimplifiedEditorPage(this));
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		IEditorInput fileEditorInput = new FileEditorInput( file);
		try {
			addPage((IEditorPart) new YEdit(), fileEditorInput);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		setPageText(1, Messages.YMLconfigurationEditor_pageTitle);
		
		//To add the old source page, use this code. Kept here for reference.
		//addSourcePage(YMLInputContext.CONTEXT_ID);
	}

	@Override
	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	@Override
	protected String computeInitialPageId() {
		// Retrieve the initial page
		return SimplifiedEditorPage.PAGE_ID;
	}

	@Override
	protected void firePropertyChange(int propertyId) {
		super.firePropertyChange(propertyId);
	}

	@Override
	public void contextRemoved(InputContext context) {
		
	}

	@Override
	public void monitoredFileAdded(IFile monitoredFile) {
		
	}
	@Override
	public IFormPage setActivePage(String pageId) {
		//As of now not getting any method so that Source page can be set as active page
		//existing setactive method only set the active page if it is kind of pdeformpage which requires PageID
		if(pageId.equals(Messages.Source_Page_ID));
		setActivePage(1);
		return super.setActivePage(pageId);
	}
	
}
