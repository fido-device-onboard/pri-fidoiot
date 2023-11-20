#!/bin/bash
#
# Copyright 2023 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
#
# Summary:
# Uploads service-info instruction

echo -e "Posting service info"
cat ./svi-insructions.txt
echo -e
curl --cacert ../ca-cert.pem --cert ../api-user.pem --key ../api-user.pem --noproxy "host.docker.internal" --header 'Content-Type: text/plain' -d "@svi-insructions.txt" -i --request POST  https://host.docker.internal:8443/api/v1/owner/svi 
echo -e
