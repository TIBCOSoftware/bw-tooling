--
-- Table structure for BWCE Monitoring Data
--
BEGIN
BEGIN
BEGIN
--
-- Table structure for table activityloggingstats
--
  EXECUTE IMMEDIATE 'CREATE TABLE activityloggingstats (
    activityinstanceuid varchar(255) NOT NULL,
    timestmp NUMBER(19,0) NULL,
    applicationname varchar(255) NULL,
    applicationversion varchar(255) NULL,
    modulename varchar(255) NULL,
    moduleversion varchar(255) NULL,
    processname varchar(255) NULL,
    processinstanceid varchar(255) NULL,
    activityname varchar(255) NULL,
    activitystarttime NUMBER(19,0) NULL,
    activitydurationtime NUMBER(19,0) NULL,
    activityevaltime NUMBER(19,0) NULL,
    activitystate varchar(255) NULL,
    domainname varchar(255) NULL,
    appspacename varchar(255) NULL,
    appnodename varchar(255) NULL,
    activityinput varchar(4000) NULL,
    activityoutput varchar(4000) NULL,
    activityexecutionid varchar(255) NULL,
    PRIMARY KEY (activityinstanceuid)
)' ;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
--
-- Table structure for table processinstanceloggingstats
--
  EXECUTE IMMEDIATE 'CREATE TABLE processinstanceloggingstats (
    processinstanceuid varchar(255) NOT NULL,
    timestmp NUMBER(19,0) NULL,
    applicationname varchar(255) NULL,
    applicationversion varchar(255) NULL,
    modulename varchar(255) NULL,
    moduleversion varchar(255) NULL,
    componentprocessname varchar(255) NULL,
    processinstancejobid varchar(255) NULL,
    parentprocessname varchar(255) NULL,
    parentprocessinstanceid varchar(255) NULL,
    processname varchar(255) NULL,
    processinstanceid varchar(255) NULL,
    processinstancestarttime NUMBER(19,0) NULL,
    processinstanceendtime NUMBER(19,0) NULL,
    processinstancedurationtime NUMBER(19,0) NULL,
    processinstanceevaltime NUMBER(19,0) NULL,
    processinstancestate varchar(255) NULL,
    domainname varchar(255) NULL,
    appspacename varchar(255) NULL,
    appnodename varchar(255) NULL,
    activityexecutionid varchar(255) NULL,
    PRIMARY KEY (processinstanceuid)
)' ;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
--
-- Table structure for table transitionloggingstats
--
  EXECUTE IMMEDIATE 'CREATE TABLE transitionloggingstats (
    transitioninstanceuid varchar(255) NOT NULL,
    timestmp NUMBER(19,0) NULL,
    applicationname varchar(255) NULL,
    applicationversion varchar(255) NULL,
    modulename varchar(255) NULL,
    moduleversion varchar(255) NULL,
    componentprocessname varchar(255) NULL,
    processname varchar(255) NULL,
    processinstanceid varchar(255) NULL,
    transitionname varchar(255) NULL,
    domainname varchar(255) NULL,
    appspacename varchar(255) NULL,
    appnodename varchar(255) NULL,
    PRIMARY KEY (transitioninstanceuid)
)' ;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;


/*
--
-- Table structure for table activityloggingstats
-- Use this table if the MAX_STRING_SIZE of Oracle DB is EXTENDED
--
BEGIN
  EXECUTE IMMEDIATE 'CREATE TABLE activityloggingstats (
    activityinstanceuid varchar(255) NOT NULL,
    timestmp NUMBER(19,0) NULL,
    applicationname varchar(255) NULL,
    applicationversion varchar(255) NULL,
    modulename varchar(255) NULL,
    moduleversion varchar(255) NULL,
    processname varchar(255) NULL,
    processinstanceid varchar(255) NULL,
    activityname varchar(255) NULL,
    activitystarttime NUMBER(19,0) NULL,
    activitydurationtime NUMBER(19,0) NULL,
    activityevaltime NUMBER(19,0) NULL,
    activitystate varchar(255) NULL,
    domainname varchar(255) NULL,
    appspacename varchar(255) NULL,
    appnodename varchar(255) NULL,
    activityinput varchar(32767) NULL,
    activityoutput varchar(32767) NULL,
    activityexecutionid varchar(255) NULL,
    PRIMARY KEY (activityinstanceuid)
)' ;
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -955 THEN
      RAISE;
    END IF;
END;
*/