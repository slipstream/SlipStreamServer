-- Target versions: pre v2.2.4
-- SHOULD be executed when upgrading to SlipStream v2.2.4

UPDATE RUNTIMEPARAMETER SET value = 'Aborted' WHERE resourceuri IN ((SELECT container_resourceuri FROM Runtimeparameter WHERE key_ = 'ss:state' AND value = 'Terminal') INTERSECT (SELECT container_resourceuri FROM Runtimeparameter WHERE key_ = 'ss:abort' AND value <> ''));
UPDATE RUNTIMEPARAMETER SET value = 'Done' WHERE key_ = 'ss:state' AND value = 'Terminal';
UPDATE RUNTIMEPARAMETER SET value = 'Initializing' WHERE key_ = 'ss:state' AND value = 'Inactive'; 
UPDATE RUNTIMEPARAMETER SET value = 'Ready' WHERE key_ = 'ss:state' AND value = 'Detached'; 
