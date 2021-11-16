# Getting the Executable

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

  JDBC URL for database connection. Includes the database driver name, port number for database, and the location of `.db` file

  Default value: jdbc:h2:tcp://localhost:8071/./target/data/reseller

- `reseller_database_username`

  Reseller database username.

  Default value: sa

- `reseller_database_password`

  Reseller database password.

  Default value: `<no-password>`

- `reseller_database_port`

  Reseller database port number.

  Default value: 8071

- `reseller_database_driver`

  Reseller database driver.

  Default value: org.h2.Driver

- `reseller_api_user`

  Reseller API username.

  Default value: apiUser

- `reseller_api_password`

  Reseller API password.

  The value for this property is auto generated using the keys_gen.sh script and is stored in creds.env file.

- `reseller_home`

  Tomcat configuration for catalina home.

  Default value: catalina.home

- `reseller_keystore`

  Path to the reseller keystore file containing the Reseller's keys.

  The keystore file and path value is generated using keys_gen.sh script and is stored in creds.env file. [Read more](https://github.com/secure-device-onboard/pri-fidoiot/tree/master/component-samples/demo#preparing-credentials-for-components) about the key generation script here.


- `reseller_keystore_type`

  Reseller keystore type

  Default value: PKCS11

- `reseller_keystore_password`

  Reseller keystore password.

  The value for this property is auto generated using the keys_gen.sh script and is stored in creds.env file.

- `user.dir`

  Base path for jdbc `.db` file location.

- `reseller_protocol_scheme`

  Enables the service to run in https mode. Pass argument value `https` for the same. For all other values, the server service defaults to `http` scheme.

  Default value: http

- `reseller_https_port`

  Allows enduser to select a port for accepting HTTPS requests.

  ***NOTE***: This property is not required if service is running in `http` mode.

  Default value: 443

- `reseller_ssl_keystore`

  Provides path for SSL keystore to be used by the service, in case it runs in HTTPS mode.

  The keystore file and path value is generated using keys_gen.sh script and is stored in creds.env file.

  ***NOTE***: This property is not required if service is running in `http` mode.

- `reseller_ssl_keystore_password`

  Provides password for the specified keystore.

  The value for this property is auto generated using the keys_gen.sh script and is stored in creds.env file.

  ***NOTE***: This property is not required if service is running in `http` mode.

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `<fdo-pri-src>/component-samples/reseller/src/main/java/org/fidoalliance/fdo/sample/ResellerServerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <reseller_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Starting the Reseller Service

Refer the [Docker Commands](../README.md/#docker-commands) / [Podman Commands](../README.md/#podman-commands) to start the service.

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/reseller/target/data/reseller.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

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

***NOTE***: These REST APIs use Digest authentication. `reseller_api_user` and `reseller_api_password` properties specify the credentials to be used while making the REST calls.

# Inserting Keys into Reseller Keystore

The PKCS12 keystore file \<fdo-pri-src\>/component-samples/demo/reseller/reseller_keystore.p12 contains the default reseller keys that are imported into the softHSM keystore inside the container, during startup. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. The default reseller_keystore.p12 is generated using the key generation script keys_gen.sh and you can [read more](https://github.com/secure-device-onboard/pri-fidoiot/tree/master/component-samples/demo#preparing-credentials-for-components) about it here.

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

    ***NOTE***: Appropriate security measures with respect to key-store management should be considered while performing production deployment of Reseller.
    Avoid using the default keystore available for production deployment.
