#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# LOG4J2 configuration file
LOG_CONFIG="-Dlog4j.configurationFile=log4j2.xml"

# Start the rv service
exec java ${LOG_CONFIG} -jar /home/fdouser/rv.jar
