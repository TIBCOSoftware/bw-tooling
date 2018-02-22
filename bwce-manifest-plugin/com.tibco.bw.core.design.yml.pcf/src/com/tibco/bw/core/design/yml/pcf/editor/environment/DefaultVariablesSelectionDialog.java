package com.tibco.bw.core.design.yml.pcf.editor.environment;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.tibco.bw.core.design.yml.pcf.Messages;
import com.tibco.bw.core.design.yml.editor.ImageProvider;
import com.tibco.bw.core.design.yml.model.YMLModel;
import com.tibco.bw.core.design.yml.pcf.YMLModelPCFConstants;

public class DefaultVariablesSelectionDialog extends TrayDialog{

	private TableViewer tableViewer;
	private YMLModel model;
	private Properties defaultProperties;
	private Label tableFooterLabel;
	private Map<String, Object> filteredProperties;
	private EnvironmentVariablesSection section;
	
	public DefaultVariablesSelectionDialog(Shell shell,
			EnvironmentVariablesSection section) {
		super(shell);
		this.model = section.getYMLModel().getModel();
		this.defaultProperties = section.properties;
		this.filteredProperties = getUnusedPropertiesFrom(section.properties);
		this.section = section;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.EnvironmentVariablesSelectionDialog_0);
		newShell.setSize(new Point(600,400));
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite topComposite = new Composite(parent,SWT.NONE );
		topComposite.setLayout(new GridLayout(1, false));
		topComposite.setLayoutData(getGridDataForTopComposite());
		
		//This label is created only as a spacer label for UI alignment.
		new Label(topComposite, SWT.NONE);
		
		Label label = new Label(topComposite, SWT.NONE);
		label.setText(Messages.EnvironmentVariablesSelectionDialog_1);
		GridData gridData = getGridDataforLabel();
        label.setLayoutData(gridData);
        
		Composite dialogCompsite = new Composite(parent, SWT.NONE);
		dialogCompsite.setLayout(new GridLayout(3, false));
		dialogCompsite.setLayoutData(getGridDataForTableComposite());
		createEnvTableViewer(dialogCompsite);
		
		tableFooterLabel = new Label(dialogCompsite, SWT.NONE);
		String labelText = getDefaultLabelText();
		tableFooterLabel.setText(labelText);
		tableFooterLabel.setLayoutData(getGridDataforLabel());

		createSelectAllButton(dialogCompsite);
		createDeSelectAllButton(dialogCompsite);
		
		return super.createDialogArea(parent);
	}

	private String getDefaultLabelText() {
		return filteredProperties.size() + Messages.EnvironmentVariablesSelectionDialog_2;
	}

	@Override
	protected void okPressed() {
		//First fetch the model that we are interested in 
		Map<String,Object> envMap = (Map<String,Object>) model.getOthers().get(YMLModelPCFConstants.KEY_ENV);
		if(envMap == null){
			envMap = new LinkedHashMap<String, Object>();
			model.getOthers().put(YMLModelPCFConstants.KEY_ENV, envMap);
		}
		
		//If any properties are selected, then add them to the model.
		int selectionCount = tableViewer.getTable().getSelectionCount();
		if(selectionCount > 0){
			TableItem[] selection = tableViewer.getTable().getSelection();
			for (TableItem tableItem : selection) {
				Entry<String,Object> data = (Entry<String,Object>) tableItem.getData();
				envMap.put(data.getKey(), data.getValue());
			}
			
			section.fireModelDirty();
		}
		
		super.okPressed();
	}
	
	private Button createSelectAllButton(Composite sectionContainer2) {
		Button addBtn = new Button(sectionContainer2, SWT.PUSH);
		addBtn.setImage(ImageProvider.getAddImage());
		addBtn.setText(Messages.EnvironmentVariablesSelectionDialog_3);
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int itemCount = tableViewer.getTable().getItemCount();
				tableViewer.getTable().setSelection(0, itemCount-1);
				updateFooterLabelText();
			}
		});
		
		GridData gridData = getGridDataForButtons();
		addBtn.setLayoutData(gridData);
		
		return addBtn;
	}
	
	private Button createDeSelectAllButton(Composite sectionContainer2) {
		Button addBtn = new Button(sectionContainer2, SWT.PUSH);
		addBtn.setImage(ImageProvider.getSelectAllImage());
		addBtn.setText(Messages.EnvironmentVariablesSelectionDialog_4);
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tableViewer.getTable().setSelection(-1);
				updateFooterLabelText();
			}
		});
		
		GridData gridData = getGridDataForButtons();
		addBtn.setLayoutData(gridData);
		
		return addBtn;
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
		tableViewer.setInput(filteredProperties);

		//define layout for the table
        GridData gridData = getGridDataForTable();
		Table table = tableViewer.getTable();
		table.setLayoutData(gridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateFooterLabelText();
			}
		});
	}
	
	private void updateFooterLabelText() {
		int selectionCount = tableViewer.getTable().getSelectionCount();
		
		String labelText;
		if(selectionCount>0){
			String string = Messages.EnvironmentVariablesSelectionDialog_5;
			labelText = MessageFormat.format(string, selectionCount, filteredProperties.size());
		}else{
			labelText =getDefaultLabelText();
		}
		tableFooterLabel.setText(labelText);
	}
	
	private GridData getGridDataForTopComposite() {
		GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
        gridData.horizontalAlignment = GridData.FILL;
        gridData.horizontalSpan = 1;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
		return gridData;
	}
	
	private GridData getGridDataForTableComposite() {
		GridData gridData = new GridData();
	    gridData.verticalAlignment = GridData.FILL;
	    gridData.horizontalAlignment = GridData.FILL;
	    gridData.grabExcessHorizontalSpace = true;
	    gridData.grabExcessVerticalSpace = true;
		return gridData;
	}

	private GridData getGridDataForTable() {
		GridData gridData = getGridDataForTableComposite();
        gridData.heightHint = 250;
        gridData.horizontalSpan = 3;
		return gridData;
	}
	
	private GridData getGridDataForButtons() {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.RIGHT;
		gridData.verticalAlignment=GridData.VERTICAL_ALIGN_CENTER;
		gridData.grabExcessHorizontalSpace = false;
		return gridData;
	}

	private GridData getGridDataforLabel() {
		GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		return gridData;
	}

	private Map<String, Object> getUnusedPropertiesFrom(Properties properties){
		Map<String, Object> envMap = new LinkedHashMap<String, Object>();
		Set<Object> keySet = defaultProperties.keySet();
		for (Object object : keySet) {
			String keyName = (String)object;
			Map<String,Object> envMapFromModel = (Map<String,Object>) model.getOthers().get(YMLModelPCFConstants.KEY_ENV);
			if(envMapFromModel!=null && envMapFromModel.containsKey(keyName)){
				continue;
			}
			envMap.put(keyName, defaultProperties.getProperty(keyName));
		}
		
		return envMap;
	}
	
	private void createColumns(TableViewer tableViewer2) {
		String[] titles = { Messages.EnvironmentVariablesSelectionDialog_6, Messages.EnvironmentVariablesSelectionDialog_7};
        int[] bounds = { 350, 200 };
        
        // first column is for the key
        TableViewerColumn keyColumn = createTableViewerColumn(titles[0], bounds[0], 0);
        keyColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
            	final String valueName = ((Map.Entry<String, String>)element).getKey();
              return valueName;
            }
        });
        
        // second column is for the value
        TableViewerColumn valueColumn = createTableViewerColumn(titles[1], bounds[1], 1);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
            	final String valueName = ((Map.Entry<String, Object>)element).getValue().toString();
	              return valueName;
            }
        });
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
	 
	 private class TableContentProvider implements IStructuredContentProvider {

			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				Map<?, ?> map = (Map<?, ?>) inputElement;
				return map.entrySet().toArray();
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		}
}
