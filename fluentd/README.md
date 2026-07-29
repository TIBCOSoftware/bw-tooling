
# Fluentd Conainter deployment for TCI Hybrid Applications

For the TCI Hybrid applications the Fluentd container works as a sidecar 
for the On-prem BWCE Applications to write the Process Monitoring data 
into the Database.


## Configuring Fluentd connection to PostgreSQL Database

To establish the connection between the Fluentd and PostgreSQL Database, update the [fluentd.conf](./PostgreSQL/config/fluentd.conf) file located inside the PostgreSQL/config folder.  

In this file the following lines need to be updated with the PostgreSQL Database details:- 
- **host** :- Update the Line #12 in the file with the host address of the Machine with the Database
- **port** :- Update the Line #13 in the file with the Database port (For PostgreSQL the default port is 5432)
- **database** :- Update the Line #14 in the file with the Database name.
- **username** :- Update the Line #15 in the file with the 'username', with which the connection can be established.
- **password** :- Update the Line #16 in the file with the 'password' for the 'username' provided in Line #10.

There are some additional configuring that the user can use:-
- **port** :- The Line #3 in the fluentd.conf file contains the port number for establishing connection with the Fluentd.(By default th port '24224' will be used.)


## Configuring Fluentd connection to MySQL Database

To establish the connection between the Fluentd and MySQL Database, update the [fluentd.conf](./MySQL/config/fluentd.conf) file located inside the MySQL/config folder.  

In this file the following lines need to be updated with the MySQL Database details:- 
- **host** :- Update the Line #13 in the file with the host address of the Machine with the Database
- **port** :- Update the Line #14 in the file with the Database port (For MySQL the default port is 3306)
- **database** :- Update the Line #15 in the file with the Database name.
- **username** :- Update the Line #16 in the file with the 'username', with which the connection can be established.
- **password** :- Update the Line #17 in the file with the 'password' for the 'username' provided in Line #10.

There are some additional configuring that the user can use:-
- **port** :- The Line #3 in the fluentd.conf file contains the port number for establishing connection with the Fluentd.(By default th port '24224' will be used.)


## Configuring Fluentd connection to Microsoft SQL Server Database

To establish the connection between the Fluentd and Microsoft SQL Server Database, update the [fluentd.conf](./MSSQL/config/fluentd.conf) file located inside the MSSQL/config folder.  

In this file the following lines need to be updated with the MSSQL Database details:- 
- **host** :- Update the Line #13 in the file with the host address of the Machine with the Database
- **port** :- Update the Line #14 in the file with the Database port (For MSSQL the default port is 1433)
- **database** :- Update the Line #15 in the file with the Database name.
- **username** :- Update the Line #16 in the file with the 'username', with which the connection can be established.
- **password** :- Update the Line #17 in the file with the 'password' for the 'username' provided in Line #10.

There are some additional configuring that the user can use:-
- **port** :- The Line #3 in the fluentd.conf file contains the port number for establishing connection with the Fluentd.(By default th port '24224' will be used.)


## Configuring Fluentd connection to Oracle Database

To establish the connection between the Fluentd and Oracle Database, update the [fluentd.conf](./Oracle/config/fluentd.conf) file located inside the Oracle/config folder.  

In this file the following lines need to be updated with the MSSQL Database details:- 
- **host** :- Update the Line #21 in the file with the host address of the Machine with the Database
- **port** :- Update the Line #22 in the file with the Database port (For Oracle DB the default port is 1521)
- **database** :- Update the Line #23 in the file with the "Service Name/SID".
- **username** :- Update the Line #24 in the file with the 'username', with which the connection can be established.
- **password** :- Update the Line #25 in the file with the 'password' for the 'username' provided in Line #10.

There are some additional configuring that the user can use:-
- **port** :- The Line #3 in the fluentd.conf file contains the port number for establishing connection with the Fluentd.(By default th port '24224' will be used.)

**Note**:- As there is a [issue](https://github.com/rsim/oracle-enhanced/issues/2211) related to data insertion in Oracle DB using fluentd sql plugin for CLOB datatype. We are using varchar2 data type with a size limit of 4000 bytes. This size can be extended to 32767 bytes by performing the steps mentioned in the following [document](https://docs.oracle.com/en/database/oracle/oracle-database/18/spmsu/enabling-the-new-extended-data-type-capability.html#GUID-88FB7FFD-4392-49C6-843A-45B49F8A1821). **The steps mentioned is irreversible.** 
  - If the steps mentioned above are being used then the user can update the create table script for ActivityLoggingStats table with the enhanced size. 
  - The user will have to comment Line #9 and #10 and un-comment Line #11 and #12.
## Steps to Build Fluentd Image

Steps to build the Fluentd Docker image:-
- Step #1:- Configure the fluentd.conf file with the steps mentioned above, depending on the type of database in use.
- Step #2:- This Step is only for Oracle database.
  - Depending on the version of Oracle DB in use download the following files from the [Oracle Website](https://www.oracle.com/sg/database/technologies/instant-client/linux-x86-64-downloads.html)
    - instantclient-basic-linux.x64
    - instantclient-sdk-linux.x64
    - instantclient-sqlplus-linux.x64
  - Place these files in the Oracle/instantclient folder.
  - Update the Line #16 with the file name for instantclient-basic-linux.x64 zip file.
  - Update the Line #24 with the file name for instantclient-sqlplus-linux.x64 zip file.
  - Update the Line #32 with the file name for instantclient-sdk-linux.x64 zip file.
- To build the fluentd docker image use the following docker command.
```bash
  docker build -t fluentd .
```



## Steps to run Fluentd Image

To run fluentd container the user can use the following command.

```
  docker run -p 24224:24224 -e FLUENTD_CONF=fluentd.conf fluent
```

The port number present in the Line #3 of the fluentd.conf file must be published during execution and the same port must also be provided to the BWCE Application. By default the port number 24224 is used.

### Environment Variables
 - **FLUENTD_CONF**:- Name of the fluentd config file present as per the database in use.