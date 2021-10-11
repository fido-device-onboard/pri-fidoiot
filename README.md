# FIDO Device Onboard (FDO) Protocol Reference Implementation (PRI) Quick Start

This is a reference implementation of the
[FIDO Device Onboard Spec](https://fidoalliance.org/specs/FDO/fido-device-onboard-v1.0-ps-20210323/)
published by the FIDO Alliance. It provides production-ready implementation for the protocol defined
by the specification. It also provides example implementation for different components to
demonstrate end-to-end execution of the protocol. Appropriate security measures should be taken while
deploying the example implementation for these components.

## System Requirements:

* **Ubuntu 20.04 / RHEL 8.4**.
* **Maven 3.6.3**.
* **Java 11**.
* **Haveged**.

## Source Layout

For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.

FDO PRI source code is organized into the following sub-folders.

- `component-samples`: It contains all the normative and non-normative server and client implementation with all specifications listed in the base profile.

- `http-api-samples`: It contains Servlet implementation for various operations to be performed using different REST endpoints for all server services.

- `protocol-samples`: It contains client and server implementation that demonstrates the preliminary E2E demo that educates the end-user with the protocol workflow.

- `protocol`: It contains implementations related to protocol message processing.

- `util`: It contains utility packages such as storage, ServiceInfo, dispatchers - for message passing and cert-utils - for loading certificates and keys from PEM formatted strings.

## Building FDO PRI Source

FDO PRI source is written in [Java 11](https://openjdk.java.net/projects/jdk/11/) and uses the
[Apache Maven* software](http://maven.apache.org).

The list of ports that are used for unit tests and sample code:

| Port | Description    |
| ---- | -------------- |
| 8038 | manufacturer https port |
| 8039 | manufacturer http port |
| 8040 | rv http port |
| 8041 | rv https port |
| 8042 | owner http port |
| 8043 | owner https port |
| 8049 | manufacturer database port |
| 8050 | rv database port |
| 8051 | owner database port |
| 8070 | reseller http port |
| 8071 | reseller database port |
| 8072 | reseller https port |
| 8080 | aio http port |
| 8081 | aio database port |
| 8090 | Test RV port |
| 8091 | Test DB port |
| 8443 | aio https port |

Use the following commands to build FDO PRI source.
```
$ cd <fdo-pri-src>
$ mvn clean install
```

The build creates artifacts which will be used in the rest of this guide.

## Using Protocol Samples

Protocol samples can run directly without any configuration. There is only one
option and that is to set EPID test mode. By default this is set to false. To enable 
test mode set the following variable to true in the application.properties file:
epid_test_mode=true
Place a copy of the file in both the protocol-samples\http-server-to0-to1-sample and 
protocol-samples\http-server-to2-sample directories before starting the servers.

### Starting FDO PRI HTTP Servers

#### Starting the FDO PRI Rendezvous (RV) HTTP Server
```
$ cd <fdo-pri-src>/protocol-samples/http-server-to0-to1-sample/
$ mvn exec:java
```

The server will listen for FDO PRI http messages on port 8040.
The H2 database will listen on TCP port 8050.

#### Starting the FDO PRI Owner HTTP Server
```
$ cd <fdo-pri-src>/protocol-samples/http-server-to2-sample/
$ mvn exec:java
```

The server will listen for FDO PRI HTTP messages on port 8042.
The H2 database will listen on TCP port 8051.

#### Starting the FDO PRI Device Initialization (DI) HTTP Server
```
$ cd <fdo-pri-src>/protocol-samples/http-server-di-sample/
$ mvn exec:java
```

The server will listen for FDO PRI HTTP messages on port 8039.
The H2 database will listen on TCP port 8049.
You can allow remote database console connections by setting webAllowOthers=true in the .h2.server.properties file located your user home directory (for example, '~' for Linux and C:\Users\[username] for Windows).

### Running FDO PRI HTTP Clients

#### Running the FDO PRI Device Initialization (DI) HTTP Client
```
$ cd <fdo-pri-src>/protocol-samples/http-client-di-sample
$ mvn exec:java
```
Expect the following line on successful DI completion.

SerialNo: d35a096f

Device Credentials: 87f5..

DI Client finished.

Refer [Ownership Voucher Creation](#ownership-voucher-creation) for the next steps.

#### Running the FDO PRI TO0 HTTP Client
```
$ cd <fdo-pri-src>/protocol-samples/http-client-to0-sample
$ mvn exec:java
```
Expect the following message on successful TO0 completion.

TO0 Response Wait: 3600

TO0 Client finished.

#### Running the FDO PRI TO1 HTTP Client
```
$ cd <fdo-pri-src>/protocol-samples/http-client-to1-sample
$ mvn exec:java
```

signed RV Blob: 84a10126...
TO1 Client finished.

### Running the FDO PRI TO2 HTTP Client
```
$ cd <fdo-pri-src>/protocol-samples/http-client-to2-sample
$ mvn exec:java
```

TO2 Client finished.

***NOTE***: During the execution of the Protocol Samples using the command 'mvn exec:java', the following warning messages may be displayed on the console. These warning messages are a result of the version discrepancy of Guice with Maven and Java 11. This does not have any effect on the execution of the Protocol Sample.
```
WARNING: An illegal reflective access operation has occurred
WARNING: Illegal reflective access by com.google.inject.internal.cglib.core.$ReflectUtils$1 (file:/usr/share/maven/lib/guice.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain)
WARNING: Please consider reporting this to the maintainers of com.google.inject.internal.cglib.core.$ReflectUtils$1
WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
WARNING: All illegal access operations will be denied in a future release
```

## Ownership Voucher Creation

The DI server will listen for messages at http://localhost:8039/fdo/100/msg/<msgid>.
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
"JDBC URL:" = "jdbc:h2:tcp://<localhost>:8049/<fdo-pri-src>/protocol-samples/http-server-di-sample/target/data/mfg"
```
The path to the DB will be printed out in the following format when the DI server is starting.

`jdbc:h2:tcp:...`

SELECT * FROM MT_DEVICES will show the current Ownership Vouchers created by DI messages.

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in the following files:


- To enable remote access for DI server DB, update file:  `<fdo-pri-src>/protocol-samples/http-server-di-sample/src/main/java/org/fidoalliance/fdo/sample/DiApp.java` <br/>

- To enable remote access for TO0-TO1 server DB, update file:  `<fdo-pri-src>/protocol-samples/http-server-to0-to1-sample/src/main/java/org/fidoalliance/fdo/sample/To0To1ServerApp.java` <br/>

- To enable remote access for TO2 server DB, update file: `<fdo-pri-src>/protocol-samples/http-server-to2-sample/src/main/java/org/fidoalliance/fdo/sample/To2ServerApp.java`

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <service_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Enabling Rendezvous Bypass

FDO includes a Rendezvous Bypass mechanism that is useful for IOT deployments that are not
dependent on a particular network structure or ownership.

In such cases, an FDO device may elect to bypass the FDO Rendezvous Server mechanism and use
the local mechanism instead. Since the TO2 provides full authentication and authorization of the
Device to the Owner, there is no change in the security posture of FDO.

To enable Rendezvous Bypass

- Update the RVInfo blob with `rvbypass` flag and owner address using the API `POST /api/v1/rvinfo` with
  `http://<owner-ip:port>?rvbypass=&ipaddress=<owner-ip>&ownerport=<port>` as body.

- Setting the `rvbypass` flag in RVblob, causes the TO1 protocol to be skipped, and a TO2 connection
 to be attempted to `<owner-ip>` address mentioned in the above POST body.

# EPID Test Mode

EPID devices can be tested using `Test` mode. EPID `Test` mode feature is intended to support onboarding for `development` and `test` devices. Enabling the test mode means signature verification won't be performed for the device. Test mode is enabled by default for protocol-sample in components.

***NOTE***: Not recommended for use in production systems.

# Using Component Samples

Refer to [Demo README](component-samples/demo/README.md) for steps to run component sample demo.

# Support for OnDie Devices

Refer to [Demo README](component-samples/demo/README.md) for steps to configure component-samples to support OnDie devices.

Support for OnDie devices is built into the protocol-samples and no configuration is required. The OnDie certs and CRLs are preloaded into the
protocol-samples/onDieCache directory. Should these need to be refreshed (in case of devices released after the FDO PRI release) then the script in
component-samples/onDieScript.py can be used to update the artifacts in this directory.
