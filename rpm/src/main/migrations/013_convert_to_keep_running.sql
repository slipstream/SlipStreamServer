-- Target versions: pre v2.5.0
-- Should be executed when upgrading from SlipStream < 2.5.0 to >= v2.5.0
-- README: You first need to upgrade SlipStream, then run the script '012_edit_save_all_users.py', then execute this script.

SET AUTOCOMMIT FALSE;

-- PLACEHOLDER FOR 012_edit_save_all_users.py

UPDATE UserParameter SET value = 'always' WHERE name = 'General.keep-running' AND container_resourceuri IN 
    (SELECT container_resourceuri FROM 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Success Run Forever' AND value = 'true') AS a INNER JOIN 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Error Run Forever' AND value = 'true') AS b ON a.container_resourceuri = b.container_resourceuri);

UPDATE UserParameter SET value = 'on-success' WHERE name = 'General.keep-running' AND container_resourceuri IN 
    (SELECT container_resourceuri FROM 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Success Run Forever' AND value = 'true') AS a INNER JOIN 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Error Run Forever' AND value = 'false') AS b ON a.container_resourceuri = b.container_resourceuri);

UPDATE UserParameter SET value = 'on-error' WHERE name = 'General.keep-running' AND container_resourceuri IN 
    (SELECT container_resourceuri FROM 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Success Run Forever' AND value = 'false') AS a INNER JOIN 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Error Run Forever' AND value = 'true') AS b ON a.container_resourceuri = b.container_resourceuri);

UPDATE UserParameter SET value = 'never' WHERE name = 'General.keep-running' AND container_resourceuri IN 
    (SELECT container_resourceuri FROM 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Success Run Forever' AND value = 'false') AS a INNER JOIN 
        (SELECT container_resourceuri FROM UserParameter WHERE name = 'General.On Error Run Forever' AND value = 'false') AS b ON a.container_resourceuri = b.container_resourceuri);

DELETE FROM UserParameter WHERE name = 'General.On Success Run Forever' OR name = 'General.On Error Run Forever';

-- COMMIT;
