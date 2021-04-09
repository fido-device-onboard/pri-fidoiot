# System Requirements:

* **Ubuntu 20.04**.
* **Maven**.
* **Java 11**.
* **Haveged**.
* **SoftHSM**.

# Getting the executable

Use the following commands to build FIDO Device Onboard (FDO) Protocol Reference Implementation
(PRI) Manufacturer component sample source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/component-samples/manufacturer/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/manufacturer/.

# Configuring the FDO PRI Manufacturer Sample

Manufacturer runtime arguments:

- `manufacturer_di_port`

  Manufacturer server port.

  Default value: 8039.

- `manufacturer_database_connection_url`

   JDBC URL for connection to database. Includes the database driver name, port number for database and the location of `.db` file

  Default value: jdbc:h2:tcp://localhost:8049/./target/data/mfg

- `manufacturer_database_username`

  Manufacturer database username.

  Default value: sa

- `manufacturer_database_password`

  Manufacturer database password.

  Default value: <no_password>

- `manufacturer_database_port`

  Manufacturer database port number.

  Default value: 8049

- `catalina_home`

  Tomcat configuration.

  Docker default: ./target/tomcat

- `manufacturer_keystore_password`

  Keystore password for manufacturer_keystore.p12 and the internal softHSM's PKCS11 keystore.

  Default value: MfgKs@3er

- `manufacturer_api_user`

  Username for the database REST API calls.

  Default value: apiUser

- `manufacturer_api_password`

  Password for the database REST API calls.

  Default value: MfgApiPass123

- `manufacturer_protocol_scheme`

  Enables the service to run in https mode. Pass argument value `https` for the same. For all other values, the server service defaults to `http` scheme.

  Default value: http

- `manufacturer_https_port`

  Allows enduser to select a port for accepting HTTPS requests.
  **NOTE** This property is not required if service is running in `http` mode.

  Default value: 443

- `manufacturer_ssl_keystore`

  Provides path for SSL keystore to be used by the service, in case it runs in HTTPS mode.
  **NOTE** This property is not required if service is running in `http` mode.

  Default value: <fdo-pri-src>/component-samples/demo/manufacturer/certs/ssl.p12

- `manufacturer_ssl_keystore-password`

  Provides password for the specified keystore.
  **NOTE** This property is not required if service is running in `http` mode.

  Default keystore password: fdo123

## Support for OnDie devices

Refer to [Demo README](../README.md) for steps to configure manufacturer to support OnDie devices.

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `<fdo-pri-src>/service/component-samples/manufacturer/src/main/java/org/fidoalliance/fdo/sample/ManufacturerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <manufacturer_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Starting the Manufacturer service

Refer the [Docker Commands](../README.md/#docker-commands) to start the service.

***NOTE*** The database file located at \<fdo-pri-src\>/component-samples/demo/manufacturer/target/data/mfg.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FDO PRI Manufacturer REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| POST /api/v1/assign/?id=<customer_id>&guid=<device_guid> | Assigns customer ID to Ownership Voucher having the input GUID. | Query - id: Customer ID, guid = Device GUID | | | |
| GET /api/v1/vouchers/<serial_no> | Gets extended Ownership Voucher with the serial number. | Path - Device Serial Number | | | Ownership Voucher |
| POST /api/v1/customers/?id=<customer_id>&name=<customer_name> | Adds customer with the given ID and Public key in PEM format. | Query - id: Customer Id, name: Customer Name | text/plain; charset=us-ascii | Customer PEM formatted Public keys | |
| POST /api/v1/rvinfo/ | Updates RV Info in `MT_SETTINGS` table | | text/plain; charset=us-ascii | RV Info | | |


# Inserting keys into Manufacturer keystore

The PKCS12 keystore file \<fdo-pri-src\>/component-samples/demo/manufacturer/manufacturer_keystore.p12 contains the default manufacturer keys that are imported into the softHSM keystore inside the container, during startup. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. To insert/replace an existing PrivateKeyEntry of any particular algorithm, refer to section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore). To insert new certificate/private-key pair into \<fdo-pri-src\>/component-samples/demo/manufacturer/manufacturer_keystore.p12.

**IMPORTANT** This is an example implementation using simplified credentials. This must be changed while performing production deployment

# Configuring Manufacturer for HTTPS/TLS Communication

By default, the PRI-Manufacturer uses HTTP for all communications on port 8039. In addition to that, the PRI-Manufacturer can be configured to handle HTTPS request from the device.

- Generate the Keystore/Certificate for the PRI-manufacturer. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

  * Ensure that the web certificate is issued to the resolvable domain of the Manufacturer server.

- Copy the generated Keystore/Certificate to `<fdo-pri-src>/component-samples/demo/manufacturer/certs` folder.

- Update the following environment varibles in `<fdo-pri-src>/component-samples/demo/manufacturer/manufacturer.env` file

    |  Variable            |  Value            |             description       |
    | ---------------------|-------------------|-------------------------------|
    | manufacturer_protocol_scheme  | https             | To enable HTTPS communication.|
    | manufacturer_https_port | Port for HTTPS communication | The given port number will be opened for HTTPS communication. |
    | manufacturer_ssl_keystore     | keystore-filename | filename of Keystore that is present in the certs folder.|
    | manufacturer_ssl_keystore-password| keystore-password | password of the keystore. |

    **NOTE:** Appropriate security measures with respect to key-store management should be considered while performing production deployment of Manufacturer.
    Avoid using the default keystore available for production deployment.

# Rendezvous Info
Commonly referred as RvInfo, is one of the most important configuration of FDO. RvInfo is specified in `MT_SETTINGS` table in the manufacturer storage. It is consumed by device for performing TO1 and by owner through the ownership voucher for performing TO0. Default RvInfo value is: `http://localhost:8040?ipaddress=127.0.0.1&ownerport=8443`

This value is interpreted internally as:

RvInfo for Device: `http://localhost:8040`, `http://127.0.0.1:8040`

RvInfo for Owner: `https://localhost:8443`, `https://127.0.0.1:8443`

**NOTE** The "http" directive is for device only as the spec dictates that TO0 should always take place over `HTTPS`, irrespective of the http directive used by the device. User can specify any number of RvInfo separated by space. Both device and owner will recursively try each IPaddress and / or DNS address specified in the RvInfo till it reaches an active server with which it can complete the respective Transfer Ownership Protocol.