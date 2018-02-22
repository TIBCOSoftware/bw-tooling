package com.tibco.bw.core.design.yml.pcf.editor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

import com.tibco.bw.core.design.yml.model.YMLConstants;
import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.model.YMLModel;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileWriter;

@SuppressWarnings("restriction")
public class YMLInputContext extends InputContext {
	public static final String CONTEXT_ID = "YML-context"; //$NON-NLS-1$

	public YMLInputContext(PDEFormEditor editor, IEditorInput input,
			boolean primary) {
		super(editor, input, primary);
		create();
	}

	@Override
	public String getId() {
		return CONTEXT_ID;
	}

	@Override
	protected String getPartitionName() {
		return null;
	}

	@Override
	protected String getDefaultCharset() {
		return YMLConstants.UTF_8;
	}

	@Override
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		YMLEditingModel model = null;
		if (input instanceof IStorageEditorInput) {
			boolean isReconciling = input instanceof IFileEditorInput;
			IDocument document = getDocumentProvider().getDocument(input);
			model = new YMLEditingModel(document, isReconciling);
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				model.setUnderlyingResource(file);
				model.setCharset(file.getCharset());
			} else {
				model.setCharset(getDefaultCharset());
			}
			model.load();
		}
		return model;
	}

	@Override
	protected void addTextEditOperation(ArrayList<TextEdit> ops,
			IModelChangedEvent event) {
	}


	@Override
	public void doSave(IProgressMonitor monitor) {
		IEditorInput input = getInput();
		YMLEditingModel editingModel = (YMLEditingModel) getModel();
		YMLModel model = editingModel.getModel();
		IDocument document = getDocumentProvider().getDocument(input);
		if (getEditor().getActivePage()== 0 && editingModel.isDirty()) {
			String writeManifestToStream = ManifestYMLFileWriter.writeManifestToStream(model);
			document.set(writeManifestToStream);
		} else {
			try {
				editingModel.reload(getInputStream(document), false);
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		super.doSave(monitor);
	}

	protected InputStream getInputStream(IDocument document)
			throws UnsupportedEncodingException {
		return new BufferedInputStream(new ByteArrayInputStream(document.get()
				.getBytes(getDefaultCharset())));
	}
	
}
