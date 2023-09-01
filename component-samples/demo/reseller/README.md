# About

The FDO Reseller Service is designed to act as a intermediary between an Owner and Manufacturer of the Device.

***NOTE***: The Reseller service is provided to demonstrate operation of FDO components.  Appropriate security measures with respect to key-store management and credential management should be considered while performing production deployment of any FDO component.

# Getting Started with the FDO Reseller

The following are the system requirements for the All-in-One demo.
- Host Operating System: Ubuntu (20.04, 22.04) / RHEL (8.4, 8.6) / Debian 11.4
- Java* Development Kit 17
- Apache Maven* 3.5.4 (Optional) software for building the demo from source
- Java IDE (Optional) for convenience in modifying the source code
- Docker 20.10.10+ / Podman engine 3.4.2+ (For RHEL)
- Docker compose 1.29.2 / Podman-compose 1.0.3(For RHEL)
- Haveged

# Configuring JAVA Execution Environment

Appropriate proxy configuration should be updated in **`_JAVA_OPTIONS`** environment variable. (Mandatory, if you are working behind a proxy.)

Update the proxy information in _JAVA_OPTIONS as ```_JAVA_OPTIONS=-Dhttp.proxyHost=http_proxy_host -Dhttp.proxyPort=http_proxy_port -Dhttps.proxyHost=https_proxy_host -Dhttps.proxyPort=https_proxy_port```.

# Getting the Executable

Use the following commands to build FIDO Device Onboard (FDO) Reseller sample source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/component-samples/reseller/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/reseller/.

# Configuring the FDO Reseller Service

All the runtime configurations for the services are specified in four files: `service.env`, `hibernate.cfg.xml`, `service.yml` & `WEB-INF/web.xml` and are present in `<fdo-pri-src>/component-samples/demo/reseller/`.

`service.env`: consists of all the credentials used by the All-in-one demo service. These credential configurations are to be generated freshly for each deployment.

`hibernate.cfg.xml`: consists of all the database configurations used by the All-in-one demo service. This file can be configured to pick various database tables and properties.

`service.yml` file is structured into multiple sections:

- `hibernate-properties:` - This section contains *Hibernate related runtime properties including the DB URL, dialect and others.


- `system-properties:` - This section contains the runtime environment variables.


- `http-server:` - This section contains the *Tomcat server related properties including ports, schemes, keystore information and api authentication setup.

- `secrets:` - This section contains path to the service credentials.

- `owner:` - This section contains the configuration related to Owner keystore path, type and credentials.


- `h2-database:` - This section contains the configuration related to database connection.


- `workers:` The section contains the configuration to select desired functionality for the services. The deployer can pick and choose the functionality during runtime.


`WEB-INF/web.xml`: consist of the all configurations related to REST endpoints served. The deployer can pick and choose the served endpoints during runtime.

# Running FDO Reseller Service

The FDO Reseller can be executed as a standalone service as well as a docker service. At the
end of initialization of all services, you will see following statement on the console.

`[INFO] Started Reseller Service.`

Follow the below steps to start FDO Reseller Service.

***NOTE***: Generate random credentails before starting the service [Refer](../../../README.md#generating-random-passwords-using-keysgensh)

##  Run as Standalone service.
Open a terminal, change directory to `<fdo-pri-src>/component-samples/demo/reseller/` and execute following command.

```shell
java -jar aio.jar
```

Make sure to export the credential environment variables set in `service.env` file.

##  Run as Docker Service

Open a terminal, change directory to `<fdo-pri-src>/component-samples/demo/reseller/` and execute following command.

```
docker-compose up --build
```

In case you need super user access, prefix 'sudo -E' to above command.

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/reseller/app-data/emdb.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FDO PRI Reseller REST APIs

***NOTE***: Follow the steps to port DIGEST auth calls with mTLS enabled cURL requests. [READ MORE](../README.MD#executing-curl-request-with-mtls)

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body | Sample cURL call |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|-----------------:|
| POST /api/v1/resell/{guid} | Gets extended resell Ownership Voucher with the guid. | Path - guid of the device to resell | | Owner Certificate | The Ownership voucher in PEM format |   curl -D - --digest -u ${api_user}: --location --request POST "http://host.docker.internal:8070/api/v1/resell/${guid}" --header 'Content-Type: text/plain' --data-raw  "$owner_certificate" -o ${serial_no}_voucher.txt |
| GET /api/v1/owner/vouchers | Returns a list of all Ownership Voucher GUIDs. | | | | line separated list of GUIDs | curl  -D - --digest -u ${api_user}: --location --request GET "http://host.docker.internal:8070/api/v1/owner/vouchers" --header 'Content-Type: text/plain' |
| GET /api/v1/owner/vouchers/<device_guid> | Returns the Ownership Voucher for the specified GUID. | Query - id: Device GUID | | | Ownership Voucher | curl  -D - --digest -u ${api_user}: --location --request GET "http://host.docker.internal:8070/api/v1/owner/vouchers/${device_guid}" --header 'Content-Type: text/plain' |
| POST /api/v1/owner/vouchers/ | Insert Ownership Voucher against the specified GUID in `ONBOARDING_VOUCHER` table. | | text/plain | Content of Ownership Voucher in PEM Format | |  curl  -D - --digest -u ${api_user}: --location --request POST "http://host.docker.internal:8070/api/v1/owner/vouchers" --header 'Content-Type: text/plain' --data-binary '${voucher}' |
| GET /api/v1/owner/vouchers/<serialNo> | Returns the Ownership Voucher for the specified serial number. | Path - id: Device SerialNo | | | Ownership Voucher | curl  -D - --digest -u ${api_user}: --location --request GET "http://host.docker.internal:8080/api/v1/owner/vouchers/${device_serialno}" --header 'Content-Type: text/plain' |
| POST /api/v1/owner/vouchers/<serialNo> | Insert Ownership Voucher against the specified serialnumber in `ONBOARDING_VOUCHER` table.  The serialno to guid mapping will be preserved in 'VOUCHER_ALIAS' table | | text/plain | Content of Ownership Voucher in PEM Format | Guid of the device |  curl  -D - --digest -u ${api_user}: --location --request POST "http://host.docker.internal:8080/api/v1/owner/vouchers/${device_serialno}" --header 'Content-Type: text/plain' --data-raw '${voucher}' |

 
Following is the list of REST response error codes and it's possible causes :

|     Error Code     |             Possible Causes               |
| -------------------:|:----------------------------------------:|
| `401 Unauthorized`  | When an invalid Authentication header is present with the REST Request. Make sure to use the correct REST credentials. |
| `404 Not Found`     | When an invalid REST request is sent to Reseller. Make sure to use the correct REST API endpoint. |
| `405 Method Not Allowed` | When an unsupported REST method is requested. Currently, Reseller supports GET, POST and DELETE only. |
| `406 Not Acceptable` | When an invalid filename is passed through the REST endpoints. |
| `500 Internal Server Error` | Due to internal error, Reseller unable to fetch/copy/delete the requested file. |

# List of key store alias values 

Alias values that can be used for GET /api/v1/certificate?alias={alias}

|    Alias   |    
| ----------:|
| SECP384R1 |
| SECP256R1  |
| RSAPKCS3072 |
| RSAPKCS2048 |
| RSA2048RESTR |


# Troubleshooting

Increase the heap size appropriately in case you encounter heap size issues.

# Configuring Reseller for HTTPS/TLS Communication

By default, the Reseller uses HTTP for all communications on port 8070. In addition to that, the Reseller can be configured to handle HTTPS requests from the device.

Reseller can generate its own certificate and if you want to override the default certificate, follow these steps:

- Generate the Keystore/Certificate for the Reseller. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

    * Ensure that the web certificate is issued to the resolvable domain of the Reseller server.

- Copy the generated Keystore/Certificate to `.app-data` folder and update credentials in `service.yml` file.

- Update the SSL keystore password & subject_names in `service.yml` file.

# Configuring Reseller workers

Workers are java classes the implement various behavioral aspects of an FDO service.  Workers are defined in the workers section of the service.yml.


|     worker                        |             Description         |
| ---------------------------------:|:-------------------------------------:|
| org.fidoalliance.fdo.protocol.StandardCryptoService | Provides cryptographic services  (e.g. sign, verify, random number generation etc) |
| org.fidoalliance.fdo.protocol.StandardDatabaseServer | Provides an embedded H2 Database server |
| org.fidoalliance.fdo.protocol.StandardHttpServer | Provides an embedded Tomcat HTTP server for handling messages |
| org.fidoalliance.fdo.protocol.StandardLogProvider | Providers logging services for other workers (must be first defined worker)
| org.fidoalliance.fdo.protocol.StandardMessageDispatcher | Provides message processing for all FDO protocols          | 
| org.fidoalliance.fdo.protocol.StandardOwnerKeySupplier | Provides owner signing keys  |
| org.fidoalliance.fdo.protocol.db.StandardExtraInfoSupplier | Provides null as owner extra data in ownership voucher entries |
| org.fidoalliance.fdo.protocol.db.StandardKeyStoreInputStream | Provides Keystore loading from a database table |
| org.fidoalliance.fdo.protocol.db.StandardKeyStoreOutputStream | Provides Keystore storage to a database table |
| org.fidoalliance.fdo.FileKeyStoreInputStream | Loads PRI Service keys from disk |
| org.fidoalliance.fdo.FileKeyStoreOutputStream | Save PRI Service keys to disk |
| org.fidoalliance.fdo.protocol.db.StandardValidityDaysSupplier | Provides the validity days for certificate generation |
