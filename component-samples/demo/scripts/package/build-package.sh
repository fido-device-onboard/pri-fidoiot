#!/bin/bash
#
# Copyright 2023 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
#
# Summary:
# Builds device package

echo -e "bulding device package"
tar cvf device-package.tar.gz onboard-device.sh additonal-device-file.txt
echo -e "done building package"
