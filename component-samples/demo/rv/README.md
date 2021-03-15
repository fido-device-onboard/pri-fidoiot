# System Requirements:

* **Ubuntu 20.04**.
* **Maven**.
* **Java 11**.
* **Haveged**.

# Getting the executable

Use the following commands to build FIDO IoT RV Component sample source.
```
$ cd <fido-iot-src>/service/component-samples/rv/
$ mvn clean install
```

This will copy the required executables and libraries into <fido-iot-src>/demo/rv/.

# Configuring the FIDO IoT RV Sample

Some required runtime arguments

- `rv_port`

  RV server port.

  Default value: 8040

- `rv_database_connection_url`

   JDBC URL for connection to database. Includes the database driver name, port number for database and the location of `.db` file

  Default value: jdbc:h2:tcp://localhost:8050/./target/data/rvs

- `rv_database_username`

  RV database username.

  Default value: sa

- `rv_database_password`

  RV database password.

  Default value: <no-password>

- `rv_database_port`

  RV database port number.

  Default value: 8050

- `epid_online_url`

  EPID Verification Service URL for EPID device signature verification.

  Default value: https://verify.epid-sbx.trustedservices.intel.com/
  Other server options: https://verify.epid.trustedservices.intel.com/ (production EPID verification server), https://localhost:1180 (onprem verification service)

- `epid_test_mode`

   EPID devices can be tested using `Test` mode, it is intended for supporting onboarding for `development` and `test` devices. Enabling the test mode means signature verification won't be performed for the device.

   Default value: false

   **NOTE** Not recommended for use in production systems.

- `catalina_home`

  Tomcat configuration for catalina home.

  Default value: ./target/tomcat

## Support for OnDie devices

Refer to [Demo README](../README.md) for steps to configure rendezvous to support OnDie devices.

# Enabling Remote Access to DB

Remote access to H2 Sample Storage DB has been disabled by default. Enabling the access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the `db.tcpServer` and `webAllowOthers` properties in `<fido-iot-src>/service/component-samples/rv/src/main/java/org/fido/iot/sample/RvServerApp.java` file

```
db.tcpServer = -tcp -tcpAllowOthers -ifNotExists -tcpPort <rv_db_port>
webAllowOthers = true
```

**IMPORTANT: Not recommended to enable this setting especially on production systems.**

# Starting the rv service

Refer the [Docker Commands](../README.md/#docker-commands) to start the service.

# Allowlist and Denylist Configuration

- RV provides the option to allow and deny requests based on the owner, manufacturer and reseller public keys and based on the GUID used in the Device Ownership Voucher
header.
- To add entries to these allowlist and denylist, update the `config.properties` file in `<fido-iot-src>/storage/storage-samples/storage-rv-sample/src/main/resources` location and rebuild the code and restart the docker or update the `config.properties` in `demo/rv` and restart the docker.
- Once updated, rebuild the code, rebuild RV docker image and then start the docker.
- The hashes for the default public keys present of owner, manufacturer and reseller are already added in allowlist configuration of component sample. The table below lists them.

  | Hashes in Allowlist | PRI-FIDOIOT Component |
  | --- | --- |
  | 42110E8F0F3184A1A5C51868BCBFF7144D66E41D1A188103C0264D5DA8BBCF88 | Manufacturer ECDSA 256 |
  | 25D42F0536CE584E5812AB8750E80E7464742B4B65347BEA90AD4BBC71D3FFA6 | Manufacturer ECDSA 384 |
  | 283ADF4CCB527C19A72CFB21A9FF7B555788E6B365CEF3A26C6B876EE0FFE017 | Manufacturer RSA 2048 |
  | 85A481BBC2DA15EDD7301FF92BA2BB60093D5864A8207F9D78A399B32AB4CFF4 | Reseller ECDSA 256 |
  | 31726603CB0751BFB926B6436369265557855744338FFC3307693E0D14D5241D | Reseller ECDSA 384 |
  | 2ED65928AD50CB8542E648B9CD5C8B4BFB76DA870C723B16464F49F5140F7098 | Reseller RSA 2048 |
  | 1DAC184C6A8BB2D00665F4CFC55B1F55AC9BFB4C899B06827C0C1990A1A0F74C | Owner ECDSA 256 |
  | 834F83875910C8507CE935BE2F947DCF854E6554C3ACB79893ACF91220EA5D8B | Owner ECDSA 384 |
  | 91984D7EE0BC1F153900401E6E0D0DC4F6F8472709AA1DAA9256429046C2E367 | Owner RSA 2048 |

- To add entries to allowlist and denylist for public key, public key hash need to be calculated. Refer [Generating Public Key Hash](#calculate-sha256-hash-of-a-public-key) for the steps to generate public key hash from public key.

**IMPORTANT** This is an example implementation using simplified credentials. This must be changed while performing production deployment

# Calculate SHA256 hash of a public key

```
$ openssl x509 -in cert.pem -pubkey -noout | openssl enc -base64 -d > example_allowlist_publickey.der

$ cat example_allowlist_publickey.der | openssl dgst -sha256 | awk '/s/{print toupper($2)}'
```

***NOTE:*** Input file cert.pem is an X509 certificate.

# Configuring RV for HTTPS/TLS Communication

By default, the RV uses HTTP for all communications on port 8040. In addition to that, the RV can be configured to handle HTTPS request from the owner & device on port 8443.

- Generate the Keystore/Certificate for the RV. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

  * Ensure that the web certificate is issued to the resolvable domain of the Rendezvous server. Refer the above section to generate the keystore.

- Copy the generated Keystore/Certificate to `demo/rv/certs` folder.

- Update the following environment varibles in `demo/rv/rv.env` file

    |  Variable            |  Value            |             description       |
    | ---------------------|-------------------|-------------------------------|
    | rv_protocol_scheme  | https             | To enable HTTPS communication.|
    | rv_https_port       | port number       | The given port number will be used for HTTPS communication. |
    | rv_ssl_keystore     | keystore-filename | filename of Keystore that is present in the certs folder.|
    | rv_ssl_keystore-password| keystore-password | password of the keystore. |

    **NOTE:** Appropriate security measures with respect to key-store management should be considered while performing production deployment of RV.
    Avoid using the default keystore available for production deployment.
