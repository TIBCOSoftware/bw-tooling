package com.tibco.bw.core.design.yml.model;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.scanner.ScannerException;
import com.tibco.bw.core.design.yml.model.io.ManifestYMLFileReader;

@SuppressWarnings("restriction")
public class YMLEditingModel extends AbstractEditingModel {

	private YMLModel ymlModel;

	private boolean doesYamlFileHaveErrors = false;
	
	public YMLEditingModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}

	@Override
	public void load(InputStream source, boolean outOfSync)
			throws CoreException {
		setDoesYamlHasErrors(false);
		try {
			this.getUnderlyingResource().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			ymlModel = ManifestYMLFileReader.getModelFrom(source);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			ymlModel = null;
			if (e.getCause() instanceof ScannerException) {
				ScannerException ex = (ScannerException) e.getCause();
				createErrorMarker(ex);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			if (e instanceof ScannerException) {
				ymlModel = null;
				createErrorMarker((ScannerException) e);
			}
		}
	}

	private void createErrorMarker(ScannerException e) throws CoreException {
		Mark errorMark = null;
		IMarker marker = this.getUnderlyingResource().createMarker(
				IMarker.PROBLEM);
		
		if(e.getContextMark()!=null){
			errorMark = e.getContextMark();
		}
		else if(e.getProblemMark() != null)
			errorMark = e.getProblemMark();
		else
			return;
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		marker.setAttribute(IMarker.MESSAGE, e.getProblem());

		// SnakeYAML uses a 0-based line number while IMarker uses a 1-based
		// line number so must add 1 to the number reported by SnakeYAML.
		int lineNumber = errorMark.getLine() + 1;
		marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		setDoesYamlHasErrors(true);
	}

	public YMLModel getModel() {
		return ymlModel;
	}


	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

	@Override
	public void adjustOffsets(IDocument document) throws CoreException {
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	public void setYmlModel(YMLModel ymlModel) {
		this.ymlModel = ymlModel;
	}

	public boolean doesYamlFileHaveErrors() {
		return doesYamlFileHaveErrors;
	}

	private void setDoesYamlHasErrors(boolean doesYamlHasErrors) {
		this.doesYamlFileHaveErrors = doesYamlHasErrors;
	}
}
