# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:24.04@sha256:cd1dba651b3080c3686ecf4e3c4220f026b521fb76978881737d24f200828b2b

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get clean && apt-get -y update && apt-get -y upgrade && apt-get install -y --no-install-recommends openjdk-17-jdk maven git

# Create a user 'fdouser'. If the user name is updated, please update the same in docker-compose.yaml.
RUN useradd -ms /bin/bash fdouser
RUN mkdir -p /home/fdouser/pri-fidoiot/ ; chown -R fdouser:fdouser /home/fdouser/pri-fidoiot/
RUN mkdir -p /home/fdouser/.m2/; chown -R fdouser:fdouser /home/fdouser/.m2
USER fdouser

WORKDIR /home/fdouser/pri-fidoiot/
ENTRYPOINT ./build/build.sh
