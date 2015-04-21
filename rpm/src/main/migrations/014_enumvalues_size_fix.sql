-- Target versions: pre v2.8
-- SHOULD be executed when upgrading to SlipStream v2.8

alter table MODULEPARAMETER alter column ENUMVALUES varbinary(65536);
alter table NODEPARAMETER alter column ENUMVALUES varbinary(65536);
alter table RUNPARAMETER alter column ENUMVALUES varbinary(65536);
alter table SERVICECATALOGPARAMETER alter column ENUMVALUES varbinary(65536);
alter table SERVICECONFIGURATIONPARAMETER alter column ENUMVALUES varbinary(65536);
alter table USERPARAMETER alter column ENUMVALUES varbinary(65536);
