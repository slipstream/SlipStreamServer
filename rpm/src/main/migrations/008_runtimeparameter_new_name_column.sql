-- Target versions: pre v2.3.1
-- SHOULD be executed when upgrading to SlipStream v2.3.1

ALTER TABLE Runtimeparameter ADD COLUMN name_ VARCHAR(255);
UPDATE Runtimeparameter SET name_ = REGEXP_SUBSTRING(key_,'[^:]*$');

SET DATABASE TRANSACTION CONTROL MVCC;

COMMIT;
