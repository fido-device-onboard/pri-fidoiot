#!/bin/bash
#
# Copyright 2023 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
#
# Summary:
# Builds device package

echo -e "building device package"
tar -czvf device-package.tar.gz onboard-device.sh additonal-device-file.txt
echo -e "done building package"
