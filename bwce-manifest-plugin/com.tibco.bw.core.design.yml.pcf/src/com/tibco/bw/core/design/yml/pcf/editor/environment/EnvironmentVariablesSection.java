package com.tibco.bw.core.design.yml.pcf.editor.environment;

import java.text.MessageFormat;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.tibco.bw.core.design.yml.editor.ImageProvider;
import com.tibco.bw.core.design.yml.model.YMLEditingModel;
import com.tibco.bw.core.design.yml.pcf.Messages;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFWrapper;
import com.tibco.bw.core.design.yml.pcf.editor.YMLInputContext;
import com.tibco.bw.design.field.utils.DynamicComposite;

@SuppressWarnings("restriction")
public class EnvironmentVariablesSection extends PDESection {

	private Composite sectionContainer;
	private TableViewer tableViewer;
	protected Properties properties;
	private YMLModelPCFWrapper wrapper;
	private Button chooseBtn;
	private Button deleteBtn;
	private Button addBtn;
	private YMLEditingModel editingModel;

	public EnvironmentVariablesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.NO_TITLE_FOCUS_BOX);
		initializeModels();
		createClient(getSection(), page.getManagedForm().getToolkit());
		updateSectionTitle();
		
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		
		this.properties = DefaultVariablesLoader.loadProperties();
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		sectionContainer = toolkit.createComposite(section);
		GridLayout createSectionClientGridLayout = new GridLayout(2, false);
		sectionContainer.setLayout(createSectionClientGridLayout);
		createEnvTableViewer(sectionContainer);
		Composite btnComposite = new DynamicComposite(sectionContainer, SWT.NONE);
		GridLayout btnLayout = new GridLayout(1 ,false); 
		btnComposite.setLayout(btnLayout);
		btnComposite.setLayoutData(getGridDataForButtons());
		
		createAddButton(btnComposite);
		createDeleteButton(btnComposite);
		createChooseButton(btnComposite);
		
		section.setClient(sectionContainer);
		getYMLModel().addModelChangedListener(this);
		setEnabled(!editingModel.doesYamlFileHaveErrors());
		setTableViewerInput();
	}
	
	private void initializeModels() {
		editingModel = getYMLModel();
		if (editingModel == null)
			return ;
		
		wrapper = new YMLModelPCFWrapper(editingModel.getModel(),editingModel);
	}

	/**
	 * Create TableViewer for editing Services.
	 * 
	 * @param parent
	 * @return
	 */
	protected void createEnvTableViewer(Composite parent) {
		
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		createColumns(tableViewer);
		tableViewer.setContentProvider(new TableContentProvider());

		//define layout for the table
        GridData gridData = getGridDataForTable();
		Table table = tableViewer.getTable();
		table.setLayoutData(gridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
	
	private void setTableViewerInput() {
		try {
			wrapper.setModel(editingModel.getModel());
			tableViewer.setInput(wrapper.getEnvironmentVariableMap());
		} catch (IllegalFormatException ex) {
			Logger.getLogger(getClass().getName()).severe(ex.getMessage());
			setEnabled(false);
		}
	}

	private Button createAddButton(Composite sectionContainer2) {
		addBtn = new Button(sectionContainer2, SWT.PUSH);
		addBtn.setImage(ImageProvider.getAddImage());
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					wrapper.getEnvironmentVariableMap().put("", ""); //$NON-NLS-1$ //$NON-NLS-2$
					fireModelDirty();
					tableViewer.getTable().select(tableViewer.getTable().getItemCount());
				} catch (IllegalFormatException ex) {
					Logger.getLogger(getClass().getName()).severe(
							ex.getMessage());
					setEnabled(false);
				}
			}
		});
		
		GridData gridData = getGridDataForButtons();
		addBtn.setLayoutData(gridData);
		addBtn.setToolTipText(Messages.EnvironmentConfigurationSection_addBtnTooltip);
		
		return addBtn;
	}

	private Button createDeleteButton(Composite parent) {
		deleteBtn = new Button(parent, SWT.PUSH);
		deleteBtn.setImage(ImageProvider.getDeleteImage());
		deleteBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectionCount = tableViewer.getTable().getSelectionCount();
				if(selectionCount > 0){
					TableItem[] selections = tableViewer.getTable().getSelection();
					for (TableItem tableItem : selections) {
						Entry<String,Object> data = (Entry<String,Object>) tableItem.getData();
						try {
							wrapper.getEnvironmentVariableMap().remove(data.getKey());
						} catch (IllegalFormatException ex) {
							Logger.getLogger(getClass().getName()).severe(
									ex.getMessage());
							setEnabled(false);
						}
					}
					
					fireModelDirty();
				}
			}
		});
		
		GridData gridData = getGridDataForButtons();
		deleteBtn.setLayoutData(gridData);
		
		deleteBtn.setToolTipText(Messages.EnvironmentConfigurationSection_deleteBtnTooltip);
		return deleteBtn;
	}

	
	private Button createChooseButton(Composite parent) {
		chooseBtn = new Button(parent, SWT.PUSH);
		chooseBtn.setImage(ImageProvider.getChooseImage());
		chooseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DefaultVariablesSelectionDialog dialog = new DefaultVariablesSelectionDialog(parent.getShell(), EnvironmentVariablesSection.this);
				dialog.open();
			}
		});
		
		GridData gridData = getGridDataForButtons();
		chooseBtn.setLayoutData(gridData);
		chooseBtn.setToolTipText(Messages.EnvironmentConfigurationSection_chooseBtnTooltip);
		
		return chooseBtn;
	}

	private GridData getGridDataForButtons() {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.TOP;
		gridData.verticalAlignment=SWT.TOP;
		return gridData;
	}

	private GridData getGridDataForTable() {
		GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
		return gridData;
	}

	protected void fireModelDirty() {
		editingModel.setDirty(true);
		fireSaveNeeded();
		setTableViewerInput();
		tableViewer.refresh();
		updateSectionTitle();
	}
	
	private void createColumns(TableViewer tableViewer2) {
		String[] titles = { Messages.EnvironmentConfigurationSection_envVarColTitle, Messages.EnvironmentConfigurationSection_valueColTitle};
        int[] bounds = { 200, 200 };
        
        // first column is for the key
        TableViewerColumn keyColumn = createTableViewerColumn(titles[0], bounds[0], 0);
        keyColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
            	final String valueName = ((Map.Entry<String, String>)element).getKey();
              return valueName;
            }
        });
        keyColumn.setEditingSupport(new VariableKeyEditingSupport(tableViewer,wrapper,getYMLModel(),this,properties));
        
        // second column is for the value
        TableViewerColumn valueColumn = createTableViewerColumn(titles[1], bounds[1], 1);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                
            	final String valueName = ((Map.Entry<String, Object>)element).getValue().toString();
	              return valueName;
            }
        });
        valueColumn.setEditingSupport(new VariableValueEditingSupport(tableViewer,wrapper,getYMLModel(),this));
	}
	
	 private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(true);
        return viewerColumn;
    }

	protected YMLEditingModel getYMLModel() {
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
			tableViewer.refresh();
			updateSectionTitle();
		
		super.commit(onSave);
	}
	
	private class TableContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof Map<?,?>){
				Map<?, ?> map = (Map<?, ?>) inputElement;
				return map.entrySet().toArray();
			}
			return new Object[0];
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	
	@Override
	public void modelChanged(IModelChangedEvent e) {
		setTableViewerInput();
		tableViewer.refresh();
		updateSectionTitle();
		setEnabled(!editingModel.doesYamlFileHaveErrors());
		super.modelChanged(e);
	}

	private void updateSectionTitle() {
		int itemCount = tableViewer.getTable().getItemCount();
		String text = MessageFormat.format("{0} ({1})",Messages.EnvironmentConfigurationSection_envVarColTitle, itemCount); //$NON-NLS-1$
		getSection().setText(text);
	}
	
	private void setEnabled(boolean isEnabled){
		deleteBtn.setEnabled(isEnabled);
		addBtn.setEnabled(isEnabled);
		chooseBtn.setEnabled(isEnabled);
		updateSectionTitle();
	}
}
