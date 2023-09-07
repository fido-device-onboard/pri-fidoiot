#!/bin/bash
#
# Copyright 2023 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
#
# Summary:
# onboard-device.sh script is downloaded to the device by FDO as part of device-package.tar.gz
# the device-package.tar.gz is extracted and this script is as described blow
# tar -xf device-package.tar.gz
# bash onboard-device.sh
#
# the purpose of onboard-device.sh is to run programs on the device and collect files to send to the owner
# after onboard-device.sh completes then the file owner-package.tar.gz will be uploaded

# the follow can be replaced with anything you want
echo "hello from device" > for-owner.output
echo "additional data for owner" > addition-owner-data.txt
tar -zcvf owner-package.tar.gz for-owner.output addition-owner-data.txt

