package com.tibco.bw.core.design.yml.editor;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;

public class FieldFactory {
	private FieldFactory(){
		
	}
	public static Font getBoldLabelFont() {
		return JFaceResources.getFontRegistry().getBold(
				"org.eclipse.jface.defaultfont"); //$NON-NLS-1$
	}
}
