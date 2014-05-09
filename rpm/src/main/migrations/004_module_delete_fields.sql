-- Target versions: pre v2.2.0
-- SHOULD be executed when upgrading to SlipStream v2.2.0

alter table MODULE drop column COMMENT;
alter table MODULE drop column PUBLISHED;
