#!/bin/bash
#
# Copyright 2023 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
#
# Summary:
# Uploads service-info instruction

echo -e "Posting service info downloadable resource"
echo -e
curl --cacert ../ca-cert.pem --cert ../api-user.pem --key ../api-user.pem --noproxy "host.docker.internal" --header 'Content-Type: text/plain' --data-binary "@device-package.tar.gz" -i --request POST https://host.docker.internal:8443/api/v1/owner/resource?filename=device-package.tar.gz
echo -e
echo "Done posting reasource"
echo -e
