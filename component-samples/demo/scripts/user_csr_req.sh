#!/bin/bash
#
# Copyright 2021 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
# ***WARNING***: The script generates the credentials using default system
# configurations and might not provide necessary security strength for a
# production deployment. Care must be taken to maintain necessary cryptographic
# strength while generating keys for production deployment.

# Summary:
# user_csr_req.sh creates a certificate signing request for mTLS user/client credentials
# the client.conf contains the subject name of the certificate.
# the csr will be outputed to client.req file.
# the private key will be outputed to client.key
#


openssl req -x509 -newkey rsa:2048 -keyout clientKey.pem -out clientUser.pem -sha256 -days 12775 -nodes -config ./client.conf
openssl x509 -x509toreq -in clientUser.pem -out client.req -signkey clientKey.pem

#comment out following line if signing with external CA
openssl x509 -req -days 12775 -in client.req -CA ca-cert.pem -CAkey caKey.pem -CAcreateserial -out apiUser.pem -extfile ./client.conf -extensions v3_req


#cat CA response to private key
cat apiUser.pem clientKey.pem > api-user.pem
