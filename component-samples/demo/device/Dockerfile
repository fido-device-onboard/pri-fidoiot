# Copyright 2022 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:22.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-17-jre --no-install-recommends

RUN useradd -ms /bin/bash fdo
WORKDIR /home/fdo/
RUN mkdir -p /home/fdo/lib /home/fdo/WEB-INF /home/fdo/app-data

# Setup service dependencies
COPY ./lib ./lib/
COPY log4j2.xml .
COPY device.jar .
COPY service.yml .
COPY secrets.run.path /home/fdo/secrets.path


RUN chown -R fdo:fdo /home/fdo
USER fdo

# Configure and start all-in-one
CMD ["/usr/lib/jvm/java-17-openjdk-amd64/bin/java", "-jar", "device.jar"]