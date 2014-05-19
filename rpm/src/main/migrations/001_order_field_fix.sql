-- Target versions: pre v2.2.0
-- MUST be executed when upgrading to SlipStream v2.2.0 and BEFORE restarting SlipStream server

update MODULEPARAMETER set ORDER_ = 0;
alter table MODULEPARAMETER alter column ORDER_ set not null;

update NODEPARAMETER set ORDER_ = 0;
alter table NODEPARAMETER alter column ORDER_ set not null;

update RUNPARAMETER set ORDER_ = 0;
alter table RUNPARAMETER alter column ORDER_ set not null;

update SERVICECATALOGPARAMETER set ORDER_ = 0;
alter table SERVICECATALOGPARAMETER alter column ORDER_ set not null;

update SERVICECONFIGURATIONPARAMETER set ORDER_ = 0;
alter table SERVICECONFIGURATIONPARAMETER alter column ORDER_ set not null;

update USERPARAMETER set ORDER_ = 0;
alter table USERPARAMETER alter column ORDER_ set not null;
