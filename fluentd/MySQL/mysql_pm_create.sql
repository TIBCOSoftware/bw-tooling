--
-- Table structure for table activityloggingstats
--
CREATE TABLE IF NOT EXISTS activityloggingstats(
	activityinstanceuid varchar(255) NOT NULL,   -- primary key column
	timestmp bigint NULL,
	applicationname varchar(255) NULL,
	applicationversion varchar(255) NULL,
	modulename varchar(255) NULL,
	moduleversion varchar(255) NULL,
	processname varchar(255) NULL,
	processinstanceid varchar(255) NULL,
	activityname varchar(255) NULL,
	activitystarttime bigint NULL,
	activitydurationtime bigint NULL,
	activityevaltime bigint NULL,
	activitystate varchar(255) NULL,
	domainname varchar(255) NULL,
	appspacename varchar(255) NULL,
	appnodename varchar(255) NULL,
	activityinput text,
	activityoutput text,
	activityexecutionid varchar(255) NULL,
	PRIMARY KEY (activityinstanceuid)
);

--
-- Table structure for table processinstanceloggingstats
--
CREATE TABLE IF NOT EXISTS processinstanceloggingstats(
	processinstanceuid varchar(255) NOT NULL,  -- primary key column
	timestmp bigint NULL,
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
	processinstancestarttime bigint NULL,
	processinstanceendtime bigint NULL,
	processinstancedurationtime bigint NULL,
	processinstanceevaltime bigint NULL,
	processinstancestate varchar(255) NULL,
	domainname varchar(255) NULL,
	appspacename varchar(255) NULL,
	appnodename varchar(255) NULL,
	activityexecutionid varchar(255) NULL,
	PRIMARY KEY (processinstanceuid)
);

--
-- Table structure for table transitionloggingstats
--
CREATE TABLE IF NOT EXISTS transitionloggingstats(
	transitioninstanceuid varchar(255) NOT NULL,  -- primary key column
	timestmp bigint NULL,
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
);
