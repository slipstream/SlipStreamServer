-- Target versions: pre v3.1
-- SHOULD be executed when upgrading to SlipStream v3.1

alter table "usage_summaries" add column "grouping" varchar(100);
