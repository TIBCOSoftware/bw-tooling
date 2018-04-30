/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf.editor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.tibco.bw.core.design.yml.model.YMLConstants;
import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.pcf.Messages;
import com.tibco.bw.core.design.yml.pcf.editor.appconfig.ApplicationSection;
import com.tibco.bw.core.design.yml.pcf.editor.environment.EnvironmentVariablesSection;
import com.tibco.bw.core.design.yml.pcf.editor.services.ServicesSection;
import com.tibco.bw.core.design.yml.util.YMLUtil;

@SuppressWarnings("restriction")
public class SimplifiedEditorPage extends PDEFormPage implements IResourceChangeListener{

	public static final String PAGE_ID = "YML"; //$NON-NLS-1$

	private ApplicationSection appConfigSection;
	private EnvironmentVariablesSection envSection;
	private ServicesSection serviceSection;
	
	public SimplifiedEditorPage(FormEditor editor) {
		super(editor, PAGE_ID, Messages.YMLConfigurationPage_pageTitle);
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return null; //TODO: FIX THIS.
	}

	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		FormToolkit toolkit = mform.getToolkit();
		ScrolledForm form = mform.getForm();
		form.setText(Messages.YMLConfigurationPage_title);
		
		Composite mainComposite = mform.getForm().getBody();
		mainComposite.setLayout(FormLayoutFactory.createFormGridLayout(true, 2));
		appConfigSection = new ApplicationSection(this,form.getBody());
		
		envSection= new EnvironmentVariablesSection(this,form.getBody());
		serviceSection= new ServicesSection(this, form.getBody());
		mform.addPart(appConfigSection);
		mform.addPart(envSection);
		mform.addPart(serviceSection);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		if(((YMLEditingModel) getModel()).doesYamlFileHaveErrors()){
			this.getEditor().setActivePage("1");
		}
	}

	@Override
	public void updateFormSelection() {
	     IBaseModel baseModel = getModel();
	     IMessageManager messageManager = getManagedForm().getMessageManager();
		 messageManager.removeAllMessages();
	     if(baseModel instanceof YMLEditingModel){
	    	 YMLEditingModel model = (YMLEditingModel)baseModel;
	    	 if(model.doesYamlFileHaveErrors()){
	    		 messageManager.addMessage(null, Messages.YMLConfigurationPage_errorMsg, null, IMessageProvider.ERROR);
	    	 }
	     }
	}
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResource resource = event.getResource();
		if(resource==null){
			return;
		}
		getEditor().close(false);
		
		String osString = resource.getFullPath().toOSString();
		if(!osString.endsWith(YMLConstants.MANIFEST_YML)){
			return;
		}
		IBaseModel baseModel = this.getModel();
		if(baseModel instanceof YMLEditingModel){
	    	 YMLEditingModel model = (YMLEditingModel)baseModel;
	    	 InputStream inputStream;
			try {
				inputStream = YMLUtil.getInputStream(resource);
				model.reload(inputStream, true);
				appConfigSection.modelChanged(null);
			} catch (UnsupportedEncodingException | FileNotFoundException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
	    }
	}
	
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
}
