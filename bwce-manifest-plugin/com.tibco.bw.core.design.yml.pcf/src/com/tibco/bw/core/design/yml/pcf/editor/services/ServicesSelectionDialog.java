/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf.editor.services;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com.tibco.bw.core.design.yml.pcf.YMLModelPCFWrapper;
import com.tibco.zion.common.ZionCommon;
import com.tibco.zion.dc.cloudcontainer.descriptor.ServiceDescriptor;
import com.tibco.zion.dc.cloudcontainer.dialog.ContainerVariablesSelectionDialog;

public class ServicesSelectionDialog extends ContainerVariablesSelectionDialog{

	private YMLModelPCFWrapper wrapper;
	private ServicesSection section;
	
	public ServicesSelectionDialog(Shell shell , ServicesSection section) {
		super(shell);
		this.wrapper  = section.wrapper;
		this.section = section;
		this.multiSelection = true;
		
	}
	
	@Override
	protected IStatus validateItem(Object item) {
		if(!(item instanceof ServiceDescriptor)){
			return  new Status(IStatus.ERROR, ZionCommon.PLUGIN_ID, IStatus.ERROR, Messages.Invalid_Data_Selection, null);
		}
		return Status.OK_STATUS;
	}

	@Override
	protected void okPressed() {
		//First fetch the model that we are interested in 
		 List<String> servicesList = wrapper.getServicesList();
		 Object[] results = this.getCurrentSelection();
		 for(Object result :results ){
			if(result instanceof ServiceDescriptor){
				ServiceDescriptor desc = (ServiceDescriptor)result;
				servicesList.add(desc.getServiceName());
				wrapper.putServicesList(servicesList);
				section.fireModelDirty();
			}
		 }
		 
				
		super.okPressed();
	}
}
