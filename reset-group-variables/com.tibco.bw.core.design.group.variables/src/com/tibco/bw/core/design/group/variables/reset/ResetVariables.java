package com.tibco.bw.core.design.group.variables.reset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Variable;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gmf.runtime.diagram.ui.editparts.GraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.IDiagramGraphicalViewer;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.tibco.bw.core.design.group.variables.Messages;
import com.tibco.bw.core.design.process.editor.diagram.edit.parts.ActivityEditPart;
import com.tibco.bw.core.design.process.editor.ext.util.BWDiagramUtil;
import com.tibco.bw.core.design.project.core.util.BWCompositeHelper;
import com.tibco.bw.core.design.resource.builder.BWGroupBuilder;
import com.tibco.bw.core.design.resource.builder.BWProcessHelper;
import com.tibco.xpd.resources.XpdResourcesPlugin;
import com.tibco.zion.common.refactoring.base.WorkingCopyHelper;

public class ResetVariables {

	private List<IProject> projects;
	private List<Variable> candidateResetVariables = new ArrayList<Variable>();
	private List<Variable> existingResetVariables = new ArrayList<Variable>();
	private List<Variable> tempCandidateResetVariables = new ArrayList<Variable>();
	private List<TableColumnObject> tempTableData = new ArrayList<TableColumnObject>();
	private List<TableColumnObject> tableData = new ArrayList<TableColumnObject>();
	private Map<Scope, List<Variable>> resetVariablesToAdd = new HashMap<Scope, List<Variable>>();
	
	public ResetVariables(List<IProject> projects) {
		this.projects = projects;
	}

	protected void performReset() {
		clearData();
		addTableData();
	}
	
	private void clearData() {
		tempTableData.clear();
		candidateResetVariables.clear();
		existingResetVariables.clear();
		tempCandidateResetVariables.clear();
		tempTableData.clear();
		resetVariablesToAdd.clear();
		tableData.clear();
	}

	private void addTableData() {
		for(IProject project : projects) {
			List<Process> processes = BWCompositeHelper.INSTANCE.getProcesses(project);
			for(Process process : processes) {
				List<Object> activities = BWProcessHelper.INSTANCE.getActivities(process, null);
				for(Object object : activities) {
					Activity activity = (Activity) object;
					if(BWProcessHelper.INSTANCE.isGroup((EObject)activity) && activity instanceof Scope) {
						tempTableData.clear();
						tempCandidateResetVariables.clear();
						resetVariablesToAdd.clear();
						Scope scope = (Scope) activity;
						candidateResetVariables = BWGroupBuilder.INSTANCE.getCandidateVariablesToReset(scope, true);
						existingResetVariables = BWGroupBuilder.INSTANCE.getResetVariables(scope);
						tempCandidateResetVariables.addAll(candidateResetVariables);
						tempCandidateResetVariables.removeAll(existingResetVariables);
						resetVariablesToAdd.put(scope, tempCandidateResetVariables);
						for(Variable variable : tempCandidateResetVariables) {
							tempTableData.add(new TableColumnObject(variable, scope, process));
						}
						tableData.addAll(tempTableData);
					}
				}
			}
		}
		invokeResetVariableSelectionDialog();
	}

	private void invokeResetVariableSelectionDialog() {
		ResetVariablesSelectionDialog dialog = new ResetVariablesSelectionDialog(Display.getDefault().getActiveShell(), Messages.Select_Variable, Messages.Variables_To_Reset, SWT.MULTI);
		dialog.setDoubleClickToClose(true);
		dialog.setHeaderVisible(true);
		dialog.setLinesVisible(false);
		dialog.setColumnInfos(new TableColumnInfo[] { new TableColumnInfo("Name", 100), new TableColumnInfo("Group", 100), new TableColumnInfo("Process", 100) }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		dialog.setSelectionValidator(new VariableSelectionValidator());
		dialog.setContentProvider(new VariableSelectionTableContentProvider());
		dialog.setLabelProvider(new VariableSelectionTableLabelProvider());
		dialog.setInput(tableData);
		if (dialog.open() == Window.OK) {
			Object[] objects = dialog.getResult();
			if (objects != null) {
				for (Object object : objects) {
					if (object instanceof TableColumnObject) {
						TableColumnObject tableColumnObject = (TableColumnObject) object;
						addResetVariables(tableColumnObject);
					}
				}
			}
		}
	}

	private void addResetVariables(TableColumnObject tableColumnObject) {
		TransactionalEditingDomain editingDomain = XpdResourcesPlugin.getDefault().getEditingDomain();
		if (editingDomain != null) {
			RecordingCommand command = new RecordingCommand(editingDomain) {
				@Override
				protected void doExecute() {
					Scope scope = tableColumnObject.getScope();
					Activity groupStart = BWProcessHelper.INSTANCE.getGroupStartActivity(scope);
					BWGroupBuilder.INSTANCE.createResetVariableCopy(scope, groupStart, getActivityView(scope), tableColumnObject.getVariable());
					WorkingCopyHelper.INSTANCE.saveWorkingCopyFor((EObject) tableColumnObject.getProcess());
				}
			};
			editingDomain.getCommandStack().execute(command);
		}
	}
	
	protected View getActivityView(Activity activity) {
		View view = null;
		if (activity != null) {
			IDiagramGraphicalViewer viewer = getGraphicalViewer();
			GraphicalEditPart editPart = BWDiagramUtil.findEditPart(viewer, ActivityEditPart.class, (EObject)activity);
			if (editPart instanceof ActivityEditPart) {
				view = ((ActivityEditPart) editPart).getNotationView();
			}
		}
		return view;
	}
	
	protected IDiagramGraphicalViewer getGraphicalViewer() {
		IDiagramGraphicalViewer viewer = null;
		return viewer;
	}
	
}