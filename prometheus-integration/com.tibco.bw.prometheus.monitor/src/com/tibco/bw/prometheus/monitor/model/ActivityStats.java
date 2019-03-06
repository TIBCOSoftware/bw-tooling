/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActivityStats implements Serializable {

	private static final long serialVersionUID = -350203686385672509L;
	private String ApplicationName;
	private String ApplicationVersion;
	private String ModuleName;
	private String ModuleVersion;
	private String ActivityName;
	private String ProcessName;
	private String ProcessInstanceId;
	private String ActivityStartTime;
	private String ActivityEndTime;
	private Long ActivityDurationTime;
	private Long ActivityEvalTime;
	private String ActivityState;
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

	public String getActivityName() {
		return ActivityName;
	}

	public void setActivityName(String activityName) {
		ActivityName = activityName;
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

	public String getActivityStartTime() {
		return ActivityStartTime;
	}

	public void setActivityStartTime(String activityStartTime) {
		ActivityStartTime = activityStartTime;
	}

	public String getActivityEndTime() {
		return ActivityEndTime;
	}

	public void setActivityEndTime(String activityEndTime) {
		ActivityEndTime = activityEndTime;
	}

	public Long getActivityDurationTime() {
		return ActivityDurationTime;
	}

	public void setActivityDurationTime(Long activityDurationTime) {
		ActivityDurationTime = activityDurationTime;
	}

	public Long getActivityEvalTime() {
		return ActivityEvalTime;
	}

	public void setActivityEvalTime(Long activityEvalTime) {
		ActivityEvalTime = activityEvalTime;
	}

	public String getActivityState() {
		return ActivityState;
	}

	public void setActivityState(String activityState) {
		ActivityState = activityState;
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
		return "ActivityStats [ApplicationName=" + ApplicationName
				+ ", ApplicationVersion=" + ApplicationVersion
				+ ", ModuleName=" + ModuleName + ", ModuleVersion="
				+ ModuleVersion + ", ActivityName=" + ActivityName
				+ ", ProcessName=" + ProcessName + ", ProcessInstanceId="
				+ ProcessInstanceId + ", ActivityStartTime="
				+ ActivityStartTime + ", ActivityEndTime=" + ActivityEndTime
				+ ", ActivityDurationTime=" + ActivityDurationTime
				+ ", ActivityEvalTime=" + ActivityEvalTime + ", ActivityState="
				+ ActivityState + ", DomainName=" + DomainName
				+ ", AppspaceName=" + AppspaceName + ", AppnodeName="
				+ AppnodeName + ", ActivityExecutionId=" + ActivityExecutionId
				+ "]";
	}
	
	public static List<String> getActivityStatsKeyList() {
		List<String> lskeys = new ArrayList<>();
		lskeys.add("ApplicationName");
		lskeys.add("ApplicationVersion");
		lskeys.add("ModuleName");
		lskeys.add("ModuleVersion");
		lskeys.add("ActivityName");
		lskeys.add("ProcessName");
		lskeys.add("ProcessInstanceId");
		lskeys.add("ActivityStartTime");
		lskeys.add("ActivityEndTime");
		lskeys.add("ActivityDurationTime");
		lskeys.add("ActivityEvalTime");
		lskeys.add("ActivityState");
		lskeys.add("DomainName");
		lskeys.add("AppspaceName");
		lskeys.add("AppnodeName");
		lskeys.add("ActivityExecutionId");
		return lskeys;
	}
	
	public List<String> getActivityStatsValueList() {
		List<String> lsvalues = new ArrayList<>();
		lsvalues.add(this.ApplicationName);
		lsvalues.add(this.ApplicationVersion);
		lsvalues.add(this.ModuleName);
		lsvalues.add(this.ModuleVersion);
		lsvalues.add(this.ActivityName);
		lsvalues.add(this.ProcessName);
		lsvalues.add(this.ProcessInstanceId);
		lsvalues.add(this.ActivityStartTime);
		lsvalues.add(this.ActivityEndTime);
		lsvalues.add(this.ActivityDurationTime.toString());
		lsvalues.add(this.ActivityEvalTime.toString());
		lsvalues.add(this.ActivityState);
		lsvalues.add(this.DomainName);
		lsvalues.add(this.AppspaceName);
		lsvalues.add(this.AppnodeName);
		lsvalues.add(this.ActivityExecutionId);
		return lsvalues;
	}
	
	public static List<String> getActivityCounterKeyList() {
		List<String> lskeys = new ArrayList<>();
		lskeys.add("ApplicationName");
		lskeys.add("ActivityName");
		lskeys.add("ActivityTime");
		lskeys.add("ActivityState");
		lskeys.add("ProcessInstanceId");
		return lskeys;
	}
	
	public List<String> getActivityCounterValueList() {
		List<String> lsvalues = new ArrayList<>();
		lsvalues.add(this.ApplicationName);
		lsvalues.add(this.ActivityName);
		lsvalues.add(this.ActivityStartTime);
		lsvalues.add(this.ActivityState);
		lsvalues.add(this.ProcessInstanceId);
		return lsvalues;
	}
	
}
