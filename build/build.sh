#!/bin/bash

# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

export http_proxy_host=$(echo $http_proxy | awk -F':' {'print $2'} | tr -d '/')
export http_proxy_port=$(echo $http_proxy | awk -F':' {'print $3'} | tr -d '/')

export https_proxy_host=$(echo $https_proxy | awk -F':' {'print $2'} | tr -d '/')
export https_proxy_port=$(echo $https_proxy | awk -F':' {'print $3'} | tr -d '/')

export _JAVA_OPTIONS="-Dhttp.proxyHost=$http_proxy_host -Dhttp.proxyPort=$http_proxy_port -Dhttps.proxyHost=$https_proxy_host -Dhttps.proxyPort=$https_proxy_port"
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

REMOTE_URL=https://github.com/fido-device-onboard/pri-fidoiot.git
REMOTE_BRANCH=master

# The PGP signature verification requires additional proxy configuration in
# ~/.m2/settings.xml. For simplicity and easier out of box experience, the PGP
# signature verification is disabled here. In case this is used to create
# production build, it is recommended to update ~/.m2/settings.xml to include
# proxy configurations.
MVN_CONFIG="-Dpgpverify.skip=true -Dmaven.test.skip=true"

if [ "$use_remote" = "1" ]; then
  echo "Building $REMOTE_URL : $REMOTE_BRANCH"
  git clone -b $REMOTE_BRANCH $REMOTE_URL /tmp/pri-fidoiot

  # Build source code
  cd /tmp/pri-fidoiot
  mvn clean install ${MVN_CONFIG}

  # Replace the demo folder in local copy
  cp -arufv /tmp/pri-fidoiot/component-samples/demo/* /home/fdouser/pri-fidoiot/component-samples/demo/
else
  echo "Building local copy"
  mvn clean install ${MVN_CONFIG}
fi
