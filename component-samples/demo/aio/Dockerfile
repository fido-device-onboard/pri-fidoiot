# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk libsofthsm2 softhsm softhsm-common opensc wget haveged

RUN useradd -ms /bin/bash fdo

RUN mkdir -p /home/fdo/lib
RUN mkdir -p /home/fdo/certs
RUN mkdir -p /home/fdo/resources
RUN mkdir -p /home/fdo/downloads
RUN mkdir -p /home/fdo/ondie_cache

WORKDIR /home/fdo/

# Setup aio dependencies
COPY ./lib ./lib/
COPY ./certs ./certs
COPY ./downloads ./downloads
COPY ./resources ./resources
COPY ./ondie_cache ./ondie_cache
COPY aio.jar .
COPY manufacturer_keystore.p12 .
COPY owner_keystore.p12 .
COPY init.sql .
COPY new-device.sql .
COPY aio-entrypoint.sh .

RUN chown -R fdo:fdo /home/fdo
USER fdo

# Configure and start aio
CMD ["bash", "aio-entrypoint.sh"]
