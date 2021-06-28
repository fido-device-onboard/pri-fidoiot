# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk libsofthsm2 softhsm softhsm-common opensc wget haveged


# AIO does not use HSM by default
# Configuring softHSM and JVM provider.
#COPY java.security /etc/java-11-openjdk/security/
#COPY pkcs11.cfg /etc/softhsm/

RUN mkdir -p /home/aio/lib
WORKDIR /home/aio/

# Setup aio dependencies
RUN mkdir -p /home/aio/onDieCache
COPY ./lib ./lib/
COPY ./certs ./certs
COPY aio.jar .
COPY manufacturer_keystore.p12 .
COPY owner_keystore.p12 .
COPY owner_customer1.pem .
COPY init.sql .
COPY new-device.sql .
COPY aio-entrypoint.sh .
CMD ["chmod", "+x", "aio-entrypoint.sh"]

# Configure and start aio
CMD ["bash", "aio-entrypoint.sh"]
