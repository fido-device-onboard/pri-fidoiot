# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk libsofthsm2 softhsm softhsm-common opensc wget

# Configuring softHSM and JVM provider.
COPY java.security /etc/java-11-openjdk/security/
COPY pkcs11.cfg /etc/softhsm/

RUN mkdir -p /home/owner/lib /home/owner/serviceinfo
WORKDIR /home/owner/

# Setup owner dependencies
COPY ./lib ./lib/
COPY ./serviceinfo ./serviceinfo/
COPY owner.jar .
COPY owner_keystore.p12 .
COPY owner-entrypoint.sh .
CMD ["chmod", "+x", "owner-entrypoint.sh"]

# Configure and start owner
CMD ["bash", "owner-entrypoint.sh"]
