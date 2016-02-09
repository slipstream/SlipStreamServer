-- Target versions: pre v2.22
-- SHOULD be executed when upgrading to SlipStream v2.22

alter table "USER" add column "CYCLONELOGIN" varchar(255);
alter table "USER" add column "GITHUBLOGIN" varchar(255);
