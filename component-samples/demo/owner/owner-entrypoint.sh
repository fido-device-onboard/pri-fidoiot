#!/bin/sh
#
# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

# Sample script that starts the Owner service.

# Start the owner service
# Configure heap size based on your requirement.
# Trying updating heap size to troubleshoot any unexpected errors.
exec java -Xmx256m -jar owner.jar
