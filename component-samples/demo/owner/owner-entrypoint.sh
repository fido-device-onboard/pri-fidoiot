#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# Sample script that starts the Owner service.

# LOG4J2 configuration file
LOG_CONFIG="-Dlog4j.configurationFile=${log4j_configuration_file}"

# Start the owner service
# Configure heap size based on your requirement.
# Trying updating heap size to troubleshoot any unexpected errors.

SSL_PARAMS="-Dfido_ssl_mode=$fido_ssl_mode -Dssl_truststore=$ssl_truststore -Dssl_truststore_password=$ssl_truststore_password -Dssl_truststore_type=$ssl_truststore_type"
exec java ${LOG_CONFIG} ${SSL_PARAMS} -Xmx256m -jar owner.jar
