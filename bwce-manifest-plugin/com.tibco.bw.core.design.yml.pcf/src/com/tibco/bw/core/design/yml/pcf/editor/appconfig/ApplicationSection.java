/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf.editor.appconfig;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.tibco.bw.core.design.yml.editor.FieldFactory;
import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.pcf.Messages;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFConstants;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFWrapper;
import com.tibco.bw.core.design.yml.pcf.editor.YMLInputContext;

@SuppressWarnings("restriction")
public class ApplicationSection extends PDESection  {

	private Composite sectionContainer;
	protected FormEntry versionEntry;
	protected FormEntry nameEntry;
	protected FormEntry providerEntry;
	protected Composite mainComposite;
	protected Label apiConnectorLabel;
	protected Text apiConnectorText;
	private FormEntry timeoutEntry;
	private FormEntry buildPackEntry;
	private YMLEditingModel ymlEditingModel;
	private FormEntry memoryEntry;
	private YMLModelPCFWrapper wrapper;

	public ApplicationSection(PDEFormPage page, Composite parent) {
		super(page, parent,Section.NO_TITLE_FOCUS_BOX);
		ymlEditingModel = getYMLModel();
		initializeWrapper();
		createClient(getSection(), page.getManagedForm().getToolkit());
		getSection().setText(Messages.ApplicationConfigurationSection_title);
	}

	private void initializeWrapper() {
		wrapper = new YMLModelPCFWrapper(ymlEditingModel.getModel(), ymlEditingModel);
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		sectionContainer = toolkit.createComposite(section);
		toolkit.paintBordersFor(sectionContainer);
		GridLayout createSectionClientGridLayout = FormLayoutFactory.createSectionClientGridLayout(false, 2);
		sectionContainer.setLayout(createSectionClientGridLayout);
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();

		createNameEntry(sectionContainer, toolkit, actionBars);
		createMemoryEntry(sectionContainer, toolkit, actionBars);
		createTimeoutEntry(sectionContainer, toolkit, actionBars);
		createBuildPackEntry(sectionContainer, toolkit, actionBars);

		createSectionToolbar(section, toolkit);
		section.setClient(sectionContainer);
		setEnabled(!ymlEditingModel.doesYamlFileHaveErrors());
		getYMLModel().addModelChangedListener(this);
		setEnabled(!ymlEditingModel.doesYamlFileHaveErrors());
		setUIValues();
	}

	private void setUIValues() {
		
		try {
			Map<String, Object> applicationMap = wrapper.getApplicationMap();
			fillUIValues(applicationMap);
		} catch (IllegalFormatException ex) {
			Logger.getLogger(getClass().getName()).severe(ex.getMessage());
			setEnabled(false);
		}
	}

	protected void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		section.setTextClient(toolbar);
	}
	
	private void createNameEntry(Composite sectionContainer2,
		FormToolkit toolkit, IActionBars actionBars) {	
		nameEntry = new FormEntry(sectionContainer2, toolkit, Messages.ApplicationConfigurationSection_name, null, false);
		nameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
				wrapper.getApplicationMap().put(YMLModelPCFConstants.KEY_NAME, entry.getValue());
				ymlEditingModel.setDirty(true);
				} catch (IllegalFormatException ex) {
					Logger.getLogger(getClass().getName()).severe(
							ex.getMessage());
					setEnabled(false);
				}
			}
		});
	}
	

	private void createMemoryEntry(Composite sectionContainer2,
			FormToolkit toolkit, IActionBars actionBars) {
		memoryEntry = new FormEntry(sectionContainer2, toolkit, Messages.ApplicationConfigurationSection_memory, null, false);
		memoryEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					wrapper.getApplicationMap().put(YMLModelPCFConstants.KEY_MEMORY,entry.getValue());
						ymlEditingModel.setDirty(true);
				} catch (IllegalFormatException ex) {
					Logger.getLogger(getClass().getName()).severe(
							ex.getMessage());
					setEnabled(false);
				}
			}
		});
	}

	
	private void createTimeoutEntry(Composite sectionContainer2,
			FormToolkit toolkit, IActionBars actionBars) {
		timeoutEntry = new FormEntry(sectionContainer2, toolkit, Messages.ApplicationConfigurationSection_timeout, null, false);
		timeoutEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					wrapper.getApplicationMap().put(YMLModelPCFConstants.KEY_TIMEOUT,entry.getValue());
					ymlEditingModel.setDirty(true);
				} catch (IllegalFormatException ex) {
					Logger.getLogger(getClass().getName()).severe(
							ex.getMessage());
					setEnabled(false);
				}
			}
		});
		timeoutEntry.getText().setToolTipText(Messages.ApplicationConfigurationSection_timeoutTooltip);
		timeoutEntry.getText().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				List<Character> chars = wrapper.getValidCharsList();
				if(!chars.contains(e.character)){
					e.doit = false;
				}
			}
		});
	}
	
	private void createBuildPackEntry(Composite sectionContainer2,
			FormToolkit toolkit, IActionBars actionBars) {
		buildPackEntry = new FormEntry(sectionContainer2, toolkit, Messages.ApplicationConfigurationSection_buildpack, null, false);
		
		buildPackEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					wrapper.getApplicationMap().put(YMLModelPCFConstants.KEY_BUILDPACK, entry.getValue());
					ymlEditingModel.setDirty(true);
				} catch (IllegalFormatException ex) {
					Logger.getLogger(getClass().getName()).severe(
							ex.getMessage());
					setEnabled(false);
				}
			}
		});
		
	}
	
	
	protected void applyBoldFontToFormEntry(FormEntry entry) {
		if (entry == null) {
			return;
		}
		entry.getLabel().setFont(FieldFactory.getBoldLabelFont());
	}
	
	private YMLEditingModel getYMLModel() {
		InputContext context = getPage().getPDEEditor().getContextManager().findContext(YMLInputContext.CONTEXT_ID);
		if (context == null)
			return null;
		return (YMLEditingModel) context.getModel();
	}

	@Override
	public void commit(boolean onSave) {
		wrapper.clearApplications();
		wrapper.cleanEmptyServicesValues();
		wrapper.clearEnvMap();
		super.commit(onSave);
	}

	
	@Override
	public void modelChanged(IModelChangedEvent e) {
		setEnabled(!ymlEditingModel.doesYamlFileHaveErrors());
		wrapper.setModel(ymlEditingModel.getModel());
		Map<String, Object> applicationMap = null;
		try {
			applicationMap = wrapper.getApplicationMap();
			if(!applicationMap.isEmpty()){
				fillUIValues(applicationMap);
			}
			else{
				clear();
				setEnabled(!ymlEditingModel.doesYamlFileHaveErrors());
			}
		} catch (IllegalFormatException ex) {
			Logger.getLogger(getClass().getName()).severe(ex.getMessage());
			setEnabled(false);
			clear();
		}
		
		
	}

	private void fillUIValues(Map<String, Object> applicationMap) {
		Object name = applicationMap.get(YMLModelPCFConstants.KEY_NAME);
		if(name!=null)
		{
			nameEntry.setValue(name.toString(), true);
		}
		Object memory = applicationMap.get(YMLModelPCFConstants.KEY_MEMORY);
		if(memory!=null){
			memoryEntry.setValue(memory.toString(),true);
		}
		Object timeout = applicationMap.get(YMLModelPCFConstants.KEY_TIMEOUT);
		if(timeout != null){
			timeoutEntry.setValue(timeout.toString(),true);
		}
		Object buildPack = applicationMap.get(YMLModelPCFConstants.KEY_BUILDPACK);
		if(buildPack!=null){
			buildPackEntry.setValue(buildPack.toString(), true);
		}
	}

	private void clear() {
		nameEntry.setValue("", true); //$NON-NLS-1$
		memoryEntry.setValue("", true); //$NON-NLS-1$
		timeoutEntry.setValue("", true); //$NON-NLS-1$
		buildPackEntry.setValue("" , true); //$NON-NLS-1$
	}
	
	private void setEnabled(boolean isEnabled){
		nameEntry.setEditable(isEnabled);
		timeoutEntry.setEditable(isEnabled);
		buildPackEntry.setEditable(isEnabled);
		memoryEntry.setEditable(isEnabled);
	}
}
