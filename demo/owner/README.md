# Getting the executable

Use the following commands to build FIDO IoT Owner Component sample source.
```
$ cd <fido-iot-src>/service/component-samples/owner/
$ mvn clean install
```

This will copy the required executables and libraries into \<fido-iot-src\>/demo/owner/.

# Configuring the FIDO IoT Owner Sample

Some required runtime arguments

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

  RV database port number.

  Default value: 8050

- `epid_online_url`

  EPID Verification Service URL for EPID device signature verification.

  Default value: https://verify.epid-sbx.trustedservices.intel.com/
  Other server options: https://verify.epid.trustedservices.intel.com/ (production EPID verification server), https://localhost:1180 (onprem verification service)

- `catalina_home`

  Tomcat configuration for cataline home.

  Default value: ./target/tomcat

- `owner_keystore_password`

  Keystore password for owner_keystore.p12 and the internal softHSM's PKCS11 keystore.

  Default value: OnrKstr1

- `owner_to0_scheduling_enabled`

  Auto completes TO0 for GUIDs with DI complete state.

  Default value: false

- `owner_to0_scheduling_interval`

  Time interval to check database for GUIDs with pending TO0.

  Default value: 300s

- `owner_to0_rv_blob`

  Information containing network address of prospective owner. Owner shares this information with RV during TO0. RV, then shares the same during TO1. Device, then uses this information to initiate TO2 protocol.

  Default value: http://localhost:8042?ipaddress=127.0.0.1

- `owner_api_user`

  Username for the non-SDO REST API calls.

  Default value: apiUser

- `owner_api_password`

  Password for the non-SDO REST API calls.

  Default value: OwnerApiPass123

- `owner_svi_values`

  Path to the directory that contains default sample owner serviceinfo values. The filenames are used as identifiers in the database, while the actual file content is the requisite serviceinfo that is transferred to the device. Only used for demo purposes and should not be modified.

  Default value: ./serviceinfo/sample-values

- `owner_svi_string`

  Path to the file that contains default sample svi string that maps serviceinfo values to module names and messages. Only used for demo purposes and should not be modified.

  Docker default: ./serviceinfo/sample-svi.csv

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `<fido-iot-src>/service/component-samples/owner/src/main/java/org/fido/iot/sample/OwnerServerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <owner_db_port>
webAllowOthers = true
```

**IMPORTANT: NOT recommended to enable this setting especially on production systems.**

# Starting the owner service

Refer to the section [Docker Commands](../README.md/#docker-commands) to start the service.

***NOTE*** The database file located at \<fido-iot-src\>/demo/owner/target/data/ops.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FIDO IoT Owner REST APIs

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

# Inserting keys into Owner keystore

The PKCS12 keystore file \<fido-iot-src\>/demo/owner/owner_keystore.p12 contains the default Owner keys that are imported into the softHSM keystore inside the container, during startup. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. To insert/replace an existing PrivateKeyEntry of any particular algorithm, refer to section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore) to insert new certificate/private-key pair into \<fido-iot-src\>/demo/owner/owner_keystore.p12.

**IMPORTANT** This is an example implementation using simplified credentials. This must be changed while performing production deployment