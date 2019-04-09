/*Copyright © 2019. TIBCO Software Inc. All Rights Reserved.*/
package com.tibco.bw.core.design.group.variables.reset;

public class TableColumnInfo {
	protected String columnName;
	protected int columnWidth;

	public TableColumnInfo() {
	}

	public TableColumnInfo(String columnName, int columnWidth) {
		this.columnName = columnName;
		this.columnWidth = columnWidth;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public int getColumnWidth() {
		return columnWidth;
	}

	public void setColumnWidth(int columnWidth) {
		this.columnWidth = columnWidth;
	}
}
