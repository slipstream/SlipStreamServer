-- Target versions: pre v2.3.1
-- SHOULD be executed when upgrading from SlipStream < 2.3.1 to >= v2.3.1

SET AUTOCOMMIT FALSE;

-- START TRANSACTION;

ALTER TABLE Module ADD GROUPCREATECHILDREN BOOLEAN;
ALTER TABLE Module ADD GROUPDELETE BOOLEAN;
ALTER TABLE Module ADD GROUPGET BOOLEAN;
ALTER TABLE Module ADD GROUPMEMBERS_ VARCHAR(1024);
ALTER TABLE Module ADD GROUPPOST BOOLEAN;
ALTER TABLE Module ADD GROUPPUT BOOLEAN;
ALTER TABLE Module ADD INHERITEDGROUPMEMBERS BOOLEAN;
ALTER TABLE Module ADD OWNER VARCHAR(255);
ALTER TABLE Module ADD OWNERCREATECHILDREN BOOLEAN;
ALTER TABLE Module ADD OWNERDELETE BOOLEAN;
ALTER TABLE Module ADD OWNERGET BOOLEAN;
ALTER TABLE Module ADD OWNERPOST BOOLEAN;
ALTER TABLE Module ADD OWNERPUT BOOLEAN;
ALTER TABLE Module ADD PUBLICCREATECHILDREN BOOLEAN;
ALTER TABLE Module ADD PUBLICDELETE BOOLEAN;
ALTER TABLE Module ADD PUBLICGET BOOLEAN;
ALTER TABLE Module ADD PUBLICPOST BOOLEAN;
ALTER TABLE Module ADD PUBLICPUT BOOLEAN;

UPDATE Module SET GroupCreateChildren = (SELECT GroupCreateChildren FROM Authz WHERE id = authz_id);
UPDATE Module SET GroupDelete = (SELECT GroupDelete FROM Authz WHERE id = authz_id);
UPDATE Module SET GroupGet = (SELECT GroupGet FROM Authz WHERE id = authz_id);
UPDATE Module SET GroupMembers_ = (SELECT GroupMembers_ FROM Authz WHERE id = authz_id);
UPDATE Module SET GroupPost = (SELECT GroupPost FROM Authz WHERE id = authz_id);
UPDATE Module SET GroupPut = (SELECT GroupPut FROM Authz WHERE id = authz_id);
UPDATE Module SET InheritedGroupMembers = (SELECT InheritedGroupMembers FROM Authz WHERE id = authz_id);
UPDATE Module SET Owner = (SELECT Owner FROM Authz WHERE id = authz_id);
UPDATE Module SET OwnerCreateChildren = (SELECT OwnerCreateChildren FROM Authz WHERE id = authz_id);
UPDATE Module SET OwnerDelete = (SELECT OwnerDelete FROM Authz WHERE id = authz_id);
UPDATE Module SET OwnerGet = (SELECT OwnerGet FROM Authz WHERE id = authz_id);
UPDATE Module SET OwnerPost = (SELECT OwnerPost FROM Authz WHERE id = authz_id);
UPDATE Module SET OwnerPut = (SELECT OwnerPut FROM Authz WHERE id = authz_id);
UPDATE Module SET PublicCreateChildren = (SELECT PublicCreateChildren FROM Authz WHERE id = authz_id);
UPDATE Module SET PublicDelete = (SELECT PublicDelete FROM Authz WHERE id = authz_id);
UPDATE Module SET PublicGet = (SELECT PublicGet FROM Authz WHERE id = authz_id);
UPDATE Module SET PublicPost = (SELECT PublicPost FROM Authz WHERE id = authz_id);
UPDATE Module SET PublicPut = (SELECT PublicPut FROM Authz WHERE id = authz_id);

ALTER TABLE Authz RENAME TO AuthzBackup;

COMMIT;

