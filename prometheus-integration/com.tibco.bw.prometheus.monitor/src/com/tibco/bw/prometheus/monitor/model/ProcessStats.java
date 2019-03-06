/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProcessStats implements Serializable{

	private static final long serialVersionUID = 8962106137238908327L;
	
	private String ApplicationName;
	private String ApplicationVersion;
	private String ModuleName;
	private String ModuleVersion;
	private String ComponentProcessName;
	private String JobId;
	private String ParentProcessName;
	private String ParentProcessInstanceId;
	private String ProcessName;
	private String ProcessInstanceId;
	private String ProcessInstanceStartTime;
	private String ProcessInstanceEndTime;
	private Long ProcessInstanceDurationTime;
	private String ProcessInstanceState;
	private String DomainName;
	private String AppspaceName;
	private String AppnodeName;
	private String ActivityExecutionId;

	public String getApplicationName() {
		return ApplicationName;
	}

	public void setApplicationName(String applicationName) {
		ApplicationName = applicationName;
	}

	public String getApplicationVersion() {
		return ApplicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		ApplicationVersion = applicationVersion;
	}

	public String getModuleName() {
		return ModuleName;
	}

	public void setModuleName(String moduleName) {
		ModuleName = moduleName;
	}

	public String getModuleVersion() {
		return ModuleVersion;
	}

	public void setModuleVersion(String moduleVersion) {
		ModuleVersion = moduleVersion;
	}

	public String getComponentProcessName() {
		return ComponentProcessName;
	}

	public void setComponentProcessName(String componentProcessName) {
		ComponentProcessName = componentProcessName;
	}

	public String getJobId() {
		return JobId;
	}

	public void setJobId(String jobId) {
		JobId = jobId;
	}

	public String getParentProcessName() {
		return ParentProcessName;
	}

	public void setParentProcessName(String parentProcessName) {
		ParentProcessName = parentProcessName;
	}

	public String getParentProcessInstanceId() {
		return ParentProcessInstanceId;
	}

	public void setParentProcessInstanceId(String parentProcessInstanceId) {
		ParentProcessInstanceId = parentProcessInstanceId;
	}

	public String getProcessName() {
		return ProcessName;
	}

	public void setProcessName(String processName) {
		ProcessName = processName;
	}

	public String getProcessInstanceId() {
		return ProcessInstanceId;
	}

	public void setProcessInstanceId(String processInstanceId) {
		ProcessInstanceId = processInstanceId;
	}

	public String getProcessInstanceStartTime() {
		return ProcessInstanceStartTime;
	}

	public void setProcessInstanceStartTime(String processInstanceStartTime) {
		ProcessInstanceStartTime = processInstanceStartTime;
	}

	public String getProcessInstanceEndTime() {
		return ProcessInstanceEndTime;
	}

	public void setProcessInstanceEndTime(String processInstanceEndTime) {
		ProcessInstanceEndTime = processInstanceEndTime;
	}

	public Long getProcessInstanceDurationTime() {
		return ProcessInstanceDurationTime;
	}

	public void setProcessInstanceDurationTime(Long processInstanceDurationTime) {
		ProcessInstanceDurationTime = processInstanceDurationTime;
	}

	public String getProcessInstanceState() {
		return ProcessInstanceState;
	}

	public void setProcessInstanceState(String processInstanceState) {
		ProcessInstanceState = processInstanceState;
	}

	public String getDomainName() {
		return DomainName;
	}

	public void setDomainName(String domainName) {
		DomainName = domainName;
	}

	public String getAppspaceName() {
		return AppspaceName;
	}

	public void setAppspaceName(String appspaceName) {
		AppspaceName = appspaceName;
	}

	public String getAppnodeName() {
		return AppnodeName;
	}

	public void setAppnodeName(String appnodeName) {
		AppnodeName = appnodeName;
	}

	public String getActivityExecutionId() {
		return ActivityExecutionId;
	}

	public void setActivityExecutionId(String activityExecutionId) {
		ActivityExecutionId = activityExecutionId;
	}

	@Override
	public String toString() {
		return "ProcessStats [ApplicationName=" + ApplicationName
				+ ", ApplicationVersion=" + ApplicationVersion
				+ ", ModuleName=" + ModuleName + ", ModuleVersion="
				+ ModuleVersion + ", ComponentProcessName="
				+ ComponentProcessName + ", JobId=" + JobId
				+ ", ParentProcessName=" + ParentProcessName
				+ ", ParentProcessInstanceId=" + ParentProcessInstanceId
				+ ", ProcessName=" + ProcessName + ", ProcessInstanceId="
				+ ProcessInstanceId + ", ProcessInstanceStartTime="
				+ ProcessInstanceStartTime + ", ProcessInstanceEndTime="
				+ ProcessInstanceEndTime + ", ProcessInstanceDurationTime="
				+ ProcessInstanceDurationTime + ", ProcessInstanceState="
				+ ProcessInstanceState + ", DomainName=" + DomainName
				+ ", AppspaceName=" + AppspaceName + ", AppnodeName="
				+ AppnodeName + ", ActivityExecutionId=" + ActivityExecutionId
				+ "]";
	}
	
	public static List<String> getProcessStatsKeyList() {
		List<String> lskeys = new ArrayList<>();
		lskeys.add("ApplicationName");
		lskeys.add("ApplicationVersion");
		lskeys.add("ModuleName");
		lskeys.add("ModuleVersion");
		lskeys.add("ComponentProcessName");
		lskeys.add("JobId");
		lskeys.add("ParentProcessName");
		lskeys.add("ParentProcessInstanceId");
		lskeys.add("ProcessName");
		lskeys.add("ProcessInstanceId");
		lskeys.add("ProcessInstanceStartTime");
		lskeys.add("ProcessInstanceEndTime");
		lskeys.add("ProcessInstanceDurationTime");
		lskeys.add("ProcessInstanceState");
		lskeys.add("DomainName");
		lskeys.add("AppspaceName");
		lskeys.add("AppnodeName");
		lskeys.add("ActivityExecutionId");
		return lskeys;
	}
	
	public List<String> getProcessStatsValueList() {
		List<String> lsvalues = new ArrayList<>();
		lsvalues.add(this.ApplicationName);
		lsvalues.add(this.ApplicationVersion);
		lsvalues.add(this.ModuleName);
		lsvalues.add(this.ModuleVersion);
		lsvalues.add(this.ComponentProcessName);
		lsvalues.add(this.JobId);
		lsvalues.add(this.ParentProcessName);
		lsvalues.add(this.ParentProcessInstanceId);
		lsvalues.add(this.ProcessName);
		lsvalues.add(this.ProcessInstanceId);
		lsvalues.add(this.ProcessInstanceStartTime);
		lsvalues.add(this.ProcessInstanceEndTime);
		lsvalues.add(this.ProcessInstanceDurationTime.toString());
		lsvalues.add(this.ProcessInstanceState);
		lsvalues.add(this.DomainName);
		lsvalues.add(this.AppspaceName);
		lsvalues.add(this.AppnodeName);
		lsvalues.add(this.ActivityExecutionId);
		return lsvalues;
	}
	
	public static List<String> getProcessCounterKeyList() {
		List<String> lskeys = new ArrayList<>();
		lskeys.add("ApplicationName");
		lskeys.add("ProcessName");
		lskeys.add("ProcessInstanceId");
		lskeys.add("ProcessInstanceTime");
		lskeys.add("ProcessInstanceState");
		return lskeys;
	}
	
	public List<String> getProcessCounterValueList() {
		List<String> lsvalues = new ArrayList<>();
		lsvalues.add(this.ApplicationName);
		lsvalues.add(this.ProcessName);
		lsvalues.add(this.ProcessInstanceId);
		lsvalues.add(this.ProcessInstanceStartTime);
		lsvalues.add(this.ProcessInstanceState);
		return lsvalues;
	}

}
