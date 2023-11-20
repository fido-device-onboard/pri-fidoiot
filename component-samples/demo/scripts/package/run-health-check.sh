#!/bin/bash
#
# Copyright 2023 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
# Summary:
# Performs health check for FDO servies

echo -e "Performing health check for https"
curl --cacert ../ca-cert.pem  --noproxy 'host.docker.internal' -i  https://host.docker.internal:8443/health
echo -e
echo -e "Performing health check for http"
curl --noproxy 'host.docker.internal' -i  http://host.docker.internal:8080/health
echo -e
echo -e "Health check complete"
echo -e
