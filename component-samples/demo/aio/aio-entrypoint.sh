#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# Sample script that starts the Owner service.

# Start the aio service
# Configure heap size based on your requirement.
# Trying updating heap size to troubleshoot any unexpected errors.

# LOG4J2 configuration file
LOG_CONFIG="-Dlog4j.configurationFile=${log4j_configuration_file}"

exec java ${LOG_CONFIG} -Xmx256m -jar aio.jar
