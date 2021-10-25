
--
-- Table structure for table ActivityLoggingStats
--

CREATE TABLE IF NOT EXISTS ActivityLoggingStats (
	activityInstanceUID varchar(40) NOT NULL,
    timestmp bigint DEFAULT NULL,
    applicationName varchar(1000) DEFAULT NULL,
    applicationVersion varchar(1000) DEFAULT NULL,
    moduleName varchar(1000) DEFAULT NULL,
    moduleVersion varchar(1000) DEFAULT NULL,
    processName varchar(1000) DEFAULT NULL,
    processInstanceId varchar(45) DEFAULT NULL,
    activityName varchar(100) DEFAULT NULL,
    activityStartTime bigint DEFAULT NULL,
    activityDurationTime bigint DEFAULT NULL,
    activityEvalTime bigint DEFAULT NULL,
    activityState varchar(45) DEFAULT NULL,
    domainName varchar(1000) DEFAULT NULL,
    appspaceName varchar(1000) DEFAULT NULL,
    appnodeName varchar(1000) DEFAULT NULL,
	activityInput text DEFAULT NULL,
	activityOutput text DEFAULT NULL,
	activityExecutionId varchar(45) DEFAULT NULL,
    PRIMARY KEY (activityInstanceUID)
);


--
-- Table structure for table ProcessInstanceLoggingStats
--

CREATE TABLE IF NOT EXISTS ProcessInstanceLoggingStats (
    processInstanceUID varchar(40) NOT NULL,
    timestmp bigint DEFAULT NULL,
    applicationName varchar(1000) DEFAULT NULL,
    applicationVersion varchar(1000) DEFAULT NULL,
    moduleName varchar(1000) DEFAULT NULL,
    moduleVersion varchar(1000) DEFAULT NULL,
    componentProcessName varchar(1000) DEFAULT NULL,
    processInstanceJobId varchar(45) DEFAULT NULL,
    parentProcessName varchar(1000) DEFAULT NULL,
    parentProcessInstanceId varchar(45) DEFAULT NULL,
    processName varchar(1000) DEFAULT NULL,
    processInstanceId varchar(45) DEFAULT NULL,
    processInstanceStartTime bigint DEFAULT NULL,
    processInstanceEndTime bigint DEFAULT NULL,
    processInstanceDurationTime bigint DEFAULT NULL,
    processInstanceEvalTime bigint DEFAULT NULL,
    processInstanceState varchar(45) DEFAULT NULL,
    domainName varchar(1000) DEFAULT NULL,
    appspaceName varchar(1000) DEFAULT NULL,
    appnodeName varchar(1000) DEFAULT NULL,
	activityExecutionId varchar(45) DEFAULT NULL,
    PRIMARY KEY (processInstanceUID)
);

--
-- Table structure for table TransitionLoggingStats
--

CREATE TABLE IF NOT EXISTS TransitionLoggingStats (
	transitionInstanceUID varchar(40) NOT NULL,
    timestmp bigint DEFAULT NULL,
    applicationName varchar(1000) DEFAULT NULL,
    applicationVersion varchar(1000) DEFAULT NULL,
    moduleName varchar(1000) DEFAULT NULL,
    moduleVersion varchar(1000) DEFAULT NULL,
    componentProcessName varchar(1000) DEFAULT NULL,
    processName varchar(1000) DEFAULT NULL,
    processInstanceId varchar(45) DEFAULT NULL,
    transitionName varchar(1000) DEFAULT NULL,
    domainName varchar(1000) DEFAULT NULL,
    appspaceName varchar(1000) DEFAULT NULL,
    appnodeName varchar(1000) DEFAULT NULL,
    PRIMARY KEY (transitionInstanceUID)
);

