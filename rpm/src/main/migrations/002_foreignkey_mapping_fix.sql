-- Target versions: pre v2.2.0
-- MUST be executed when upgrading to SlipStream v2.2.0 and BEFORE restarting SlipStream server

-- Migrating MAPKEY and MODULE_RESOURCE_URI fields from MODULE_NODE
alter table NODE add column NODES_KEY varchar(255);
update NODE set NODES_KEY = (select MAPKEY from MODULE_NODE where MODULE_NODE.NODES_ID = NODE.ID);
update NODE set MODULE_RESOURCEURI = (select MODULE_RESOURCEURI from MODULE_NODE where MODULE_NODE.NODES_ID = NODE.ID);
drop table MODULE_NODE;

-- Migrating MODULE_RESOURCE_URI field from MODULE_PACKAGE
alter table PACKAGE add column MODULE_RESOURCEURI varchar(255);
update PACKAGE set MODULE_RESOURCEURI = (select MODULE_RESOURCEURI from MODULE_PACKAGE where MODULE_PACKAGE.PACKAGES_ID = PACKAGE.ID);
drop table MODULE_PACKAGE;

-- Migrating MODULE_RESOURCE_URI field from MODULE_TARGET
alter table TARGET add column MODULE_RESOURCEURI varchar(255);
update TARGET set MODULE_RESOURCEURI = (select MODULE_RESOURCEURI from MODULE_TARGET where MODULE_TARGET.TARGETS_ID = TARGET.ID);
drop table MODULE_TARGET;
