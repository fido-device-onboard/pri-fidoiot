**NOTE**: This is a preliminary implementation of the [FIDO IoT Spec - Working Draft ](https://fidoalliance.org/specs/fidoiot/FIDO-IoT-spec-v1.0-wd-20200730.html) published by the FIDO Alliance. The implementation is experimental and incomplete, and is not ready for use in any production capacity. Some cryptographic algorithms and encoding formats have not been implemented, and any aspect of this implementation is subject to change.

# FIDO IoT Quick Start

## System Requirements:

* **Ubuntu 20.04**.
* **Maven 3.6.3**.
* **Java 11**.
* **Haveged**.

## Source Layout

For the instructions in this document, `<fido-iot-src>` refers to the path of the FIDO
IoT source folder 'pri'.

FIDO IoT source code is organized into the following sub-folders.

- `cert-utils`: It contains utilities for loading certificates and keys from PEM formatted strings.

- `protocol`: It contains implementations related to protocol message processing.

- `service`: It contains implementations for FIDO IoT HTTP servers and clients.

- `serviceinfo`: It contains implementations for Owner and Device ServiceInfo.

- `storage`: It contains SQL based storage implementations for Fido IoT Servers.

## Building FIDO IoT source

FIDO IoT source is written in [Java 11](https://openjdk.java.net/projects/jdk/11/) and uses the
[Apache Maven* software](http://maven.apache.org). The instructions which follow describes a simple
build and assume familiarity with
[the Maven build lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).

Following ports are used for unit-tests and sample code - 8039, 8040, 8042, 8043, 8049, 8050, and 8051.
Ensure that these ports are not used by other applications while building and executing the
binaries.

Use the following commands to build FIDO IoT source.
```
$ cd <fido-iot-src>
$ mvn clean install
```

The build creates artifacts which will be used in the rest of this guide.

## Using Protocol Samples

### Starting FIDO IoT HTTP Servers

#### Starting the FIDO IoT Rendezvous (RV) HTTP Server
```
$ cd <fido-iot-src>/service/protocol-samples/http-server-to0-to1-sample/
$ mvn exec:java
```

The server will listen for FIDO IoT http messages on port 8040.
The H2 database will listen on TCP port 8050.

#### Starting the FIDO IoT Owner HTTP Server
```
$ cd <fido-iot-src>/service/protocol-samples/http-server-to2-sample/
$ mvn exec:java
```

The server will listen for FIDO IoT HTTP messages on port 8042.
The H2 database will listen on TCP port 8051.

#### Starting the FIDO IoT Device Initialization (DI) HTTP Server
```
$ cd <fido-iot-src>/service/protocol-samples/http-server-di-sample/
$ mvn exec:java
```

The server will listen for FIDO IoT HTTP messages on port 8039.
The H2 database will listen on TCP port 8049.
You can allow remote database console connections by setting webAllowOthers=true in the .h2.server.properties file located your user home directory (for example, '~' for Linux and C:\Users\[username] for Windows).

### Running FIDO IoT HTTP Clients

#### Running the FIDO IoT Device Initialization (DI) HTTP client
```
$ cd <fido-iot-src>/service/protocol-samples/http-client-di-sample
$ mvn exec:java
```

SerialNo: d35a096f
Device Credentials: 87f5..
DI Client finished.

Refer [Ownership Voucher Creation](#ownership-voucher-creation) for next steps.

#### Running the FIDO IoT To0 HTTP client
```
$ cd <fido-iot-src>/service/protocol-samples/http-client-to0-sample
$ mvn exec:java
```

To0 Response Wait: 3600
TO0 Client finished.

#### Running the FIDO IoT To1 HTTP client
```
$ cd <fido-iot-src>/service/protocol-samples/http-client-to1-sample
$ mvn exec:java
```

signed RV Blob: 84a10126...
TO1 Client finished.

### Running the FIDO IoT To2 HTTP client
```
$ cd <fido-iot-src>/service/protocol-samples/http-client-to2-sample
$ mvn exec:java
```

TO2 Client finished.

***NOTE***: During the execution of the Protcol Samples using the command 'mvn exec:java', following warning messages may be displayed on the console. These warning messages are a result of the version discrepency of Guice with maven and Java 11. This does not have any effect on the execution of the Protocol Sample.
```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.google.inject.internal.cglib.core.$ReflectUtils$1 (file:/usr/share/maven/lib/guice.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of com.google.inject.internal.cglib.core.$ReflectUtils$1
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```

## Ownership Voucher creation

The DI server will listen for messages at http://localhost:8039/fido/100/msg/<msgid>.
The server also includes a SQL database that runs on port 8049. The database console
UI will be available at http://localhost:8039/console.

Extended Ownership Vouchers can be obtained from the following url:

http://localhost:8039/api/v1/vouchers/<serial_no>
***NOTE***: Default serial number is '0'. To get the serial_no corresponding to the GUID, look up the `SERIAL_NO` field of MT_DEVICES table in the DI database.

The hex value of the extended Ownership Voucher can be obtained by running the API above. The value will be populated on DI server console.

To log in to the database and view records use the following information:
```
"User Name:" = "sa"
"Password:" = "" (blank)
"JDBC URL:" = "jdbc:h2:tcp://<localhost>:8049/<fido-iot-src>/service/protocol-samples/http-server-di-sample/target/data/mfg"
```
The path to the DB will be printed out in following format when the DI server is starting.

`jdbc:h2:tcp:...`

SELECT * FROM MT_DEVICES will show the current Ownership Vouchers created by DI messages.

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in the following files:

- To enable remote access for DI server DB, update file:  `<fido-iot-src>/service/protocol-samples/http-server-di-sample/src/main/java/org/fido/iot/sample/DiApp.java` <br/>

- To enable remote access for TO0-TO1 server DB, update file:  `<fido-iot-src>/service/protocol-samples/http-server-to0-to1-sample/src/main/java/org/fido/iot/sample/To0To1ServerApp.java` <br/>

- To enable remote access for TO2 server DB, update file:  `<fido-iot-src>/service/protocol-samples/http-server-to2-sample/src/main/java/org/fido/iot/sample/To2ServerApp.java`

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <service_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# EPID Test Mode

EPID devices can be tested using `Test` mode. EPID `Test` mode feature is intended to support onboarding for `development` and `test` devices. Enabling the test mode means signature verification won't be performed for the device. Test mode is enabled by default for protocol-sample in components.

**NOTE** Not recommended for use in production systems.

# Using Component Samples

Refer to [Demo README](demo/README.md) for steps to run component sample demo.