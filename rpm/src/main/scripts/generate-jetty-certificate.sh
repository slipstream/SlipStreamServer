#!/bin/bash
PASSWORD=${1:?"Provide keystore password. Sould match the one in jetty SSL config."}
FULL_HOSTNAME=${2:?"Provide FQDN of the host to be set as CN of the server cert."}

JETTY_HOME=`dirname $0`
JETTY_CERT=${JETTY_HOME}/jetty.jks

# If certificate exists, then do nothing.
if [ -f "${JETTY_CERT}" ]; then
  exit 0;
fi

# File is needed for OpenSSL.
RANDFILE=${JETTY_HOME}/.rnd
touch ${RANDFILE}
export RANDFILE

cd ${JETTY_HOME}

echo "Creating SSL certificate for Jetty..."

cat > openssl.cfg <<EOF
[ req ]
distinguished_name     = req_distinguished_name
x509_extensions        = v3_ca
prompt                 = no
input_password         = ${PASSWORD}
output_password        = ${PASSWORD}

dirstring_type = nobmp

[ req_distinguished_name ]
C = EU
CN = ${FULL_HOSTNAME}

[ v3_ca ]
basicConstraints = CA:false
nsCertType=server, email, objsign
keyUsage=critical, digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment, keyAgreement
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer:always

EOF

# Generate initial private key.
openssl genrsa -passout pass:${PASSWORD} -des3 -out test-key.pem 2048

# Create a certificate signing request.
openssl req -new -key test-key.pem -out test.csr -config openssl.cfg

# Create (self-)signed certificate. 
openssl x509 -req -days 365 -in test.csr -signkey test-key.pem \
             -out test-cert.pem -extfile openssl.cfg -extensions v3_ca \
             -passin pass:${PASSWORD}

# Convert to PKCS12 format. 
openssl pkcs12 -export -in test-cert.pem -inkey test-key.pem -out test.p12 \
               -passin pass:${PASSWORD} -passout pass:${PASSWORD}

# Import PKCS12 certificate/key into the java store.
keytool -importkeystore \
        -srckeystore test.p12 \
        -srcstoretype pkcs12 \
        -srcstorepass ${PASSWORD} \
        -destkeystore ${JETTY_CERT} \
        -deststoretype jks \
        -deststorepass ${PASSWORD}

# Remove intermediate files.
rm -f openssl.cfg test-key.pem test.csr test-cert.pem
