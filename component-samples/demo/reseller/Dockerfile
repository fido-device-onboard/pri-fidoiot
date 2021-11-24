# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk wget haveged
RUN apt-get install -y opensc

RUN useradd -ms /bin/bash fdo
RUN mkdir -p /home/fdo/lib
WORKDIR /home/fdo/

# Setup reseller dependencies
COPY ./lib ./lib/
COPY ./certs ./certs
COPY log4j2.xml .
COPY reseller.jar .
COPY reseller_keystore.p12 owner_pub_keys.pem* ./
COPY reseller-entrypoint.sh .

RUN chown -R fdo:fdo /home/fdo
USER fdo

# Configure and start reseller
CMD ["bash", "reseller-entrypoint.sh"]
