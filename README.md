**NOTE**: The "protocol-next" branch of the PRI repository contains a preliminary implementation of the [FIDO IoT Spec - Working Draft ](https://fidoalliance.org/specs/fidoiot/FIDO-IoT-spec-v1.0-wd-20200730.html) published by the FIDO Alliance. The implementation is experimental and incomplete, and is not ready for use in any production capacity. Some cryptographic algorithms and encoding formats have not been implemented, and any aspect of this implementation is subject to change.

# FIDO IoT Quick Start

## Source Layout

For the instructions in this document, `<fido-iot-src>` refers to the path of the FIDO
IoT source folder 'pri'.

FIDO IoT source code is organized into following sub-folders.

- `cert-utils`: It contains utilities for loading certificates and keys from PEM formatted strings.

- `protocol`: It contains implementations related to protocol message processing.

- `service`: It contains implementations for FIDO IoT HTTP servers and clients.

- `serviceinfo`: It contains implementations for Owner and Device ServiceInfo.

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

To generate the code coverage report, execute the following command:
```
$ mvn clean verify
```
The code coverage report of individual components are stored in their respective build folder. ( Eg: `protocol/target/site/jacoco/`)

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
You can allow remote database console connections by setting webAllowOthers=true in the .h2.server.properties file located your user home directory (e.g '~' for Linux and C:\Users\[username] for Windows).

### Running FIDO IoT HTTP Clients

#### Running the FIDO IoT Device Initialization (DI) HTTP client
```
$ cd <fido-iot-src>/service/http-client-samples/http-client-di-sample
$ mvn exec:java
```

SerialNo: d35a096f
Device Credentials: 87f5..
DI Client finished.

Refer [Ownership Voucher Creation](#ownership-voucher-creation) for next steps.

#### Running the FIDO IoT To0 HTTP client
```
$ cd <fido-iot-src>/service/http-client-samples/http-client-to0-sample
$ mvn exec:java
```

To0 Response Wait: 3600
TO0 Client finished.

#### Running the FIDO IoT To1 HTTP client
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

## Ownership voucher creation

The DI server will listen for messages at http://localhost:8039/fido/100/msg/<msgid>.
The server also includes an SQL database that runs on port 8049. The database console
UI will be available at http://localhost:8039/console.

Extended ownership vouchers can be obtained from the following uri:

http://localhost:8039/api/v1/vouchers/<serial_no>
***NOTE***: Default serial number is '0'. To get the serial_no corresponding to the GUID, look up the `SERIAL_NO` field of MT_DEVICES table in the DI database.

The hex value of the extended voucher can be obtained by running the API above. The value will be populated on DI server console.

To log in to the database and view records use the following information:
```
"User Name:" = "sa"
"Password:" = "" (blank)
"JDBC URL:" = "jdbc:h2:tcp://<localhost>:8049/<fido-iot-src>/service/http-server-samples/http-server-di-sample/target/data/mfg"
```
The path to the DB will be printed out in following format when the DI server is starting.

`jdbc:h2:tcp:...`

SELECT * FROM MT_DEVICES will show the current vouchers created by DI messages.

## Using Component Samples

### Setup Keystore using SoftHSM

- Install the following dependencies:

  ```
  $ sudo apt-get install libsofthsm2 softhsm softhsm-common opensc
  ```
- Copy <fido-iot-src>/demo/<component-name>/java.security to /etc/java-11-openjdk/security/
- Copy <fido-iot-src>/demo/<component-name>/pkcs11.cfg to /etc/softhsm/

### Configuring the FIDO IoT Manufacturer Sample

Some required runtime arguments

- `manufacturer_di_port`

  Manufacturer server port.

  Docker default is: 8039.

- `manufacturer_database_connection_url`

   JDBC URL for connection to database. Includes the database driver name, port number for database and the location of `.db` file

  Docker default: jdbc:h2:tcp://localhost:8049/./target/data/mfg

- `manufacturer_database_username`

  Manufacturer database username.

  Docker default: sa

- `manufacturer_database_password`

  Manufacturer database password.

  Docker default: <no_password>

- `manufacturer_database_port`

  Manufacturer database port number.

  Docker default: 8049

- `catalina_home`

  Tomcat configuration.

  Docker default: ./target/tomcat

- `manufacturer_keystore_password`

  Keystore password for manufacturer_keystore.p12 and the internal softHSM's PKCS11 keystore.

  Docker default: MfgKs@3er

- `manufacturer_api_user`

  Username for the non-SDO REST API calls.

  Docker default: apiUser

- `manufacturer_api_password`

  Password for the non-SDO REST API calls.

  Docker default: MfgApiPass123

### Running the FIDO IoT Manufacturer Sample

There are two ways of running Manufacturer Component Sample:

1. Using docker

  To execute Manufacturer using docker, refer [Demo Manufacturer README](#https://github.com/secure-device-onboard/pri-fidoiot/tree/master/demo/manufacturer)

2. Command line execution:

```
$ cd <fido-iot-src>/service/component-samples/manufacturer
$ mvn -Dmanufacturer_di_port=<manufacturer-server-port> -Dmanufacturer_database_connection_url=<jdbc-url> -Dmanufacturer_database_username=<manufacturer-database-username> -Dmanufacturer_database_password=<manufacturer-database-password> -Dmanufacturer_database_port=<manufacturer-server-database-port> -Dcatalina_home=<path-to-catalina-home> -Dmanufacturer_keystore_password=<manufacturer-keystore-password> -Dmanufacturer_api_user=<manufacturer-api-user> -Dmanufacturer_api_password=<manufacturer-api-password> exec:java
```

### FIDO IoT Manufacturer REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| POST /api/v1/assign/?id=<customer_id>&guid=<device_guid> | Assigns customer ID to voucher having the input GUID. | Query - id: Customer ID, guid = Device GUID | | | |
| GET /api/v1/vouchers/<serial_no> | Gets extended voucher with the serial number. | Path - Device Serial Number | | | Ownership Voucher |
| POST /api/v1/customers/?id=<customer_id>&name=<customer_name> | Adds customer with the given ID and Public key in PEM format. | Query - id: Customer Id, name: Customer Name | text/plain; charset=us-ascii | Customer PEM formatted Public keys | |

***NOTE*** These REST APIs use Digest authentication. `manufacturer_api_user` and `manufacturer_api_password` properties specify the credentials to be used while making the REST calls.

### Configuring the FIDO IoT Reseller Sample

Some required runtime arguments

- `reseller_api_port`

  Reseller server port.

  Default is: 8070

- `reseller_database_connection_url`

   JDBC URL for connection to database. Includes the database driver name, port number for database and the location of `.db` file

  Default: jdbc:h2:tcp://localhost:8071/<path-to-user-dir>

- `reseller_database_username`

  Reseller database username.

  Default: sa

- `reseller_database_password`

  Reseller database password.

  Default: <no-password>

- `reseller_database_port`

  Reseller database port number.

  Default: 8071

- `reseller_database_driver`

  Reseller database driver.

  Default: org.h2.Driver

- `reseller_api_user`

  Reseller API username.

  Default: admin

- `reseller_api_password`

  Reseller API password.

  Default: test

- `reseller_home`

  Tomcat configuration for catalina home.

  Default: catalina.home

- `reseller_keystore_type`

  Reseller keystore type

  Default: PKCS11

- `reseller_keystore_password`

  Reseller keystore password

  Default: 123456

- `user.dir`

  Base path for jdbc `.db` file location.

### Running the FIDO IoT Reseller Sample

There are two ways of running Reseller Component Sample:

1. Using docker

  To execute Reseller using docker, refer [Demo Reseller README](#https://github.com/secure-device-onboard/pri-fidoiot/tree/master/demo/reseller)

2. Command line execution:

  - Refer steps to [setup keystore using softhsm](#setup-keystore-using-softhsm).
  - Run service using the command:
    ```
    $ cd <fido-iot-src>/service/component-samples/reseller
    $ mvn exec:java
    ```

### FIDO IoT Reseller REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| GET /api/v1/resell/vouchers/<serial_number>?id=<customer_id> | Assigns the customer and returns the extended voucher for the given serial number from `RT_DEVICES` table. | Query - id: Customer Id, Path - Device Serial Number | | | Ownership voucher |
| POST /api/v1/resell/vouchers/<serial_number> | Adds voucher to `RT_DEVICES` table against the serial number. | Path - Device Serial Number | application/cbor | Ownership voucher | |
| DELETE /api/v1/resell/vouchers/<serial_number> | Deletes voucher from `RT_DEVICES` table with the specified serial number. | Path - Device Serial Number | | | |
| POST /api/v1/resell/customers/?id=<customer_id>&name=<customer_name> | Adds customer keyset to `RT_CUSTOMERS` table. | Query - id: Customer Id, name: Customer Name | text/plain; charset=us-ascii | Customer PEM formatted Public keys | |
| POST /api/v1/resell/keys/?alias=<keystore_alias> | Adds new Reseller keys to the keystore with the given alias | Query - alias: Alias to be added in keystore | | PEM formatted certificate and private key | | 
| DELETE /api/v1/resell/keys/?alias=<keystore_alias> | Deletes the keys corresponding to the input alias from keystore. | Query - alias: Alias to be removed from keystore | | | |

***NOTE*** These REST APIs use Digest authentication. `reseller_api_user` and `reseller_api_password` properties specify the credentials to be used while making the REST calls.

### Configuring the FIDO IoT RV Sample

Some required runtime arguments

- `rv_port`

  RV server port.

  Docker default: 8040

- `rv_database_connection_url`

   JDBC URL for connection to database. Includes the database driver name, port number for database and the location of `.db` file

  Docker default: jdbc:h2:tcp://localhost:8050/./target/data/rvs

- `rv_database_username`

  RV database username.

  Docker username: sa

- `rv_database_password`

  RV database password.

  Docker username: <no-password>

- `rv_database_port`

  RV database port number.

  Docker default: 8050

- `epid_online_url`

  EPID Verification Service URL for EPID device signature verification.

  Default: https://verify.epid-sbx.trustedservices.intel.com/
  Other server options: https://verify.epid.trustedservices.intel.com/ (production EPID verification server), https://localhost:1180 (onprem verification service)

- `catalina_home`

  Tomcat configuration for catalina home.

  Docker default: ./target/tomcat

### Running the FIDO IoT RV Sample

There are two ways of running RV Component Sample:

1. Using docker

  To execute RV using docker, refer [Demo RV README](#https://github.com/secure-device-onboard/pri-fidoiot/tree/master/demo/rv)

2. Command line execution:

```
$ cd <fido-iot-src>/service/component-samples/rv
$ mvn -Drv_port=<rv-server-port> -Drv_database_connection_url=<jdbc-url> -Drv_database_username=<rv-database-username> -Drv_database_password=<rv-database-password> -Drv_database_port=<rv-server-database-port> -Depid_online_url=<verification-service-url> -Dcatalina_home=<path-to-cataline-home> exec:java
```

### Configuring the FIDO IoT Owner Sample

Some required runtime arguments

- `owner_to2_port`

  Owner server port.

  Docker default: 8042

- `owner_database_connection_url`

  JDBC URL for connection to database. Includes the database driver name, port number for database and the location of `.db` file.

  Docker default: jdbc:h2:tcp://localhost:8051/./target/data/ops

- `owner_database_username`

  Owner database username.

  Docker default: sa

- `owner_database_password`

  Owner database password.

  Docker default: <no-password>

- `owner_database_port`

  RV database port number.

  Docker default: 8050

- `epid_online_url`

  EPID Verification Service URL for EPID device signature verification.

  Docker Default: https://verify.epid-sbx.trustedservices.intel.com/
  Other server options: https://verify.epid.trustedservices.intel.com/ (production EPID verification server), https://localhost:1180 (onprem verification service)

- `catalina_home`

  Tomcat configuration for cataline home.

  Docker default: ./target/tomcat

- `owner_keystore_password`

  Keystore password for owner_keystore.p12 and the internal softHSM's PKCS11 keystore.

  Docker default: OnrKstr1

- `owner_to0_scheduling_enabled`

  Auto completes TO0 for GUIDs with DI complete state.

  Docker default: false

- `owner_to0_scheduling_interval`

  Time interval to check database for GUIDs with pending TO0.

  Docker default: 300s

- `owner_to0_rv_blob`

  Information containing network address of prospective owner. Owner shares this information with RV during TO0. RV, then shares the same during TO1. Device, then uses this information to initiate TO2 protocol.

  Docker default: http://localhost:8042?ipaddress=127.0.0.1

- `owner_api_user'

  Username for the non-SDO REST API calls.

  Docker default: apiUser

- `owner_api_password`

  Password for the non-SDO REST API calls.

  Docker default: OwnerApiPass123

- `owner_svi_values`

  Path to the directory that contains default sample owner serviceinfo values. The filenames are used as identifiers in the database, while the actual file content is the requisite serviceinfo that is transferred to the device. Only used for demo purposes and should not be modified.

  Docker default: ./serviceinfo/sample-values

- `owner_svi_string`

  Path to the file that contains default sample svi string that maps serviceinfo values to module names and messages. Only used for demo purposes and should not be modified.

  Docker default: ./serviceinfo/sample-svi.csv

### Running the FIDO IoT Owner Sample

There are two ways of running Owner Component Sample:

1. Using docker

  To execute RV using docker, refer [Demo Owner README](#https://github.com/secure-device-onboard/pri-fidoiot/tree/master/demo/owner)

2. Command line execution:

   - Refer steps to [setup keystore using softhsm](#setup-keystore-using-softhsm).
   - Run service using the command:
   ```
   $ cd <fido-iot-src>/service/component-samples/owner
   $ mvn -Downer_to2_port=<owner-server-port> -Downer_database_connection_url=<jdbc-url> -Downer_database_username=<owner-database-name> -Downer_database_password=<owner-database-password> -Downer_database_port=<owner-server-database-port> -Depid_online_url=<verification-service-url> -Dcatalina_home=<path-to-catalina-home> -Downer_keystore_password=<owner-keystore-password> -Downer_to0_scheduling_enabled=<true-or-false> -Downer_to0_scheduling_interval=<time-interval-in-ms> -Downer_to0_rv_blob=<owner-url> -Downer_api_user=<owner-api-user> -Downer_api_password=<owner-api-password> -Downer_svi_values=<path-to-owner-svi-values> -Downer_svi_string=<path-to-owner-svi-string> exec:java
   ```

### FIDO IoT Owner REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| GET /api/v1/owner/vouchers/    | Returns all voucher available in `TO2_DEVICES` table. | | | | Comma-separated list of GUIDs |
| GET /api/v1/owner/vouchers/?id=<device_guid> | Returns the voucher for the specified GUID. | Query - id: Device GUID | | | Ownership voucher |
| POST /api/v1/owner/vouchers/ | Insert voucher against the specified GUID in `TO2_DEVICES` table. | | application/cbor | Content of Ownership voucher in binary format | |
| DELETE /api/v1/owner/vouchers/?id=<device_guid> | Deletes voucher of the specified GUID from the `TO2_DEVICES` table. | Query - id: Device GUID | | | |
| POST /api/v1/owner/svivalues/?id=\<serviceinfo_id>&isCborEncoded=<boolean_value> | Adds serviceinfo entry to `OWNER_SERVICEINFO` table. The query parameter 'isCborEncoded' should be 'true' for CBOR encoded binary data that will never be split into smaller chunks while transferring to the device (small in length, to be used for CBOR primitives such as boolean, int, array and map), and it should be 'false' (preferably) for other binary data that could be splitted into smaller chunks and transferred across messages (for example binary values, string). | Query - id: Service info ID, isCborEncoded: Boolean | application/octet-stream or application/cbor | Content of Serviceinfo in binary format. | |
| DELETE /api/v1/owner/svivalues/?id=<serviceinfo_id> | Deletes the serviceinfo values from `OWNER_SERVICEINFO` table. | Query - id: Service info ID | | | |
| POST /api/v1/owner/svi/?guid=<device_guid> | Adds owner serviceinfo for the GUID in `GUID_OWNERSVI` table, that will be transferred to the device in the insertion order. The format is `Entry1,Entry2,Entry3` and so on, where each Entry contains `moduleName:messageName=serviceInfoId`. Here, the 'content' corresponding to the 'serviceInfoId' is transferred to the device. Please see \<fido-iot-src\>/demo/owner/serviceinfo/sample-svi.csv as an example for the above format, where moduleName is 'sdo_sys' and messageName is either one of 'filedesc', 'write' and 'exec'. The order of each 'Entry' is important as this order decides the sequence in which the Owner will transfer the Service info. | Query - guid: Device GUID | application/text | SVI string | |
| DELETE /api/v1/owner/svi/?guid=<device_guid> | Deletes owner serviceinfo for the GUID from the `GUID_OWNERSVI` table. | Query - guid: Device GUID | | | |

***NOTE*** These REST APIs use Digest authentication. `owner_api_user` and `owner_api_password` properties specify the credentials to be used while making the REST calls.

### Configuring the FIDO IoT HTTP Java Device Sample

Some software settings are runtime-configurable via Java properties.  They include:

- `fido.iot.randoms`

  A comma-separated list of Java `SecureRandom` algorithm names for random number generation.
  Values are in order from least to most preferred.

  Default is `"NativePRNG,Windows-PRNG"`.

- `fido.iot.url.di`

  The URL at which the Device Initialization (DI) server may be found.

  Default is `http://localhost:8039/`.

- `fido.iot.pem.dev`

  The location of the PEM file containing the device keys (private and public).
  If not set, a hardcoded key is used - see the Java source for details.

  There is no default. Provide value './device.pem' to use the existing default EC-256 key-pair.

### Running the FIDO IoT HTTP Java Device Sample
```
$ cd <fido-iot-src>/device
$ mvn -Dfido.iot.url.di=<di-server-URL> -Dfido.iot.pem.dev=<device-PEM-file> exec:java
```

Alternatively,
```
$ cd <fido-iot-src>/demo/device
$ java -Dfido.iot.url.di=<di-server-URL> -Dfido.iot.pem.dev=<device-PEM-file> -jar device.jar
```

The `device-PEM-file` must contain the following PEM-encoded data:
- The device's private key
- The device's public key or certificate.

The device will initialize and exit.  A `credential.bin` file will be created containing the device state.
Removing this file will make the device re-initialize the next time it runs.

The initialization (manufacturer) server must be available during this step.

```
$ mvn -Dfido.iot.pem.dev=<device-PEM-file> exec:java
```

The device will be onboarded.

The rendezvous and owner servers must be available during this step.

### Running a demo using Component Samples

1. Start the FIDO IoT Manufacturer Sample as per the steps outlined in [Running the FIDO IoT Manufacturer Sample](#running-the-fido-iot-manufacturer-sample).

2. Complete Device Initialization (DI) by starting the FIDO IoT HTTP Java Device Sample as per the steps outlined in [Running the FIDO IoT HTTP Java Device Sample](#running-the-fido-iot-http-java-device-sample). Delete any existing 'credential.bin' before starting the device.

3. Complete Ownership Voucher Extension by using the API `GET /api/v1/vouchers/<serial_no>` and save the Ownership Voucher. By default, existing customer with customer Id '1', is assigned to the device. To add a new customer and assign the inserted customer to the device, please refer to [FIDO IoT Manufacturer REST APIs](#fido-iot-manufacturer-rest-apis) for more information about the API.

4. Start the FIDO IoT RV Sample as per the steps outlined in [Running the FIDO IoT RV Sample](#running-the-fido-iot-rv-sample).

5. Start the FIDO IoT Owner Sample as per the steps outlined in [Running the FIDO IoT Owner Sample](#running-the-fido-iot-owner-sample). Import the extended Ownership voucher from Step#3 into the Owner database by using the API `POST /api/v1/owner/vouchers/`. Please refer to [FIDO IoT Owner REST APIs](#fido-iot-owner-rest-apis) for more information about the API. Optionally, if service info transfer is needed, please refer to [Enabling Service info transfer](#enabling-service-info-transfer).

6. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FIDO IoT HTTP Java Device Sample again. The previously created 'credential.bin' from Step#2 will be used directly by the Device.

### Service info setup between FIDO IoT HTTP Java Device Sample and FIDO IoT Owner Sample

The FIDO IoT HTTP Java Device Sample curretly supports `sdo_sys` module for interpreting received owner service info and `devmod` module to share device service info with Owner.

- `sdo_sys` Owner service info module: This module supports the following 3 message names as listed below to interpret the service info as received from the Owner. The basic functionality of this module is to support payload/script transfers and basic command execution.  A sample format looks like 'sdo_sys:filedesc=filename, sdo_sys:write=filecontent,sdo_sys:exec=command-to-execute'.

    *filedesc* - The name to be given to the file once it is transferred. Upon receiving this, device creates a file with the given name and opens stream to write into it.

    *write* - The payload/content (script, binaries, and others) that is sent to the device. Upon receiving this, device writes the content into the open stream as given by the preceeding 'filedesc' message.

    *exec* - The command that will be executed at the device. Device executes the command as received.

***NOTE*** The comma-separated values must be ordered such that the 'filedesc' and 'write' objects are one after the other pair-wise, followed by the 'exec' commands.

- `devmod` Device service info module: This module supports multiple messages as listed down in the protocol specification, that are sent to the Owner as Device Service info. A sample format looks like 'devmod:active=1'.

The FIDO IoT Owner Sample currently supports the same `sdo_sys` module to send Owner service info to the Device and `devmod` module to store the received Device Service info. Few sample service info values, as present in \<fido-iot-src\>/demo/owner/serviceinfo/sample-values/ are populated into the database table 'OWNER_SERVICEINFO' as byte arrays. For keeping the association between the Device and the service info values to transfer, 'GUID_OWNERSVI' database table is used. When a Device is inserted into the database table 'TO2_DEVICES', it'll not have any association with the service info values, and so by default, no service info is transferred to the Device.

#### Enabling Service info transfer

To enable service info transfer to a Device with a given GUID, following are the steps:

1. (Optional) Insert required Service info values into the database table 'OWNER_SERVICEINFO' using the API `POST /api/v1/owner/svivalues/?id=<ServiceInfoId>&isCborEncoded=<boolean>`. More information about the same is provided in section [FIDO IoT Owner REST APIs](#fido-iot-owner-rest-apis). If the required service info already exists in the table, go on to the next step.

2. (Mandatory) Insert required association between the Device and Service info values to transfer using the API `POST /api/v1/owner/svi/?guid=<guid>`. More information about the same is provided in section [FIDO IoT Owner REST APIs](#fido-iot-owner-rest-apis). As a referance, please see \<fido-iot-src\>/demo/owner/serviceinfo/sample-svi.csv, which says that Owner will transfer the column 'Content' of serviceinfoIds 'payload.bin' and 'package.sh', which the device will store in files named by the column 'Content' of serviceinfoids 'payload_name' and 'package_name'. Additionally, the Owner transfers the command as specified in column 'Content' of serviceinfoId 'binsh-linux', to be executed by the Device.