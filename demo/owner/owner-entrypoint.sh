#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# Sample script that configures the softHSM and inserts the owner keys

# Initialize softHSM token with SO PIN and User pin.
softhsm2-util --init-token --slot 0 --label "Owner-Token" --so-pin $owner_keystore_password --pin $owner_keystore_password

# Import owner keys into PKCS11 keystore
keytool -importkeystore -deststorepass $owner_keystore_password -destkeystore NONE -srckeystore owner_keystore.p12 -deststoretype PKCS11 -srcstoretype PKCS12 -srcstorepass $owner_keystore_password -alias owner_pri

# Delete the PKCS12 keystore as its not needed anymore
rm -f owner_keystore.p12

# Start the owner service
exec java -jar owner.jar
