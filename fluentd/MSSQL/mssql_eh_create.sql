--
-- Table structure for table activityloggingstats
--
if not exists (select * from sysobjects where name='activityloggingstats' and xtype='U')
CREATE TABLE activityloggingstats(
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
	activityinput varchar(max) NULL,
	activityoutput varchar(max) NULL,
	activityexecutionid varchar(255) NULL,
	PRIMARY KEY (activityinstanceuid)
) 

--
-- Table structure for table processinstanceloggingstats
--
if not exists (select * from sysobjects where name='processinstanceloggingstats' and xtype='U')
CREATE TABLE processinstanceloggingstats(
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
) 

--
-- Table structure for table transitionloggingstats
--
if not exists (select * from sysobjects where name='transitionloggingstats' and xtype='U')
CREATE TABLE transitionloggingstats(
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
) 
