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

## Support for OnDie devices

Refer to [Demo README](../README.md) for steps to configure owner to support OnDie devices.

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
| POST /api/v1/owner/svivalues/?id=\<serviceinfo_id> | Adds ServiceInfo entry to `OWNER_SERVICEINFO` table. | Query - id: ServiceInfo ID | application/octet-stream or application/cbor | Content of Serviceinfo in binary format. | |
| DELETE /api/v1/owner/svi/?guid=<device_guid> | Deletes owner ServiceInfo for the GUID from the `GUID_OWNERSVI` table. | Query - guid: Device GUID | | | |
| POST /api/v1/owner/svi/settings/ | Updates the various fields of `TO2_SETTINGS` table for ID=1 field.<br/> Example input looks like 'devicemtu:=2000,ownerthreshold:=8192',wgetModContentVerification:=<boolean> <br/> For field: wgetModContentVerification, acceptable input values: true or false <br/> True: To enable content verification. <br/> False: To opt out of content verification| | application/text| values based on the field(s) to be modified.| |
| POST /api/v1/owner/setupinfo/?id=current_guid | updates `Replacement GUID` or `Replacement RVInfo` or both in TO2_DEVICES table | Query - guid: current device GUID| application/text | New GUID or New RV_Info or both. <br/> To update both GUID and RV Info: guid:=\<replacement_guid\>,rvinfo:=\<replacement_rvinfo\> <br/> To update Replacement GUID: guid:=\<replacement_guid\> <br/> To update Replacement RV_Info: rvinfo:=\<replacement_rvinfo\>| | |
| GET /api/v1/owner/newvoucher/?id=<device_guid> | Returns the replacement Ownership Voucher for the GUID if TO2 is completed and resale/non-resale was selected. | Query - id: Device GUID | | | Ownership Voucher |
| POST /api/v1/owner/setupinfo?id=<device_guid> | Updates the replacement RendezvousInfo, GUID and customer ID (for owner2 key) for the device. The setupinfo sring format is 'guid:=<replacement_device_guid_string>,rvinfo:=<replacement_rv_info_string>,ownerkey:=<customer_id>'. Both 'guid', 'rvinfo' and 'ownerkey' are optional. An example setupinfo string looks like 'guid:=64612afb-4ad9-4c69-a7d1-1cb1378157ec,rvinfo:=http://localhost:8040?ipaddress=127.0.0.1&ownerport=8040,ownerkey:=2'. | Query - id: Device GUID | application/text | Setupinfo string | |
| POST /api/v1/owner/svi/string/ | Adds owner serviceinfo string for each device type | | application/text | devicetype followed by the owner svi string separated by space | |
| DELETE /api/v1/owner/svi/string/?devicetype=<device_type> | Deletes device entry from `DEVICE_TYPE_OWNERSVI_STRING` table. | Query - devicetype: Device Type | | | |
| POST /api/v1/owner/devicetype/criteria/ | Adds criteria for identifying device type based on the DSI keys sent by the device. **NOTE** The first device type for which all criteria matches is determined as the device type for the current device. Recommended to add more restrictive device criteria first followed by less restrictive ones | | application/text | Device type followed by DSI keys followed by expected values separated by space | |
| DELETE /api/v1/owner/devicetype/criteria/?devicetype=<device_type> | Deletes device entry from `DEVICE_TYPE_OWNERSVI_CRITERIA` table. | Query - devicetype: Device Type  | | | |
| POST /api/v1/owner/customer/?id=<customer_id>&name=<customer_name> | Adds customer with the given ID and Public key in PEM format. | Query - id: Customer Id, name: Customer Name | text/plain; charset=us-ascii | Customer PEM formatted Public keys | |
| DELETE /api/v1/owner/customer/?id=<customer_id> | Deletes device entry from `OWNER_CUSTOMERS` table. | Query - id: Customer Id  | | | |


***NOTE*** These REST APIs use Digest authentication. `owner_api_user` and `owner_api_password` properties specify the credentials to be used while making the REST calls.

# Inserting keys into Owner keystore

The PKCS12 keystore file \<fido-iot-src\>/demo/owner/owner_keystore.p12 contains the default Owner keys. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. To insert/replace an existing PrivateKeyEntry of any particular algorithm, refer to section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore) to insert new certificate/private-key pair into \<fido-iot-src\>/demo/owner/owner_keystore.p12.

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
