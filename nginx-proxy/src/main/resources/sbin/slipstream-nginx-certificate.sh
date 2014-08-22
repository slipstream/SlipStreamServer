#!/bin/bash

function setup_ssl() {

    # Create a directory for the certificate
    mkdir -p /etc/nginx/ssl

    # Moving to certificate directory
    pushd /etc/nginx/ssl/

    # Creating the server private key
    openssl genrsa -out server.key 2048

    # Get the machine hostname.
    export SS_HOSTNAME=`hostname -f`

    cat > openssl.cfg <<EOF
[ req ]
distinguished_name     = req_distinguished_name
x509_extensions        = v3_ca
prompt                 = no

dirstring_type = nobmp

[ req_distinguished_name ]
C = EU
CN = ${SS_HOSTNAME}

[ v3_ca ]
basicConstraints = CA:false
nsCertType=server, email, objsign
keyUsage=critical, digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment, keyAgreement
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always
EOF

    # Creating the Certificate Signing Request (CSR)
    openssl req -new -key server.key -out server.csr -config openssl.cfg

    # Signing the certificate using the former private key and CSR
    openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt

    # Remove work files.
    rm -f openssl.cfg server.csr

    # Restoring to previous directory
    popd
}

EOF

if [ ! -f /etc/nginx/ssl/server.crt ]; then
    setup_ssl;
fi
