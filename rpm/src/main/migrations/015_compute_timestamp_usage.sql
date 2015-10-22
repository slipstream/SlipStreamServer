-- Target versions: pre v2.18
-- SHOULD be executed when upgrading to SlipStream v2.18

alter table "usage_summaries" add column "compute_timestamp" varchar(30);
