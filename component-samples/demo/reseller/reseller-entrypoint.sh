#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# LOG4J2 configuration file
LOG_CONFIG="-Dlog4j.configurationFile=${log4j_configuration_file}"

# Sample script that configures the softHSM and inserts the reseller keys

# Initialize softHSM token with SO PIN and User pin.
softhsm2-util --init-token --slot 0 --label "Reseller-Token" --so-pin $reseller_keystore_password --pin $reseller_keystore_password

# Import owner keys into PKCS11 keystore
keytool -importkeystore -deststorepass $reseller_keystore_password -destkeystore NONE -srckeystore reseller_keystore.p12 -deststoretype PKCS11 -srcstoretype PKCS12 -srcstorepass $reseller_keystore_password

# Delete the PKCS12 keystore as its not needed anymore
rm -f reseller_keystore.p12

# Start the owner service
exec java ${LOG_CONFIG} -jar reseller.jar
