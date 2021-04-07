# System Requirements:

* **Ubuntu 20.04**.
* **Maven**.
* **Java 11**.
* **Haveged**.

# Getting the executable

Use the following commands to build FIDO Device Onboard (FDO) Owner Component sample source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/component-samples/owner/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/owner/.

# Configuring the FDO PRI Owner Sample

Owner runtime arguments:

- `owner_to2_port`

  Owner server port.

  Default value: 8042

- `owner_database_connection_url`

  JDBC URL for connection to database. Includes the database driver name, port number for database, and the location of `.db` file.

  Default value: jdbc:h2:tcp://localhost:8051/./target/data/ops

- `owner_database_username`

  Owner database username.

  Default value: sa

- `owner_database_password`

  Owner database password.

  Default value: <no-password>

- `owner_database_port`

  Owner database port number.

  Default value: 8051

- `epid_online_url`

  EPID Verification Service URL for EPID device signature verification.

  Default value: https://verify.epid-sbx.trustedservices.intel.com/
  Other server options: https://verify.epid.trustedservices.intel.com/ (production EPID verification server), https://localhost:1180 (onprem verification service)

- `epid_test_mode`

   EPID devices can be tested using `Test` mode, it is intended for supporting onboarding for `development` and `test` devices. Enabling the test mode means signature verification won't be performed for the device.

   Default value: false

   **NOTE** Not recommended for use in production systems.

- `catalina_home`

  Tomcat configuration for Cataline home.

  Default value: ./target/tomcat

- `owner_keystore`

  Path to the Owner keystore file containing the owner's keys.

  Default value: ./owner_keystore.p12

- `owner_keystore_password`

  Keystore password for owner_keystore.p12 and the internal softHSM's PKCS11 keystore.

  Default value: OnrKstr1

- `owner_to0_scheduling_enabled`

  Schedule eligible devices for TO0, as present in `TO2_DEVICES` table. If true, automatic TO0 scheduling is enabled. If false, TO0 scheduling is disabled.

  Default value: true

- `owner_to0_scheduling_interval`

  Time interval to check database for GUIDs with pending TO0.

  Default value: 300s

- `owner_to0_rv_blob`

  Information containing network address of prospective owner. Owner shares this information with RV during TO0. RV, then shares the same during TO1. Device, then uses this information to initiate TO2 protocol.

  Default value: http://localhost:8042?ipaddress=127.0.0.1

- `owner_api_user`

  Username for the database REST API calls.

  Default value: apiUser

- `owner_api_password`

  Password for the database REST API calls.

  Default value: OwnerApiPass123

- `owner_protocol_scheme`

  Enables the service to run in https mode. Pass argument value `https` for the same. For all other values, the server service defaults to `http` scheme.

  Default value: https

- `owner_https_port`

   Allows enduser to select a port for accepting HTTPS requests.
  **NOTE** This property is not required if service is running in `http` mode.

  Default value: 443

- `fido_ssl_mode`

  There are 2 modes namely 'TEST' and 'PROD'. TEST mode disables SSL verification.

  Default value: TEST

- `owner_ssl_keystore`

  Provides path for SSL keystore to be used by the service, in case it runs in HTTPS mode.
  **NOTE** This property is not required if service is running in `http` mode.

  Default value: <fdo-pri-src>/component-samples/demo/owner/certs/ssl.p12

- `owner_ssl_keystore_password`

  Provides password for the specified keystore.
  **NOTE** This property is not required if service is running in `http` mode.

  Default keystore password: fdo123

- `ssl_truststore`

  Provides path for SSL truststore to be used by the service.
  **NOTE** This property is not required if service is running in `http` mode.

  Default value: <fdo-pri-src>/component-samples/demo/owner/certs/truststore

- `ssl_truststore_password`

  Provides password for the specified truststore.
  **NOTE** This property is not required if service is running in `http` mode.

  Default keystore password: fdo123

- `ssl_truststore_type`

  Defines the type of truststore.
  Default truststore type: PKCS12


## Support for OnDie devices

Refer to [Demo README](../README.md) for steps to configure owner to support OnDie devices.

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `\<fdo-pri-src>/component-samples/owner/src/main/java/org/fidoalliance/fdo/sample/OwnerServerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <owner_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Starting the owner service

Refer to the section [Docker Commands](../README.md/#docker-commands) to start the service.

***NOTE*** The database file located at \<fdo-pri-src\>/component-samples/demo/owner/target/data/ops.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FDO PRI Owner REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| GET /api/v1/owner/vouchers/    | Returns all GUID of Ownership Voucher available in `TO2_DEVICES` table. | | | | Comma-separated list of GUIDs |
| GET /api/v1/owner/vouchers/?id=<device_guid> | Returns the Ownership Voucher for the specified GUID. | Query - id: Device GUID | | | Ownership Voucher |
| POST /api/v1/owner/vouchers/ | Insert Ownership Voucher against the specified GUID in `TO2_DEVICES` table. | | application/cbor | Content of Ownership Voucher in binary format | |
| DELETE /api/v1/owner/vouchers/?id=<device_guid> | Deletes Ownership Voucher of the specified GUID from the `TO2_DEVICES` table. | Query - id: Device GUID | | | |
| GET /api/v1/owner/newvoucher/?id=<device_guid> | Returns the new Ownership Voucher for the specified GUID to enable resale. | Query - id: Device GUID | | | Ownership Voucher |
| POST /api/v1/owner/svi/settings/ | Updates the various fields of `TO2_SETTINGS` table for ID=1 field.<br/> Example input looks like 'devicemtu:=2000,ownerthreshold:=8192',wgetModContentVerification:=<boolean> <br/> For field: wgetModContentVerification, acceptable input values: true or false <br/> True: To enable content verification. <br/> False: To opt out of content verification| | application/text| values based on the field(s) to be modified.| |
| POST /api/v1/owner/setupinfo/?id=current_guid | updates `Replacement GUID` or `Replacement RVInfo` or both in TO2_DEVICES table | Query - guid: current device GUID| application/text | New GUID or New RV_Info or both. <br/> To update both GUID and RV Info: guid:=\<replacement_guid\>,rvinfo:=\<replacement_rvinfo\> <br/> To update Replacement GUID: guid:=\<replacement_guid\> <br/> To update Replacement RV_Info: rvinfo:=\<replacement_rvinfo\>| | |
| POST /api/v1/owner/customer/?id=<customer_id>&name=<customer_name> | Adds customer with the given ID and Public key in PEM format. | Query - id: Customer Id, name: Customer Name | text/plain; charset=us-ascii | Customer PEM formatted Public keys | |
| DELETE /api/v1/owner/customer/?id=<customer_id> | Deletes device entry from `OWNER_CUSTOMERS` table. | Query - id: Customer Id  | | | |
| GET /api/v1/device/svi/?guid=<guid> | Retieves tag information about a resoruce including id. | Query - guid: Device GUID  | | | |
| PUT /api/v1/device/svi/?id=<customer_id> | Adds a new tagged resource. | Query - id: Customer Id.  | Query - module: Module Name. <br/>  var: Message Name. <br/> filename: zero length file to be created on device with the given filename. <br/> bytes: content to be populated in file, specified using filename. <br/> guid: Tag resource using GUID. <br/> device: device type tag. <br/> priority: priority order to send messages to device. <br/> os: os name tag. <br/> version: os version tag. <br/> arch: device architecture tag. <br/> crid : content resource identifier tag. <br/> hash: storing hash value of content. **NOTE** Resource will be transferred only to devices matching the tagged architecture version type.| | |
| POST /api/v1/device/svi/?id=<resource_id> | Updates the content of existing resource. | Query - id: Resource Id  | | | |
| DELETE /api/v1/device/svi/?id=<resource_id> | Removes resource(s) by tag or id. | Query - id: Resource Id  | | | |


***NOTE*** These REST APIs use Digest authentication. `owner_api_user` and `owner_api_password` properties specify the credentials to be used while making the REST calls.

# Inserting keys into Owner keystore

The PKCS12 keystore file \<fdo-pri-src\>/component-samples/demo/owner/owner_keystore.p12 contains the default Owner keys. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. To insert/replace an existing PrivateKeyEntry of any particular algorithm, refer to section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore) to insert new certificate/private-key pair into \<fdo-pri-src\>/component-samples/demo/owner/owner_keystore.p12.

**IMPORTANT** This is an example implementation using simplified credentials. This must be changed while performing production deployment

***NOTE*** A 'PKCS12' keystore is used to store the keys, instead of 'PKCS11' keystore (softHSM). This is because of use of 'BouncyCastle' as a security provider for algorithm 'RSA/NONE/OAEPWithSHA256AndMGF1Padding' to support Asymmetric key exchange, since the security provider 'SUNPKCS11' configured with softHSM, does not support the same as per the available documentation.

# Troubleshooting

As the H2 DB grows, larger heap space will be required by the application to run the service. Default configured heap size is `256 MB`. Increase the heap size appropriately in `demo/owner/owner-entrypoint.sh` to avoid heap size issues.

# Configuring Owner for HTTPS/TLS Communication

By default, the Owner uses HTTP for all communications on port 8042. In addition to that, the Owner can be configured to handle HTTPS request from the device.

- Generate the Keystore/Certificate for the Owner. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

  * Ensure that the web certificate is issued to the resolvable domain of the Owner server.

- Copy the generated Keystore/Certificate to `demo/owner/certs` folder.

- Copy the truststore containing all the required certificates to `demo/owner/certs` folder.

- Update the following environment varibles in `demo/owner/owner.env` file

    |  Variable              |  Value            |             Description       |
    | -----------------------|-------------------|-------------------------------|
    | owner_protocol_scheme  | https             | To enable HTTPS communication.|
    | owner_https_port       | port number       | The given port will be used for HTTPS communication. |
    | fido_ssl_mode          | TEST / PROD       | If set to `TEST`, then SSL verification is disabled. If set to `PROD`, then certificate verification is initiated. |
    | owner_ssl_keystore     | keystore-filename | Filename of Keystore that is present in the certs folder.|
    | owner_ssl_keystore-password| keystore-password | Password of the keystore. |
    | ssl_truststore         | truststore-filename  | Filename of truststore that is present in the certs folder. Not required in `TEST` mode. |
    | ssl_truststore_password| truststore-password | Password of the truststore. Not required in `TEST` mode. |
    | ssl_truststore_type    | truststore-type   | Type of truststore. eg: JKS ,PKCS12   |
    | owner_to0_rv_blob      | to0_rv_blob       | Contains the to0_rv_blob used by device to connect with the Owner during T02. Eg: https://localhost:\<owner-https-port\>?ipaddress=127.0.0.1 |

    **NOTE:** Appropriate security measures with respect to key-store management should be considered while performing production deployment of Owner.
    Avoid using the default keystore available for production deployment.
