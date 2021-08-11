# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk opensc wget haveged

RUN useradd -ms /bin/bash fdo
RUN mkdir -p /home/fdo/lib
WORKDIR /home/fdo/

# Setup manufacturer dependencies
COPY ./lib ./lib/
COPY ./certs ./certs/
COPY ./ondie_cache ./ondie_cache/
COPY log4j2.xml .
COPY manufacturer.jar .
COPY manufacturer_keystore.p12 owner_pub_keys.pem* reseller_pub_keys.pem* ./
COPY manufacturer-entrypoint.sh .

RUN chown -R fdo:fdo /home/fdo
USER fdo

# Configure and start manufacturer
CMD ["bash", "manufacturer-entrypoint.sh"]
