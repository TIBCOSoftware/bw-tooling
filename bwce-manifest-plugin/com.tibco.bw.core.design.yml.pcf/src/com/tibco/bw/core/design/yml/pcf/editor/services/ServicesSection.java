/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.core.design.yml.pcf.editor.services;

import java.text.MessageFormat;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
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
public class ServicesSection extends PDESection {

	private Composite sectionContainer;
	private TableViewer tableViewer;
	protected YMLModelPCFWrapper wrapper;
	private YMLEditingModel editingModel;
	private Button deleteBtn;
	private Button addBtn;
	private Button chooseBtn;

	public ServicesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.NO_TITLE_FOCUS_BOX);
		initializeModels();
		createClient(getSection(), page.getManagedForm().getToolkit());
		updateSectionTitle();
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		// TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB,
		// TableWrapData.FILL_GRAB);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		sectionContainer = toolkit.createComposite(section);
		GridLayout createSectionClientGridLayout = FormLayoutFactory
				.createSectionClientGridLayout(false, 2);
		sectionContainer.setLayout(createSectionClientGridLayout);
		createServicesTableViewer(sectionContainer);
		section.setClient(sectionContainer);
		editingModel.addModelChangedListener(this);
		setEnabled(!editingModel.doesYamlFileHaveErrors());
	}

	private void createServicesTableViewer(Composite sectionContainer2) {
		int style = SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
				| SWT.FULL_SELECTION;
		tableViewer = new TableViewer(sectionContainer2, style);
		createColumns(tableViewer);
		// define layout for the viewer
		GridData gridData = getGridDataForTable();

		Table table = tableViewer.getTable();
		table.setLayoutData(gridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		Composite btnComposite = new DynamicComposite(sectionContainer2,
				SWT.NONE);
		GridLayout btnLayout = new GridLayout(1, false);
		btnComposite.setLayout(btnLayout);
		btnComposite.setLayoutData(getGridDataForButtons());

		createAddButton(btnComposite);
		createDeleteButton(btnComposite);

		createChooseButton(btnComposite);

		tableViewer.setContentProvider(new ServicesContentProvider());

		setTableViewerInput();
	}

	private void setTableViewerInput() {
		try {
			List<String> servicesList = wrapper.getServicesList();
			tableViewer.setInput(servicesList);
		} catch (IllegalFormatException ex) {
			Logger.getLogger(getClass().getName()).severe(ex.getMessage());
			setEnabled(false);
		}
	}

	private void createAddButton(Composite sectionContainer2) {
		addBtn = new Button(sectionContainer2, SWT.PUSH);
		addBtn.setImage(ImageProvider.getAddImage());
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					List<String> servicesList = wrapper.getServicesList();
					servicesList.add(""); //$NON-NLS-1$
					wrapper.putServicesList(servicesList);
					fireModelDirty();

				} catch (IllegalFormatException ex) {
					Logger.getLogger(getClass().getName()).severe(
							ex.getMessage());
					setEnabled(false);
					return;
				}
			}
		});
		addBtn.setToolTipText(Messages.ServicesConfigurationSection_addBtnTooltip);
	}

	private void createDeleteButton(Composite sectionContainer2) {
		deleteBtn = new Button(sectionContainer2, SWT.PUSH);
		deleteBtn.setImage(ImageProvider.getDeleteImage());
		deleteBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int selectionCount = tableViewer.getTable().getSelectionCount();
				if (selectionCount > 0) {
					TableItem[] selections = tableViewer.getTable()
							.getSelection();
					for (TableItem tableItem : selections) {
						String data = tableItem.getData().toString();

						try {
							List<String> servicesList = wrapper
									.getServicesList();
							servicesList.remove(data);
							wrapper.putServicesList(servicesList);
						} catch (IllegalFormatException ex) {
							Logger.getLogger(getClass().getName()).severe(
									ex.getMessage());
							setEnabled(false);
						}
					}

					fireModelDirty();
				}
				;
			}
		});

		deleteBtn
				.setToolTipText(Messages.ServicesConfigurationSection_deleteBtnTooltip);
	}

	private Button createChooseButton(Composite parent) {
		try {
			Class.forName(
					Messages.Container_Dialog_Class)
					.getPackage().isSealed();
		} catch (ClassNotFoundException e) {
			Logger.getLogger(ServicesSection.class.getName()).info(Messages.Container_Dialog_Class + "not Found");;
			return chooseBtn;
		}

		chooseBtn = new Button(parent, SWT.PUSH);
		chooseBtn.setImage(ImageProvider.getChooseImage());
		chooseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ServicesSelectionDialog dialog = new ServicesSelectionDialog(
						parent.getShell(), ServicesSection.this);
				dialog.open();
			}
		});

		GridData gridData = getGridDataForButtons();
		chooseBtn.setLayoutData(gridData);
		chooseBtn.setToolTipText(Messages.ServicesSection_chooseBtnTooltip);

		return chooseBtn;
	}

	protected void fireModelDirty() {
		fireSaveNeeded();
		editingModel.setDirty(true);
		setTableViewerInput();
		tableViewer.refresh();
		updateSectionTitle();
	}

	@Override
	public void commit(boolean onSave) {
		wrapper.clearApplications();
		wrapper.cleanEmptyServicesValues();
		wrapper.clearEnvMap();
		tableViewer.setInput(wrapper.getServicesList());
		wrapper.cleanEmptyServicesValues();
		tableViewer.refresh();
		updateSectionTitle();
		super.commit(onSave);
	}

	private GridData getGridDataForButtons() {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.TOP;
		gridData.verticalAlignment = SWT.TOP;
		return gridData;
	}

	private GridData getGridDataForTable() {
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		return gridData;
	}

	private void createColumns(TableViewer tableViewer2) {
		TableViewerColumn col = createTableViewerColumn(
				Messages.ServicesConfigurationSection_colTitle, 200, 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return element.toString();
			}
		});
		col.setEditingSupport(new ServicesEditingSupport(tableViewer,
				editingModel, wrapper, this));
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound,
			final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				tableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	private void initializeModels() {
		InputContext context = getContext();
		if (context == null)
			return;

		editingModel = (YMLEditingModel) context.getModel();
		wrapper = new YMLModelPCFWrapper(editingModel.getModel(), editingModel);
	}

	private InputContext getContext() {
		PDEFormEditor pdeEditor = getPage().getPDEEditor();
		InputContextManager contextManager = pdeEditor.getContextManager();
		InputContext context = contextManager
				.findContext(YMLInputContext.CONTEXT_ID);
		return context;
	}

	private class ServicesContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List<?>) {
				return ((List<?>) inputElement).toArray();
			}
			return new Object[0]; // TODO - Test this code path
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		setEnabled(!editingModel.doesYamlFileHaveErrors());
		wrapper.setModel(editingModel.getModel());
		setTableViewerInput();
		tableViewer.refresh();
		super.modelChanged(e);
	}

	private void updateSectionTitle() {
		int itemCount = tableViewer.getTable().getItemCount();
		String text = MessageFormat
				.format("{0} ({1})", Messages.ServicesConfigurationSection_colTitle, itemCount); //$NON-NLS-1$
		getSection().setText(text);
	}

	private void setEnabled(boolean isEnabled) {
		deleteBtn.setEnabled(isEnabled);
		addBtn.setEnabled(isEnabled);
		if (chooseBtn != null)
			chooseBtn.setEnabled(isEnabled);
		updateSectionTitle();
	}
}
