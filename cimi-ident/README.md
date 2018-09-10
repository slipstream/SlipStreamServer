This script allows for a "manual" creation of a user-identifier document for

- A user  
- An instance (the OIDC organization)
  The external login
  
  
1 - Create jar file
--------------------

From project root run
`lein uberjar`

Copy the jar on a machine accessing ES


2 - run the script
------------------

```java -jar SlipStreamIdentity-jar-3.59-SNAPSHOT-standalone.jar -h
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
[2018-09-07T11:27:20,021][INFO ][o.e.p.PluginsService     ] [_client_] no modules loaded
[2018-09-07T11:27:20,024][INFO ][o.e.p.PluginsService     ] [_client_] loaded plugin [org.elasticsearch.index.reindex.ReindexPlugin]
[2018-09-07T11:27:20,024][INFO ][o.e.p.PluginsService     ] [_client_] loaded plugin [org.elasticsearch.join.ParentJoinPlugin]
[2018-09-07T11:27:20,025][INFO ][o.e.p.PluginsService     ] [_client_] loaded plugin [org.elasticsearch.percolator.PercolatorPlugin]
[2018-09-07T11:27:20,025][INFO ][o.e.p.PluginsService     ] [_client_] loaded plugin [org.elasticsearch.script.mustache.MustachePlugin]
[2018-09-07T11:27:20,025][INFO ][o.e.p.PluginsService     ] [_client_] loaded plugin [org.elasticsearch.transport.Netty4Plugin]
18-09-07 09:27:36 Erics-MBP.fritz.box INFO [com.sixsq.slipstream.tools.cli.users-identifiers:42] -   -u, --user USER                       Create user identifier for provided user id.
  -i, --instance INSTANCE               Will be used as prefix in identifier, eg SixSq
  -e, --external EXTERNAL-LOGIN         The unmangled federated username
  -h, --help
      --logging LEVEL            :info  Logging level: trace, debug, info, warn, error, fatal, or report.
```

E.g :

```
java -jar -jar SlipStreamIdentity-jar-3.59-SNAPSHOT-standalone.jar  -- -u usertest -i instance -e '273149@vho-switchaai.chhttps://aai-logon.vho-switchaai.ch/idp/shibboleth!https://fed-id.nuv.la/samlbridge/module.php/saml/sp/metadata.php/sixsq-saml-bridge!+lbxsfaq+vvwm/iajeyszg+pc3q='
```

This will create a new identifier

```
{
  "updated" : "2018-09-07T09:31:50.310Z",
  "created" : "2018-09-07T09:31:50.310Z",
  "id" : "user-identifier/d223292e2041f21234770a760807bed0",
  "identifier" : "instance:273149@vho-switchaai.chhttps://aai-logon.vho-switchaai.ch/idp/shibboleth!https://fed-id.nuv.la/samlbridg\\\ne/module.php/saml/sp/metadata.php/sixsq-saml-bridge!+lbxsfaq+vvwm/iajeyszg+pc3q=",
  "acl" : {
    "owner" : {
      "principal" : "ADMIN",
      "type" : "ROLE"
    },
    "rules" : [ {
      "type" : "ROLE",
      "principal" : "ADMIN",
      "right" : "ALL"
    }, {
      "principal" : "usertest",
      "type" : "USER",
      "right" : "VIEW"
    } ]
  },
  "operations" : [ {
    "rel" : "edit",
    "href" : "user-identifier/d223292e2041f21234770a760807bed0"
  }, {
    "rel" : "delete",
    "href" : "user-identifier/d223292e2041f21234770a760807bed0"
  } ],
  "resourceURI" : "http://sixsq.com/slipstream/1/UserIdentifier",
  "user" : {
    "href" : "user/usertest"
  }
}
```
