-- Target versions: pre v2.2.0
-- SHOULD be executed when upgrading to SlipStream v2.2.0

alter table AUTHZ alter column GROUPMEMBERS_ varchar(1024);

alter table COMMIT alter column COMMENT varchar(1024);

alter table COOKIEKEYPAIR alter column PRIVATEKEY varchar(1024);
alter table COOKIEKEYPAIR alter column PUBLICKEY varchar(1024);

alter table MODULE alter column DESCRIPTION varchar(1024);
alter table MODULE alter column TAG varchar(1024);
alter table MODULE alter column PRERECIPE varchar(65536);
alter table MODULE alter column RECIPE varchar(65536);

alter table MODULEPARAMETER alter column DESCRIPTION varchar(1024);
alter table MODULEPARAMETER alter column VALUE varchar(1024);

alter table NODE alter column DESCRIPTION varchar(1024);

alter table NODEPARAMETER alter column DESCRIPTION varchar(1024);
alter table NODEPARAMETER alter column VALUE varchar(1024);

alter table ONESHOTACTION alter column ENCODEDFORM varchar(1024);

alter table RUN alter column DESCRIPTION varchar(1024);
alter table RUN alter column NODENAMES varchar(65536);
alter table RUN alter column CLOUDSERVICENAMES varchar(1024);

alter table RUNPARAMETER alter column DESCRIPTION varchar(1024);
alter table RUNPARAMETER alter column VALUE varchar(1024);

alter table RUNTIMEPARAMETER alter column DESCRIPTION varchar(1024);
alter table RUNTIMEPARAMETER alter column VALUE varchar(1024);
alter table RUNTIMEPARAMETER alter column MAPPEDRUNTIMEPARAMETERNAMES varchar(1024);

alter table SERVICECATALOG alter column DESCRIPTION varchar(1024);

alter table SERVICECATALOGPARAMETER alter column DESCRIPTION varchar(1024);
alter table SERVICECATALOGPARAMETER alter column VALUE varchar(1024);

alter table SERVICECONFIGURATION alter column DESCRIPTION varchar(1024);

alter table SERVICECONFIGURATIONPARAMETER alter column DESCRIPTION varchar(1024);
alter table SERVICECONFIGURATIONPARAMETER alter column VALUE varchar(1024);

alter table TARGET alter column SCRIPT varchar(65536);

alter table USER alter column DESCRIPTION varchar(1024);

alter table USERPARAMETER alter column DESCRIPTION varchar(1024);
alter table USERPARAMETER alter column VALUE varchar(1024);
