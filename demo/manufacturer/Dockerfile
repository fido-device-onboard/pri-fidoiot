# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk

RUN useradd -ms /bin/bash manufacturer
USER manufacturer
WORKDIR /home/manufacturer/
RUN mkdir -p /home/manufacturer/lib

# Setup manufacturer dependencies
COPY ./lib ./lib/
COPY manufacturer.jar .
COPY manufacturer-entrypoint.sh .
CMD ["chmod", "+x", "manufacturer-entrypoint.sh"]

# Configure and start manufacturer
CMD ["bash", "manufacturer-entrypoint.sh"]