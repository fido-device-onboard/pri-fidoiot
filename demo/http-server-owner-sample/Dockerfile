# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk
RUN useradd -ms /bin/bash fidoiotowner
USER fidoiotowner
WORKDIR /home/fidoiotowner/
RUN mkdir lib target
COPY ./lib ./lib/
COPY ./target ./target/
COPY http-server-owner-sample.jar .
CMD ["java", "-jar", "http-server-owner-sample.jar"]
