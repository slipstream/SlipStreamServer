-- Target version: v2.2.0
-- SHOULD be executed when upgrading to SlipStream v2.2.1

alter table MODULEPARAMETER alter column VALUE varchar(65536);
alter table NODEPARAMETER alter column VALUE varchar(65536);
alter table RUNPARAMETER alter column VALUE varchar(65536);
alter table RUNTIMEPARAMETER alter column VALUE varchar(65536);
alter table SERVICECATALOGPARAMETER alter column VALUE varchar(65536);
alter table SERVICECONFIGURATIONPARAMETER alter column VALUE varchar(65536);
alter table USERPARAMETER alter column VALUE varchar(65536);
