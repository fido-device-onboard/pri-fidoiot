# Copyright 2020 Intel Corporation
# SPDX-License-Identifier: Apache 2.0

FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update && apt-get install -y openjdk-11-jdk haveged

RUN useradd -ms /bin/bash sdouser
USER sdouser
WORKDIR /home/sdouser/
RUN mkdir -p /home/sdouser/lib

# Setup rv dependencies
COPY ./lib ./lib/
COPY ./certs ./certs/
COPY config.properties .
COPY rv.jar .
COPY rv-entrypoint.sh .
CMD ["chmod", "+x", "rv-entrypoint.sh"]

# Configure and start rv
CMD ["bash", "rv-entrypoint.sh"]