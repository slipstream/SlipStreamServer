HOW TO USE
----------

- Unzip the tar bundle `SlipStreamMigration-3.55-SNAPSHOT.tar.gz` created by this project on `nuv.la`
- Run the script via `lein run`

The Script addresses 4 categories of users :


1 - Users having an `externalIdentity` vector attributes

Each element of the vector will create a UserIdentifier resource, where identifier is 
the element of the externalIdentity vector

2 - Users having a `githublogin` attribute

A resource is created with identifier like "github:XXXX" where XXXX is the githublogin value


3 - Users belonging to an HNSciCloud organization

The lowercase version of the organization (e.g "cern") will compose the first 
part of the identifier and the OIDC login is the second part 

Users may exist both with both a sanitized and an unmangled version of their OIDC login 
names :  In this case, the sanitized version is exluded from the migration.


4 - Users belonging to the "Biosphere" organization

They have a UserIdentifier with identifier starting with "biosphere:"


