# Introduction to auth

## Create private and public keys

> cd .../resources

> rm auth_p*

> openssl genrsa -aes128 -out auth_privkey.pem 2048

> openssl rsa -pubout -in auth_privkey.pem -out auth_pubkey.pem

Use same passphrase as declared in resources/<config> ! 

## Start authentication server (in dev environment)

> lein repl

> user=> (require '[com.sixsq.slipstream.auth.app.server :as s])

> user=> (s/start 8202)

2015-09-02 12:09:23,976 INFO  - creating ring handler
2015-09-02 12:09:23,982 INFO  - starting the http-kit container on port 8202
{:stop-fn #object[clojure.lang.AFunction$1 0x4c028a32 "clojure.lang.AFunction$1@4c028a32"]}

## Build and install as a tiny library

This is used by SlipStream main server to check token validity locally (no need to make a HTTP call).
> lein with-profile uberjar install

This will install in maven repository this jar (version may differ):
../.m2/repository/com/sixsq/slipstream/auth/2.16-SNAPSHOT/auth-2.16-SNAPSHOT.jar

# Troubleshooting

## "Something strange out there" displayed with valid credentials

One possibility is that the authentication server is not started.
Check for the following message in SlipStream logs:

2015-09-03T10:24:18.351+0200 SEVERE com.sixsq.slipstream.authn.AuthProxy throwConnectionError 
Error in contacting authentication server : The connector failed to complete the communication with the server 

To fix this, just start authentication server.



