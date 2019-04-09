/*Copyright © 2019. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.group.variables.reset;

import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Variable;

public class TableColumnObject {
	
	private Variable variable;
	private Scope scope;
	private Process process;
	
	public TableColumnObject(Variable variable, Scope scope, Process process) {
		this.variable = variable;
		this.scope = scope;
		this.process = process;
	}

	public Variable getVariable() {
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}

	public Scope getScope() {
		return scope;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}
}
