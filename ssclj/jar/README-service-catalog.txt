To be added as part of the release note.

Pre-requesite
* leiningen installed
* this script must be run on the server itself.

The Clojure script works in 2 phases:
1) Import current service catalog content (XML) and writes it to JSON files.
(for both attribute and service-info new resources)

2) Seeding of the resources with the JSON files.

Usage

Following environment variables must be defined:
MIGRATION_USER          : user to retrieve content during import
MIGRATION_PASSWORD      : password to retrieve content during import
MIGRATION_SCHEMA_NAME   : schema name to use for attribute and service-info URIs (e.g http://schema.a.b.c)

$ lein run import
Import XML from Service Catalog and stores JSON files (for new resource)...
:service-info  JSON saved to src/main/resources/service-info.json
:attribute  JSON saved to src/main/resources/attribute.json

$ lein run seed
Created  attribute/3dbb69ac-cdbc-4fb6-b911-5abf9706eac5
...
Created  attribute/93bcf734-2cf3-43f8-b38c-2183b86a202e
Created  service-info/5da7049f-3ab1-4bcd-82e3-92b96ac4a0aa
...
Created  service-info/284b3334-2d3a-4a52-a0d6-937d28f3affd

Populate some records:

curl -XPOST -d@a1.json -H "content-type: application/json"  -H "slipstream-authn-info: super ADMIN" http://localhost:8201/api/service-attribute
with a1.json being a JSON file for an attribute.

Migration of existing attributes and service-info:

* Update DB
