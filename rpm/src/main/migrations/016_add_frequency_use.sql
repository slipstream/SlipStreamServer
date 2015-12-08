-- Target versions: pre v2.20
-- SHOULD be executed when upgrading to SlipStream v2.20

alter table "usage_summaries" add column "frequency" varchar(30);
