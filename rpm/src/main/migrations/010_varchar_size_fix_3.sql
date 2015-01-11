-- Target version: v2.3.8
-- SHOULD be executed when upgrading to SlipStream v2.3.8

alter table RUNTIMEPARAMETER alter column MAPPEDRUNTIMEPARAMETERNAMES varchar(65536);
