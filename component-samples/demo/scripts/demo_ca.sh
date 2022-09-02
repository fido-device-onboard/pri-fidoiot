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
# demo_ca.sh creates a demo certificate signing authority based on properties in the root-ca.conf
# This demo CA can sign web and api user certificates
# 
# 

openssl req -x509 -newkey rsa:4096 -keyout caKey.pem -out ca-cert.pem -sha256 -days 12775 -nodes -config ./root-ca.conf