#!/usr/bin/env bash

KEYS_LOC=/etc/slipstream/auth
PRIVKEY=$KEYS_LOC/auth_privkey.pem
PUBKEY=$KEYS_LOC/auth_pubkey.pem

if [ ! -f $PRIVKEY ]; then
   rm -f $PRIVKEY $PUBKEY
   mkdir -p $KEYS_LOC
   openssl genrsa -out $PRIVKEY 2048
   openssl rsa -pubout -in $PRIVKEY -out $PUBKEY
fi