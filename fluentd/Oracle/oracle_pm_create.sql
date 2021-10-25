--
-- Table structure for Process Monitoring
--
BEGIN
BEGIN
BEGIN

--
-- Table structure for table ActivityLoggingStats
--

  EXECUTE IMMEDIATE 'CREATE TABLE ActivityLoggingStats (
    activityInstanceUID varchar(40) NOT NULL,
    timestmp NUMBER(19,0) DEFAULT NULL,
    applicationName varchar(1000) DEFAULT NULL,
    applicationVersion varchar(1000) DEFAULT NULL,
    moduleName varchar(1000) DEFAULT NULL,
    moduleVersion varchar(1000) DEFAULT NULL,
    processName varchar(1000) DEFAULT NULL,
    processInstanceId varchar(45) DEFAULT NULL,
    activityName varchar(100) DEFAULT NULL,
    activityStartTime NUMBER(19,0) DEFAULT NULL,
    activityDurationTime NUMBER(19,0) DEFAULT NULL,
    activityEvalTime NUMBER(19,0) DEFAULT NULL,
    activityState varchar(45) DEFAULT NULL,
    domainName varchar(1000) DEFAULT NULL,
    appspaceName varchar(1000) DEFAULT NULL,
    appnodeName varchar(1000) DEFAULT NULL,
	activityInput CLOB DEFAULT NULL,
	activityOutput CLOB DEFAULT NULL,
	activityExecutionId varchar(45) DEFAULT NULL,
    PRIMARY KEY (activityInstanceUID)
)' ;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;


--
-- Table structure for table ProcessInstanceLoggingStats
--

  EXECUTE IMMEDIATE 'CREATE TABLE ProcessInstanceLoggingStats (
    processInstanceUID varchar(40) NOT NULL,
    timestmp NUMBER(19,0) DEFAULT NULL,
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
    processInstanceStartTime NUMBER(19,0) DEFAULT NULL,
    processInstanceEndTime NUMBER(19,0) DEFAULT NULL,
    processInstanceDurationTime NUMBER(19,0) DEFAULT NULL,
    processInstanceEvalTime NUMBER(19,0) DEFAULT NULL,
    processInstanceState varchar(45) DEFAULT NULL,
    domainName varchar(1000) DEFAULT NULL,
    appspaceName varchar(1000) DEFAULT NULL,
    appnodeName varchar(1000) DEFAULT NULL,
	activityExecutionId varchar(45) DEFAULT NULL,
    PRIMARY KEY (processInstanceUID)
)' ;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;

--
-- Table structure for table TransitionLoggingStats
--

  EXECUTE IMMEDIATE 'CREATE TABLE TransitionLoggingStats (
    transitionInstanceUID varchar(40) NOT NULL,
    timestmp NUMBER(19,0) DEFAULT NULL,
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
)' ;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;