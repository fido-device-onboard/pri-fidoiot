#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# Sample script that starts the Owner service.

# Start the aio service
# Configure heap size based on your requirement.
# Trying updating heap size to troubleshoot any unexpected errors.

# PKCS11 is not used by default for AIO
# Sample script that configures the softHSM and inserts the manufacturer keys

# Initialize softHSM token with SO PIN and User pin.
#softhsm2-util --init-token --slot 0 --label "manufacturer-Token" --so-pin $manufacturer_keystore_password --pin $manufacturer_keystore_password

# Import manufacturer keys into PKCS11 keystore
#keytool -importkeystore -deststorepass $manufacturer_keystore_password -destkeystore NONE -srckeystore manufacturer_keystore.p12 -deststoretype PKCS11 -srcstoretype PKCS12 -srcstorepass $manufacturer_keystore_password


exec java -Xmx256m -jar aio.jar
