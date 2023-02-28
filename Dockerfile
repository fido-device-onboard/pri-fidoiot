FROM ubuntu:20.04
ARG _JAVA_OPTIONS
ENV _JAVA_OPTIONS=${_JAVA_OPTIONS}
RUN apt-get update 
RUN apt-get install -y openjdk-11-jdk

RUN useradd -ms /bin/bash fdo
WORKDIR /home/fdo/
RUN mkdir -p /home/fdo/lib /home/fdo/WEB-INF /home/fdo/app-data

# Setup service dependencies
COPY ./lib ./lib/
COPY ./WEB-INF ./WEB-INF
COPY log4j2.xml .
COPY aio.jar .
COPY hibernate.cfg.xml .
COPY service.yml .
COPY context.xml .
COPY tomcat-users.xml .
COPY secrets.run.path /home/fdo/secrets.path


RUN chown -R fdo:fdo /home/fdo
USER fdo

# Configure and start all-in-one
CMD ["/usr/lib/jvm/java-11-openjdk-amd64/bin/java", "-jar", "aio.jar"]