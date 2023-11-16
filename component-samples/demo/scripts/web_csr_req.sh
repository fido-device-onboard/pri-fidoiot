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
# web_csr_req.sh creates a certificate signing request for a web server host
# the web-server.conf contains the subject name of the certificate.
# the csr will be outputed to server.req file.
# the private key will be outputed to client.key
#


openssl req -x509 -newkey rsa:2048 -keyout server-key.pem -out webServer.pem -sha256 -days 12775 -nodes -config ./web-server.conf
openssl x509 -x509toreq -in webServer.pem -out server.req -signkey server-key.pem

#comment out following line if signing with external CA
openssl x509 -req -days 12775 -in server.req -CA ca-cert.pem -CAkey caKey.pem -CAcreateserial -out server-cert.pem -extfile ./web-server.conf -extensions v3_req

#Add read permission to server-key.pem
chmod 644 server-key.pem
