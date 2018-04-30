/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.yml.editor;
import org.eclipse.pde.internal.ui.editor.PDEFormTextEditorContributor;

/**
 * 
 * This class is created only because eclipse mandates us to do that. 
 * This editor does not contribute any menu actions.
 *
 */
@SuppressWarnings("restriction")
public class ManifestContribution extends PDEFormTextEditorContributor {
	public ManifestContribution() {
		super("&Plugin"); //$NON-NLS-1$
	}
}
