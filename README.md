**NOTE**: The "protocol-next" branch of the PRI repository contains a preliminary implementation of the [FIDO IoT Spec - Working Draft ](https://fidoalliance.org/specs/fidoiot/FIDO-IoT-spec-v1.0-wd-20200730.html) published by the FIDO Alliance. The implementation is experimental and incomplete, and is not ready for use in any production capacity. Some cryptographic algorithms and encoding formats have not been implemented, and any aspect of this implementation is subject to change.

# FIDO IoT Quick Start

## Source Layout

For the instructions in this document, `<fido-iot-src>` refers to the path of the FIDO
IoT source folder 'pri'.

FIDO IoT source code is organized into following sub-folders.

- `cert-utils`: It contains utilities for loading certificates and keys from PEM formatted strings.

- `protocol`: It contains implementations related to protocol message processing.

- `service`: It contains implementations for Fido IoT HTTP servers and clients.

- `storage`: It contains SQL based storage implementations for Fido IoT Servers.

## Building FIDO IoT source

FIDO IoT source is written in [Java 11](https://openjdk.java.net/projects/jdk/11/) and uses the
[Apache Maven* software](http://maven.apache.org). The instructions which follow describe a simple
build and assume familiarity with
[the Maven build lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).

Following ports are used for unit-tests and sample code - 8039, 8040, 8042, 8043, 8049, 8050 and 8051.
Ensure that these ports are not used by other applications while building and executing the
binaries.

Use following commands to build FIDO IoT source.
```
$ cd <fido-iot-src>
$ mvn clean install
```

The build creates artifacts which will be used in the rest of this guide.

## Starting FIDO IoT HTTP Servers

### Starting the FIDO IoT Rendezvous (RV) HTTP Server
```
$ cd <fido-iot-src>/service/http-server-samples/http-server-rv-sample/
$ mvn exec:java
```

The server will listen for FIDO IoT http messages on port 8040.
The H2 database will listen on TCP port 8050.

### Starting the FIDO IoT Owner HTTP Server
```
$ cd <fido-iot-src>/service/http-server-samples/http-server-owner-sample/
$ mvn exec:java
```

The server will listen for FIDO IoT HTTP messages on port 8042.
The H2 database will listen on TCP port 8051.

### Starting the FIDO IoT Device Initialization (DI) HTTP Server
```
$ cd <fido-iot-src>/service/http-server-samples/http-server-di-sample/
$ mvn exec:java
```

The server will listen for FIDO IoT HTTP messages on port 8039.
The H2 database will listen on TCP port 8049.
You can allow remote database console connections by setting webAllowOthers=true in the .h2.server.properties file located your user home directory (e.g '~' for Linux and C:\Users\[username] for Windows).

## Running FIDO IoT HTTP Clients

### Running the FIDO IoT Device Initialization (DI) HTTP client
```
$ cd <fido-iot-src>/service/http-client-samples/http-client-di-sample
$ mvn exec:java
```

SerialNo: d35a096f
Device Credentials: 87f5..
DI Client finished.

## Ownership voucher creation

The DI server will listen for messages at http://localhost:8039/fido/100/msg/<msgid>.
The server also includes an SQL database that runs on port 8049. The database console
UI will be available at http://localhost:8039/console.

Extended ownership vouchers can be obtained from the following uri:

http://localhost:8039/api/v1/vouchers/<serial_no>

To log in to the database and view records use the following information:
```
"User Name:" = "sa"
"Password:" = "" (blank)
"JDBC URL:" = "jdbc:h2:tcp://<localhost>:8049/<fido-iot-src>/service/http-server-samples/http-server-di-sample/target/data/mfg"
```
The path to the DB will be printed out in following format when the DI server is starting.

`jdbc:h2:tcp:...`

SELECT * FROM MT_DEVICES will show the current vouchers created by DI messages.

### Running the FIDO IoT To0 HTTP client
```
$ cd <fido-iot-src>/service/http-client-samples/http-client-to0-sample
$ mvn exec:java
```

To0 Response Wait: 3600
TO0 Client finished.

### Running the FIDO IoT To1 HTTP client
```
$ cd <fido-iot-src>/service/http-client-samples/http-client-to1-sample
$ mvn exec:java
```

signed RV Blob: 84a10126...
TO1 Client finished.

### Running the FIDO IoT To2 HTTP client
```
$ cd <fido-iot-src>/service/http-client-samples/http-client-to2-sample
$ mvn exec:java
```

TO2 Client finished.
