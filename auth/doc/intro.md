# Introduction to auth


## Build and install as a tiny library
This is used by SlipStream main server to check token validity locally.
> lein with-profile uberjar install

This will install in maven repository this jar:
../.m2/repository/com/sixsq/slipstream/auth/2.14-SNAPSHOT/auth-2.14-SNAPSHOT.jar

## Create private and public keys

openssl genrsa -aes128 -out auth_privkey.pem 2048
openssl rsa -pubout -in auth_privkey.pem -out auth_pubkey.pem






