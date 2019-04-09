/*Copyright © 2019. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.group.variables.reset;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

import com.tibco.bw.core.design.group.variables.Messages;

public class ResetVariablesSelectionDialog extends SelectionStatusDialog implements ISelectionProvider {

	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
	protected static final int DEFAULT_WIDTH = 320;
	protected static final int DEFAULT_HEIGHT = 250;

	protected ISelectionValidator selectionValidator;
	protected ViewerFilter viewerFilter;
	protected IStructuredContentProvider contentProvider;
	protected ITableLabelProvider labelProvider;
	protected Object input;

	protected Label messageLabel;
	protected TableViewer tableViewer;
	protected Label statusMessage;

	protected ListenerList selectionChangedListeners = new ListenerList();
	protected ISelection initialSelection;
	protected ISelection selection;

	protected int style;
	protected int widthHint;
	protected int heightHint;

	protected boolean isHeaderVisible = false;
	protected boolean isLinesVisible = false;
	protected TableColumnInfo[] columnInfos = new TableColumnInfo[0];
	protected boolean doubleClickToClose = false;

	public ResetVariablesSelectionDialog(Shell shell, String title, String message, int style) {
		super(shell);
		setTitle(title);
		setMessage(message);

		this.style = style;
		this.widthHint = DEFAULT_WIDTH;
		this.heightHint = DEFAULT_HEIGHT;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
	}

	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);

		doValidate(false);

		return control;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite mainComposite = (Composite) super.createDialogArea(parent);

		// 1-1
		this.messageLabel = new Label(mainComposite, SWT.WRAP);
		this.messageLabel.setText(getMessage());

		// 2-1
		this.tableViewer = createTableViewer(mainComposite);
		this.tableViewer.getTable().setFocus();

		// 3-1
		this.statusMessage = new Label(mainComposite, SWT.WRAP);

		// layout
		GridData gd11 = new GridData(GridData.FILL_HORIZONTAL);
		this.messageLabel.setLayoutData(gd11);

		GridData gd21 = new GridData(GridData.FILL_BOTH);
		gd21.widthHint = widthHint;
		gd21.heightHint = heightHint;
		this.tableViewer.getTable().setLayoutData(gd21);

		GridData gd31 = new GridData(GridData.FILL_HORIZONTAL);
		this.statusMessage.setLayoutData(gd31);

		return mainComposite;
	}

	public Label getMessageLabel() {
		return this.messageLabel;
	}

	public TableViewer getTableViewer() {
		return this.tableViewer;
	}

	public Label getStatusMessage() {
		return this.statusMessage;
	}

	/**
	 * Create a TableViewer for this dialog.
	 * 
	 * @param parent
	 *            parent Composite
	 * @return
	 */
	protected TableViewer createTableViewer(Composite parent) {
		final TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | this.style);
		tableViewer.setUseHashlookup(true);

		tableViewer.getTable().setHeaderVisible(this.isHeaderVisible);
		tableViewer.getTable().setLinesVisible(this.isLinesVisible);

		if (this.columnInfos != null) {
			for (TableColumnInfo columnInfo : columnInfos) {
				TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
				TableColumn column = viewerColumn.getColumn();
				column.setText(columnInfo.getColumnName());
				column.setWidth(columnInfo.getColumnWidth());
			}
		}

		ViewerFilter viewerFilter = getFilter();
		if (viewerFilter != null) {
			tableViewer.addFilter(viewerFilter);
		}

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (isDoubleClickToClose()) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					ResetVariablesSelectionDialog.this.selection = selection;

					boolean isValid = doValidate(true);
					if (isValid) {
						// close the dialog and return the selection result
						ResetVariablesSelectionDialog.this.okPressed();
					}
				}
			}
		});

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				ResetVariablesSelectionDialog.this.selection = selection;

				doValidate(true);

				fireSelectionChanged(new SelectionChangedEvent(ResetVariablesSelectionDialog.this, getSelection()));
			}
		});

		tableViewer.setContentProvider(getContentProvider());
		tableViewer.setLabelProvider(getLabelProvider());
		tableViewer.setInput(getInput());

		ISelection initialSelection = getInitialSelection();
		if (initialSelection != null) {
			setSelection(initialSelection);
		}

		return tableViewer;
	}

	protected boolean doValidate(boolean updateStatusMessage) {
		boolean isValid = false;
		if (selectionValidator != null && statusMessage != null) {
			String errorMsg = selectionValidator.isValid(getSelection());
			isValid = (errorMsg == null || errorMsg.trim().isEmpty()) ? true : false;

			if (isValid) {
				if (updateStatusMessage) {
					statusMessage.setText(EMPTY_STRING);
				}
				getOkButton().setEnabled(true);

			} else {
				if (updateStatusMessage) {
					statusMessage.setText(errorMsg);
				}
				getOkButton().setEnabled(false);
			}
		}
		return isValid;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		this.selectionChangedListeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		return this.selection;
	}

	@Override
	public void removeSelectionChangedListener (ISelectionChangedListener listener) {
		this.selectionChangedListeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		this.selection = selection;
		if (this.tableViewer != null && this.selection instanceof IStructuredSelection) {
			this.tableViewer.setSelection(this.selection, true);
		}
	}

	/**
	 * This method is called by okPressed() method.
	 */
	@Override
	protected void computeResult() {
		List<Object> objectsToReturn = new ArrayList<Object>();

		ISelection selection = getSelection();
		if (selection instanceof IStructuredSelection) {
			Object[] selectedObjects = ((IStructuredSelection) selection).toArray();
			if (selectedObjects != null) {
				for (Object selectedElement : selectedObjects) {
					objectsToReturn.add(selectedElement);
				}
			}
		}

		setResult(objectsToReturn);
	}
	
	public boolean isDoubleClickToClose() {
		return this.doubleClickToClose;
	}

	public void setDoubleClickToClose(boolean doubleClickToClose) {
		this.doubleClickToClose = doubleClickToClose;
	}

	public boolean isHeaderVisible() {
		return isHeaderVisible;
	}

	public void setHeaderVisible(boolean isHeaderVisible) {
		this.isHeaderVisible = isHeaderVisible;
	}

	public boolean isLinesVisible() {
		return isLinesVisible;
	}

	public void setLinesVisible(boolean isLinesVisible) {
		this.isLinesVisible = isLinesVisible;
	}

	public TableColumnInfo[] getColumnInfos() {
		return columnInfos;
	}

	public void setColumnInfos(TableColumnInfo[] columnInfos) {
		this.columnInfos = columnInfos;
	}

	public int getWidthHint() {
		return widthHint;
	}

	public void setWidthHint(int widthHint) {
		this.widthHint = widthHint;
	}

	public int getHeightHint() {
		return heightHint;
	}

	public void setHeightHint(int heightHint) {
		this.heightHint = heightHint;
	}

	/**
	 * @param selectionValidator
	 */
	public void setSelectionValidator(ISelectionValidator selectionValidator) {
		this.selectionValidator = selectionValidator;
	}

	public ISelectionValidator getSelectionValidator() {
		return this.selectionValidator;
	}

	/**
	 * 
	 * @param viewerFilter
	 */
	public void setFilter(ViewerFilter viewerFilter) {
		this.viewerFilter = viewerFilter;
	}

	public ViewerFilter getFilter() {
		return this.viewerFilter;
	}

	public IStructuredContentProvider getContentProvider() {
		return this.contentProvider;
	}

	public void setContentProvider(IStructuredContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	public ITableLabelProvider getLabelProvider() {
		return this.labelProvider;
	}

	public void setLabelProvider(ITableLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}

	protected void checkProviders() {
		if (this.contentProvider == null) {
			throw new RuntimeException(Messages.Content_Provider_Error);
		}
		if (this.labelProvider == null) {
			throw new RuntimeException(Messages.Label_Provider_Error);
		}
	}

	public Object getInput() {
		return this.input;
	}

	/**
	 * 
	 * @param input
	 */
	public void setInput(Object input) {
		checkProviders();
		this.input = input;
	}

	protected void checkInput() {
		if (this.input == null) {
			throw new RuntimeException(Messages.Input_Error);
		}
	}

	public ISelection getInitialSelection() {
		return initialSelection;
	}

	public void setInitialSelection(ISelection initialSelection) {
		this.initialSelection = initialSelection;
	}

	protected void fireSelectionChanged(final SelectionChangedEvent event) {
		for(Object listner : this.selectionChangedListeners.getListeners()) {
			SafeRunnable.run(new SafeRunnable() {
				public void run() {
					((ISelectionChangedListener) listner).selectionChanged(event);
				}
			});
		}
	}

}
