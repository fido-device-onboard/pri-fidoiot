#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# Sample script that starts the Owner service.

# Start the owner service
# Configure heap size based on your requirement.
# Trying updating heap size to troubleshoot any unexpected errors.

SSL_PARAMS="-Dfido_ssl_mode=$fido_ssl_mode -Dssl_truststore=$ssl_truststore -Dssl_truststore_password=$ssl_truststore_password -Dssl_truststore_type=$ssl_truststore_type"
exec java ${SSL_PARAMS} -Xmx256m -jar owner.jar
