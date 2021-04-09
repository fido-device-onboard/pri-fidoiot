# System Requirements:

* **Ubuntu 20.04**.
* **Maven**.
* **Java 11**.
* **Haveged**.
* **SoftHSM**.

# Getting the executable

Use the following commands to build FIDO Device Onboard (FDO) Protocol Reference Implementation
(PRI) Reseller component sample source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/component-samples/reseller/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/reseller/.

# Configuring the FDO PRI Reseller Sample

Some required runtime arguments

- `reseller_api_port`

  Reseller server port.

  Default value: 8070

- `reseller_database_connection_url`

  JDBC URL for connection to database. Includes the database driver name, port number for database, and the location of `.db` file

  Default value: jdbc:h2:tcp://localhost:8071/./target/data/reseller

- `reseller_database_username`

  Reseller database username.

  Default value: sa

- `reseller_database_password`

  Reseller database password.

  Default value: <no-password>

- `reseller_database_port`

  Reseller database port number.

  Default value: 8071

- `reseller_database_driver`

  Reseller database driver.

  Default value: org.h2.Driver

- `reseller_api_user`

  Reseller API username.

  Default value: admin

- `reseller_api_password`

  Reseller API password.

  Default value: test

- `reseller_home`

  Tomcat configuration for catalina home.

  Default value: catalina.home

- `reseller_keystore_type`

  Reseller keystore type

  Default value: PKCS11

- `reseller_keystore_password`

  Reseller keystore password

  Default value: 123456

- `user.dir`

  Base path for jdbc `.db` file location.

- `reseller_protocol_scheme`

  Enables the service to run in https mode. Pass argument value `https` for the same. For all other values, the server service defaults to `http` scheme.

  Default value: http

- `reseller_https_port`

  Allows enduser to select a port for accepting HTTPS requests.
  **NOTE** This property is not required if service is running in `http` mode.

  Default value: 443

- `reseller_ssl_keystore`

  Provides path for SSL keystore to be used by the service, in case it runs in HTTPS mode.
  **NOTE** This property is not required if service is running in `http` mode.

  Default value: <fdo-pri-src>/component-samples/demo/reseller/certs/ssl.p12

- `reseller_ssl_keystore_password`

  Provides password for the specified keystore.
  **NOTE** This property is not required if service is running in `http` mode.

  Default keystore password: fdo123

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `<fdo-pri-src>/component-samples/reseller/src/main/java/org/fidoalliance/fdo/sample/ResellerServerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <reseller_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Starting the Reseller service

Refer the [Docker Commands](../README.md/#docker-commands) to start the service.

***NOTE*** The database file located at \<fdo-pri-src\>/component-samples/demo/reseller/target/data/reseller.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FDO PRI Reseller REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| GET /api/v1/resell/vouchers/<serial_number>?id=<customer_id> | Assigns the customer and returns the extended Ownership Voucher for the given serial number from `RT_DEVICES` table. | Query - id: Customer Id, Path - Device Serial Number | | | Ownership Voucher |
| POST /api/v1/resell/vouchers/<serial_number> | Adds Ownership Voucher to `RT_DEVICES` table against the serial number. | Path - Device Serial Number | application/cbor | Ownership Voucher | |
| DELETE /api/v1/resell/vouchers/<serial_number> | Deletes Ownership Voucher from `RT_DEVICES` table with the specified serial number. | Path - Device Serial Number | | | |
| POST /api/v1/resell/customers/?id=<customer_id>&name=<customer_name> | Adds customer keyset to `RT_CUSTOMERS` table. | Query - id: Customer Id, name: Customer Name | text/plain; charset=us-ascii | Customer PEM formatted Public keys | |
| DELETE /api/v1/resell/customers/<customer_id> | Deletes customer keyset from `RT_CUSTOMERS` table. | Path - Customer Id | | | |
| POST /api/v1/resell/keys/?alias=<keystore_alias> | Adds new Reseller keys to the keystore with the given alias | Query - alias: Alias to be added in keystore | | PEM formatted certificate and private key | |
| DELETE /api/v1/resell/keys/?alias=<keystore_alias> | Deletes the keys corresponding to the input alias from keystore. | Query - alias: Alias to be removed from keystore | | | |

***NOTE*** These REST APIs use Digest authentication. `reseller_api_user` and `reseller_api_password` properties specify the credentials to be used while making the REST calls.

# Inserting keys into Reseller keystore

The PKCS12 keystore file \<fdo-pri-src\>/component-samples/demo/reseller/reseller_keystore.p12 contains the default reseller keys that are imported into the softHSM keystore inside the container, during startup. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. To insert/replace an existing PrivateKeyEntry of any particular algorithm, refer to section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore) to insert new certificate/private-key pair into \<fdo-pri-src\>/component-samples/demo/reseller/reseller_keystore.p12.

**IMPORTANT** This is an example implementation using simplified credentials. This must be changed while performing production deployment

# Configuring Reseller for HTTPS/TLS Communication

By default, the Reseller uses HTTP for all communications on port 8070. In addition to that, the Reseller can be configured to handle HTTPS request.

- Generate the Keystore/Certificate for the Reseller. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

  * Ensure that the web certificate is issued to the resolvable domain of the Reseller server.

- Copy the generated Keystore/Certificate to `reseller/certs` folder.

- Update the following environment varibles in `reseller/reseller.env` file

    |  Variable            |  Value            |             description       |
    | ---------------------|-------------------|-------------------------------|
    | reseller_protocol_scheme  | https             | To enable HTTPS communication.|
    | reseller_https_port       | port number       | The given port number will be used for HTTPS communication. |
    | reseller_ssl_keystore     | keystore-filename | filename of Keystore that is present in the `reseller/certs` folder.|
    | reseller_ssl_keystore_password| keystore-password | password of the keystore. |

    **NOTE:** Appropriate security measures with respect to key-store management should be considered while performing production deployment of Reseller.
    Avoid using the default keystore available for production deployment.
