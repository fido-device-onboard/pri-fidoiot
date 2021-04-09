# System Requirements:

* **Ubuntu 20.04**.
* **Maven**.
* **Java 11**.
* **Haveged**.

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

  Username for the non-SDO REST API calls.

  Default value: apiUser

- `owner_api_password`

  Password for the non-SDO REST API calls.

  Default value: OwnerApiPass123

- `owner_svi_values`

  Path to the directory that contains default sample owner ServiceInfo values. The filenames are used as identifiers in the database, while the actual file content is the requisite ServiceInfo that is transferred to the device. Only used for demo purposes and should not be modified.

  Default value: ./serviceinfo/sample-values

- `owner_svi_string`

  Path to the file that contains default sample svi string that maps ServiceInfo values to module names and messages. Only used for demo purposes and should not be modified.

  Docker default: ./serviceinfo/sample-svi.csv

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `<fido-iot-src>/service/component-samples/owner/src/main/java/org/fido/iot/sample/OwnerServerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <owner_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Starting the owner service

Refer to the section [Docker Commands](../README.md/#docker-commands) to start the service.

***NOTE*** The database file located at \<fido-iot-src\>/demo/owner/target/data/ops.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FIDO IoT Owner REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| GET /api/v1/owner/vouchers/    | Returns all GUID of Ownership Voucher available in `TO2_DEVICES` table. | | | | Comma-separated list of GUIDs |
| GET /api/v1/owner/vouchers/?id=<device_guid> | Returns the Ownership Voucher for the specified GUID. | Query - id: Device GUID | | | Ownership Voucher |
| POST /api/v1/owner/vouchers/ | Insert Ownership Voucher against the specified GUID in `TO2_DEVICES` table. | | application/cbor | Content of Ownership Voucher in binary format | |
| DELETE /api/v1/owner/vouchers/?id=<device_guid> | Deletes Ownership Voucher of the specified GUID from the `TO2_DEVICES` table. | Query - id: Device GUID | | | |
| GET /api/v1/owner/newvoucher/?id=<device_guid> | Returns the new Ownership Voucher for the specified GUID to enable resale. | Query - id: Device GUID | | | Ownership Voucher |
| POST /api/v1/owner/svivalues/?id=\<serviceinfo_id>&isCborEncoded=<boolean_value> | Adds ServiceInfo entry to `OWNER_SERVICEINFO` table. The query parameter 'isCborEncoded' should be 'true' for CBOR encoded binary data that will never be split into smaller chunks while transferring to the device (small in length, to be used for CBOR primitives such as boolean, int, array and map), and it should be 'false' (preferably) for other binary data that could be split into smaller chunks and transferred across messages (for example binary values, string). | Query - id: ServiceInfo ID, isCborEncoded: Boolean | application/octet-stream or application/cbor | Content of Serviceinfo in binary format. | |
| DELETE /api/v1/owner/svivalues/?id=<serviceinfo_id> | Deletes the ServiceInfo values from `OWNER_SERVICEINFO` table. | Query - id: ServiceInfo ID | | | |
| POST /api/v1/owner/svi/?guid=<device_guid> | Adds owner ServiceInfo for the GUID in `GUID_OWNERSVI` table, that will be transferred to the device in the insertion order. The format is `Entry1,Entry2,Entry3` and so on, where each Entry contains `moduleName:messageName=serviceInfoId`. Here, the 'content' corresponding to the 'serviceInfoId' is transferred to the device. Please see \<fido-iot-src\>/demo/owner/serviceinfo/sample-svi.csv as an example for the above format, where moduleName is 'sdo_sys' and messageName is either one of 'filedesc', 'write' and 'exec'. The order of each 'Entry' is important as this order decides the sequence in which the Owner will transfer the ServiceInfo. | Query - guid: Device GUID | application/text | SVI string | |
| DELETE /api/v1/owner/svi/?guid=<device_guid> | Deletes owner ServiceInfo for the GUID from the `GUID_OWNERSVI` table. | Query - guid: Device GUID | | | |
| POST /api/v1/owner/setupinfo/?id=current_guid | updates `Replacement GUID` or `Replacement RVInfo` or both in TO2_DEVICES table | Query - guid: current device GUID| application/text | New GUID or New RV_Info or both. <br/> To update both GUID and RV Info: guid:=\<replacement_guid\>,rvinfo:=\<replacement_rvinfo\> <br/> To update Replacement GUID: guid:=\<replacement_guid\> <br/> To update Replacement RV_Info: rvinfo:=\<replacement_rvinfo\>| | |
| GET /api/v1/owner/newvoucher/?id=<device_guid> | Returns the replacement Ownership Voucher for the GUID if TO2 is completed and resale/non-resale was selected. | Query - id: Device GUID | | | Ownership Voucher |
| POST /api/v1/owner/setupinfo?id=<device_guid> | Updates the replacement RendezvousInfo and GUID for the device. The setupinfo sring format is 'guid:=<replacement_device_guid_string>,rvinfo:=<replacement_rv_info_string>'. Both 'guid' and 'rvinfo' are optional. An example setupinfo string looks like 'guid:=64612afb-4ad9-4c69-a7d1-1cb1378157ec,rvinfo:=http://localhost:8040?ipaddress=127.0.0.1&ownerport=8040'. | Query - id: Device GUID | application/text | Setupinfo string | |

***NOTE*** These REST APIs use Digest authentication. `owner_api_user` and `owner_api_password` properties specify the credentials to be used while making the REST calls.

# Inserting keys into Owner keystore

The PKCS12 keystore file \<fido-iot-src\>/demo/owner/owner_keystore.p12 contains the default Owner keys. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. To insert/replace an existing PrivateKeyEntry of any particular algorithm, refer to section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore) to insert new certificate/private-key pair into \<fido-iot-src\>/demo/owner/owner_keystore.p12.

**IMPORTANT** This is an example implementation using simplified credentials. This must be changed while performing production deployment

***NOTE*** A 'PKCS12' keystore is used to store the keys, instead of 'PKCS11' keystore (softHSM). This is because of use of 'BouncyCastle' as a security provider for algorithm 'RSA/NONE/OAEPWithSHA256AndMGF1Padding' to support Asymmetric key exchange, since the security provider 'SUNPKCS11' configured with softHSM, does not support the same as per the available documentation.

# Troubleshooting

As the H2 DB grows, larger heap space will be required by the application to run the service. Default configured heap size is `256 MB`. Increase the heap size appropriately in `demo/owner/owner-entrypoint.sh` to avoid heap size issues.