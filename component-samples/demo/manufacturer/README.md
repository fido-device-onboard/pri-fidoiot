# Getting the Executable

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

   JDBC URL for database connection. Includes the database driver name, port number for database and the location of `.db` file

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

  Allows end user to select a port for accepting HTTPS requests.
  ***NOTE***: This property is not required if service is running in `http` mode.

  Default value: 443

- `manufacturer_ssl_keystore`

  Provides path for SSL keystore to be used by the service, in case it runs in HTTPS mode.
  ***NOTE***: This property is not required if service is running in `http` mode.

  Default value: <fdo-pri-src>/component-samples/demo/manufacturer/certs/ssl.p12

- `manufacturer_ssl_keystore-password`

  Provides password for the specified keystore.
  ***NOTE***: This property is not required if service is running in `http` mode.

  Default keystore password: fdo123

## Support for OnDie Devices

Refer to [Demo README](../README.md/#configuring-ondie-optional) for steps to configure manufacturer to support OnDie devices.

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `<fdo-pri-src>/component-samples/manufacturer/src/main/java/org/fidoalliance/fdo/sample/ManufacturerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <manufacturer_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Starting the Manufacturer Service

Refer the [Docker Commands](../README.md/#docker-commands) / [Podman Commands](../README.md/#podman-commands) to start the service.

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/manufacturer/target/data/mfg.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FDO PRI Manufacturer REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| POST /api/v1/assign/?id=<customer_id>&serial=<serial_no> | Assigns customer ID to Ownership Voucher having the input serial no. | Query - id: Customer ID, serial = Device serial number | | | |
| GET /api/v1/vouchers/<serial_no> | Gets extended Ownership Voucher with the serial number. | Path - Device Serial Number | | | Ownership Voucher |
| POST /api/v1/customers/?id=<customer_id>&name=<customer_name> | Adds customer with the given ID and Public key in PEM format. | Query - id: Customer Id, name: Customer Name | text/plain; charset=us-ascii | Customer PEM formatted Public keys | |
| POST /api/v1/rvinfo/ | Updates RV Info in `MT_SETTINGS` table | | text/plain; charset=us-ascii | RV Info | | |


# Inserting Keys into Manufacturer Keystore

The PKCS12 keystore file \<fdo-pri-src\>/component-samples/demo/manufacturer/manufacturer_keystore.p12 contains the default manufacturer keys that are imported into the softHSM keystore inside the container, during startup. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. Refer to section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore) to insert/replace a PrivateKeyEntry of any particular algorithm in \<fdo-pri-src\>/component-samples/demo/manufacturer/manufacturer_keystore.p12.

**IMPORTANT** This is an example implementation using simplified credentials. This must be changed while performing production deployment

# Configuring Manufacturer for HTTPS/TLS Communication

By default, the PRI-Manufacturer uses HTTP for all communications on port 8039. In addition to that, the PRI-Manufacturer can be configured to handle HTTPS requests from the device.

- Generate the Keystore/Certificate for the PRI-manufacturer. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

  * Ensure that the web certificate is issued to the resolvable domain of the Manufacturer server.

- Copy the generated Keystore/Certificate to `<fdo-pri-src>/component-samples/demo/manufacturer/certs` folder.

- Update the following environment variables in `<fdo-pri-src>/component-samples/demo/manufacturer/manufacturer.env` file

    |  Variable            |  Value            |             description       |
    | ---------------------|-------------------|-------------------------------|
    | manufacturer_protocol_scheme  | https             | To enable HTTPS communication.|
    | manufacturer_https_port | Port for HTTPS communication | The given port number will be opened for HTTPS communication. |
    | manufacturer_ssl_keystore     | keystore-filename | filename of Keystore that is present in the certs folder.|
    | manufacturer_ssl_keystore-password| keystore-password | password of the keystore. |

    ***NOTE***: Appropriate security measures with respect to key-store management should be considered while performing production deployment of Manufacturer.
    Avoid using the default keystore available for production deployment.

# Rendezvous Info
Commonly referred as RvInfo, is one of the most important configurations of FDO. RvInfo is specified in `MT_SETTINGS` table in the manufacturer storage. It is consumed by device for performing TO1 and by owner through the ownership voucher for performing TO0. The default diagnostic representation of the RvInfo value is: `[[[5, "localhost"], [3, 8040], [12, 1], [2, h'7F000001'], [4, 8443]]]` which points to localhost over the port 8443 for Owner during TO0 and localhost over the port 8040 for device during TO1
and the equivalent bytes representation is `81858205696c6f63616c686f73748203191f68820c018202447f00000182041920fb`. In the following section, we will be discussing on the generation of bytes representation from the CBOR diagnostic representation.

## Generating CBOR RVInfo

As per the spec, a sample RendezvousInfo with one RendezvousInstrList is as follows:

```
[[[RVDns,"localhost"],
  [RVDevPort,8040],
  [RVProt, 1],
  [RVIPAddress, h’7F000001'],     //Represents 127.0.0.1
  [RVOwnerPort,8443]]]
```
and the equivalent diagnostic representation is:

```
[[[5, "localhost"], [3, 8040], [12, 1], [2, h'7F000001'], [4, 8443]]]
```
**NOTE:** In the spec, RVDns is represented as 5, RVDevPort as 3, RVProt as 12, RVIPAddress as 2 and RVOwnerPort as 4.
[Read more](https://fidoalliance.org/specs/FDO/fido-device-onboard-v1.0-ps-20210323/#RVInfo) about the RV variable representations.

**NOTE:** h'7F00001' is the hexadecimal representation of the ip address and it is interpreted as
```
7F -> 127
00 -> 0
00 -> 0
01 -> 1     ; 127.0.0.1
```

You can generate the equivalent byte value of the above CBOR representation by visiting [CBOR playground](cbor.me).

On visiting CBOR playground, you will be presented with two text areas (Diagnostic and Bytes). Enter the diagnostic
representation `[[[5, "localhost"],[3,8040], .. ]` on the Diagnostic text area and click `→`. The bytes representation will be generated on the 'Bytes' textarea.

Sample bytes representation of the default RvInfo:
```
81                             # array(1)
   85                          # array(5)
      82                       # array(2)
         05                    # unsigned(5)
         69                    # text(9)
            6C6F63616C686F7374 # "localhost"
      82                       # array(2)
         03                    # unsigned(3)
         19 1F68               # unsigned(8040)
      82                       # array(2)
         0C                    # unsigned(12)
         01                    # unsigned(1)
      82                       # array(2)
         02                    # unsigned(2)
         44                    # bytes(4)
            7F000001           # "\x7F\x00\x00\x01"
      82                       # array(2)
         04                    # unsigned(4)
         19 20FB               # unsigned(8443)
```

From the above representation, you have to strip the whitespaces and comments(Starting with #) to the format `81858205696c6f63616c686f73748203191f68820c018202447f00000182041920fb`.

You can also make use of the `get-cbor-bytes.sh` script available in `component-samples/scripts` to generate the bytes
representation from the diagnostic representation.

This bytes value is interpreted internally as:

Directives for Device: `http://localhost:8040`, `http://127.0.0.1:8040`

Directives for Owner: `https://localhost:8443`, `https://127.0.0.1:8443`

***NOTE***: The "http" directive is for device only as the spec dictates that TO0 should always take place over `HTTPS`, irrespective of the http directive used by the device. User can specify any number of RvInfo separated by space. Both device and owner will recursively try each IPaddress and / or DNS address specified in the RvInfo till it reaches an active server with which it can complete the respective Transfer Ownership Protocol. [Read more](https://fidoalliance.org/specs/FDO/fido-device-onboard-v1.0-ps-20210323/#RVInfo) about RendezvousInfo.
