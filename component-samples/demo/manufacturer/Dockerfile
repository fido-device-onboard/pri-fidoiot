# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk libsofthsm2 softhsm softhsm-common opensc wget haveged

# Configuring softHSM and JVM provider.
COPY java.security /etc/java-11-openjdk/security/
COPY pkcs11.cfg /etc/softhsm/

RUN mkdir -p /home/manufacturer/lib
WORKDIR /home/manufacturer/

# Setup manufacturer dependencies
COPY ./lib ./lib/
COPY ./certs ./certs/
COPY manufacturer.jar .
COPY manufacturer_keystore.p12 .
COPY manufacturer-entrypoint.sh .
CMD ["chmod", "+x", "manufacturer-entrypoint.sh"]

# Configure and start manufacturer
CMD ["bash", "manufacturer-entrypoint.sh"]