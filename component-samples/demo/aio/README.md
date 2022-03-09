# About

The FDO All-In-One demo is designed to run multiple FDO services like Manufacturer, Rendezvous and Owner Service in a single
**docker** instance. The purpose of this demo is to fast-track demonstration of FDO protocol using different client implementations.

***NOTE***: The All-In-One demo is provided to demonstrate out-of-box operation of FDO components.  Appropriate security measures with respect to key-store management and credential management should be considered while performing production deployment of any FDO component.

# Getting Started with the FDO All-In-One Demo

The following are the system requirements for the All-in-One demo.
- Operating System: Ubuntu* 20.04
- Java* Development Kit 11
- Apache Maven* 3.5.4 (Optional) software for building the demo from source
- Java IDE (Optional) for convenience in modifying the source code
- Docker 18.09
- Docker compose 1.21.2
- Haveged

# Configuring JAVA Execution Environment

Appropriate proxy configuration should be updated in **`_JAVA_OPTIONS`** environment variable. (Mandatory, if you are working behind a proxy.)

Update the proxy information in _JAVA_OPTIONS as ```_JAVA_OPTIONS=-Dhttp.proxyHost=http_proxy_host -Dhttp.proxyPort=http_proxy_port -Dhttps.proxyHost=https_proxy_host -Dhttps.proxyPort=https_proxy_port```.

# Getting the Executable

Use the following commands to build FIDO Device Onboard (FDO) All-In-One Component sample source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/component-samples/aio/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/aio/.

# Configuring the FDO All-In-One Demo Service

All the runtime configurations for the services are specified in four files: `service.env`, `hibernate.cfg.xml`, `service.yml` and `WEB-INF/web.xml`.

`service.env`: consists of all the credentials used by the All-in-one demo service. These credential configurations are to be generated freshly for each deployment.

`hibernate.cfg.xml`: consists of all the database configurations used by the All-in-one demo service. This file can be configured to pick various database tables and properties.

`service.yml` file is structured into multiple sections:

- `hibernate-properties:` - This section contains *Hibernate related runtime properties including the DB URL, dialect and others.


- `system-properties:` - This section contains the runtime environment variables.


- `http-server:` - This section contains the *Tomcat server related properties including ports, schemes, keystore information and api authentication setup.


- `manufacturer:` - This section contains the configuration related to manufacturer keystore path, type and credentials.


- `owner:` - This section contains the configuration related to Owner keystore path, type and credentials.


- `cwt:` - This section contains the configuration related to CBOR web token (cwt) keystore path, type and credentials.


- `workers:` The section contains the configuration to select desired functionality for the services. The deployer can pick and choose the functionality during runtime.


`WEB-INF/web.xml`: consist of the all configurations related to REST endpoints served. The deployer can pick and choose the served endpoints during runtime.

# Running All-In-One Demo

The All-In-One demo can be executed as a standalone service as well as a docker service. At the
end of initialization of all services, you will see following statement on the console.

`[INFO] Started All-in-one demo Service.`

Follow the below steps to start All-In-One demo.

##  Run as Standalone service.
Open a terminal, change directory to `<fdo-pri-src>/component-samples/aio/` and execute following command.

```shell
java -jar aio.jar
```

Make sure to export the credential environment variables set in `service.env` file.

##  Run as Docker Service

Open a terminal, change directory to `<fdo-pri-src>/component-samples/aio/` and execute following command.

```
docker-compose up --build
```

In case you need super user access, prefix 'sudo -E' to above command.

***NOTE :*** To support OnDie ECDSA Device attestation, copy the required certificates and crls to `<fdo-pri-src>/component-samples/aio/ondiecache` folder.

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/aio/app-data/emdb.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FDO PRI AIO REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body | Sample cURL call |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|-----------------:|
| GET /api/v1/deviceinfo/{seconds} | Serves the serial no. and GUID of the devices that completed DI in the last `n` seconds | | | | Serial No, GUID and Timestamp of device that completed DI | curl -D - --digest -u ${api_user}:  --location --request GET 'http://localhost:8080/api/v1/deviceinfo/30' --header 'Content-Type: text/plain' | 
| POST /api/v1/to0/{guid} | initiate TO0 from Owner | GUID of the device to initiate TO0 | text/plain |  |  | curl  -D - --digest -u ${api_user}: --location --request GET "http://localhost:8080/api/v1/to0/${device_guid}" --header 'Content-Type: text/plain' |
| POST /api/v1/aio/rvinfo?ip=ip-address&rvprot=(http or https) | allows to update rvInfo with ipaddress and rv protocol | ip => ip-address of machine running aio & rvprot => supported rvProtocol | text/plain |  |  |  curl  -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8080/api/v1/aio/rvinfo?ip=127.0.0.1&rvprot=http' --header 'Content-Type: text/plain' |
| GET /api/v1/logs | Serves the log from the AIO service | | | | AIO logs | curl  -D - --digest -u ${api_user}:  --location --request GET 'http://localhost:8080/api/v1/logs' --header 'Content-Type: text/plain'| 
| DELETE /api/v1/logs | Deletes the log from the AIO service | |  |  |  | curl  -D - --digest -u ${api_user}:  --location --request DELETE 'http://localhost:8080/api/v1/logs' --header 'Content-Type: text/plain'|
| GET /health | Returns the health status | | | | Current version | curl  -D - --digest -u ${api_user}:  --location --request GET 'http://localhost:8080/health' --header 'Content-Type: text/plain' |


# FDO PRI Manufacturer REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body | Sample cURL call |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|-----------------:|
| GET /api/v1/mfg/vouchers/<serial_no> | Gets extended Ownership Voucher with the serial number. | Path - Device Serial Number | | Owner Certificate | |   curl -D - --digest -u ${api_user}: --location --request POST "http://localhost:8039/api/v1/mfg/vouchers/${serial_no}" --header 'Content-Type: text/plain' --data-raw  "$owner_certificate" -o ${serial_no}_voucher.txt |
| GET /api/v1/certificate?filename=fileName | Returns the certificate file based on filename | Path - filename | | | Certificate file in PKCS12 format | curl  -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8039/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' |
| POST /api/v1/certificate?filename=fileName | Adds the certificate file to DB based on filename | Path - filename | text/plain| PKCS12 Certificate file in Binary format |  | curl -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8039/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' --data-binary '@< path to ssl.p12 >' |
| DELETE /api/v1/certificate?filename=fileName | Delete the certificate file to DB based on filename | Path - filename | | |  | curl  -D - --digest -u ${api_user}: --location --request DELETE 'http://localhost:8039/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' | 
| POST /api/v1/rvinfo/ | Updates RV Info in `RV_DATA` table | | text/plain; charset=us-ascii | RV Info |   |  curl  -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8039/api/v1/rvinfo' --header 'Content-Type: text/plain' --data-raw '[[[5,"localhost"],[3,8040],[12,1],[2,"127.0.0.1"],[4,8040]]]' |
| GET /api/v1/deviceinfo/{seconds} | Serves the serial no. and GUID of the devices that completed DI in the last `n` seconds |  |  |  | JSON array of Serial No, GUID and DI Timestamp. | curl -D - --digest -u apiUser:  --location --request GET 'http://localhost:8080/api/v1/deviceinfo/30' --header 'Content-Type: text/plain' | 
| GET /api/v1/logs | Serves the log from the manufacturer service | | | | Manufacturer logs| curl  -D - --digest -u ${api_user}:  --location --request GET 'http://localhost:8039/api/v1/logs' --header 'Content-Type: text/plain'| 
| DELETE /api/v1/logs | Deletes the log from the manufacturer service | | |  | | curl  -D - --digest -u ${api_user}:  --location --request DELETE 'http://localhost:8039/api/v1/logs' --header 'Content-Type: text/plain'|
| POST /api/v1/certificate/validity?days=no_of_days | Updates certificate validity in `CERTIFICATE_VALIDITY` table | | text/plain; charset=us-ascii |  | | curl  -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8039/api/v1/certificate/validity?days=10' --header 'Content-Type: text/plain' |
| GET /api/v1/certificate/validity | Collects certificate validity days from  `CERTIFICATE_VALIDITY` table | |  | | Number of Days| curl  -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8039/api/v1/certificate/validity' --header 'Content-Type: text/plain' |
| GET /health | Returns the health status |  |  | | Current version |  curl  -D - --digest -u ${api_user}:  --location --request GET 'http://localhost:8039/health' --header 'Content-Type: text/plain' |

# FDO PRI Owner REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body | Sample cURL call |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|-----------------:|
| POST /api/v1/owner/redirect    | Updates TO2 RVBlob in `ONBOARDING_CONFIG` table. | | text/plain | RVTO2Addr in diagnostic form | | curl -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8042/api/v1/owner/redirect' --header 'Content-Type: text/plain'  --data-raw '[["localhost","127.0.0.1",8042,3]]' |
| POST /api/v1/to0/{guid} | initiate TO0 from Owner | GUID of the device to initiate TO0 | text/plain |  |  | curl  -D - --digest -u ${api_user}: --location --request GET "http://localhost:8042/api/v1/to0/${device_guid}" --header 'Content-Type: text/plain' |
| POST /api/v1/owner/svi | Uploads SVI instructions to `SYSTEM_PACKAGE` table. |  | text/plain | SVI Instruction |   | curl -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8042/api/v1/owner/svi' --header 'Content-Type: text/plain' --data-raw '[{"filedesc" : "setup.sh","resource" : "URL"}, {"exec" : ["bash","setup.sh"] }]' |
| GET /api/v1/owner/vouchers/<device_guid> | Returns the Ownership Voucher for the specified GUID. | Query - id: Device GUID | | | Ownership Voucher | curl  -D - --digest -u ${api_user}: --location --request GET "http://localhost:8042/api/v1/owner/vouchers/${device_guid}" --header 'Content-Type: text/plain' |
| POST /api/v1/owner/vouchers/ | Insert Ownership Voucher against the specified GUID in `ONBOARDING_VOUCHER` table. | | text/plain | Content of Ownership Voucher in PEM Format | |  curl  -D - --digest -u ${api_user}: --location --request GET "http://localhost:8042/api/v1/owner/vouchers" --header 'Content-Type: text/plain' --data-binary '${voucher}' |
| GET /api/v1/logs | Serves the log from the manufacturer service | | | | Manufacturer logs| curl  -D - --digest -u ${api_user}:  --location --request GET 'http://localhost:8042/api/v1/logs' --header 'Content-Type: text/plain'| 
| DELETE /api/v1/logs | Deletes the log from the manufacturer service | | |  | | curl  -D - --digest -u ${api_user}:  --location --request DELETE 'http://localhost:8042/api/v1/logs' --header 'Content-Type: text/plain'|
| GET /health | Returns the health status |  |  | | Current version |  curl  -D - --digest -u ${api_user}:  --location --request GET 'http://localhost:8042/health' --header 'Content-Type: text/plain' |
| GET /api/v1/ondie | Serves the stored certs & crls files | || Ondie certs & crl files |
| POST /api/v1/ondie | To download onDie certs and crls zip file url. | | text/plain | Ondie certs/crls URL |
| GET /api/v1/certificate?filename=fileName | Returns the certificate file based on filename | Path - filename | | | Certificate file in PKCS12 format | curl  -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8042/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' |
| GET /api/v1/certificate?alias=SECP256R1 | Returns the owner certificate of the given alias type | Path - alias | | | Certificate PEM format | curl  -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8042/api/v1/certificate?alias=SECP256R1' --header 'Content-Type: text/plain' |
| GET /api/v1/certificate?uuid=uuid | Returns the owner alias type for the given voucher| Path - uuid | | | Certificate PEM format | curl  -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8042/api/v1/certificate?uuid=cc60f0aa-56d0-492e-8c8d-9a1fe55cb60 --header 'Content-Type: text/plain' |
| POST /api/v1/certificate?filename=fileName | Adds the certificate file to DB based on filename | Path - filename | text/plain| PKCS12 Certificate file in Binary format |  | curl -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8042/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' --data-binary '@< path to ssl.p12 >' |
| DELETE /api/v1/certificate?filename=fileName | Delete the certificate file to DB based on filename | Path - filename | | |  | curl  -D - --digest -u ${api_user}: --location --request DELETE 'http://localhost:8042/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' | | POST /api/v1/certificate/validity?days=no_of_days | Updates certificate validity in `CERTIFICATE_VALIDITY` table | | text/plain; charset=us-ascii |  | | |
| POST /api/v1/certificate/validity?days=no_of_days | Updates certificate validity in `CERTIFICATE_VALIDITY` table | | text/plain; charset=us-ascii |  | | curl  -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8039/api/v1/certificate/validity?days=10' --header 'Content-Type: text/plain' |
| GET /api/v1/certificate/validity | Collects certificate validity days from  `CERTIFICATE_VALIDITY` table | |  | | Number of Days| curl  -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8039/api/v1/certificate/validity' --header 'Content-Type: text/plain' |
| GET /api/v1/owner/messagesize | Collects the max message size from `ONBOARDING_CONFIG` table | | |  | MAX_MESSAGE_SIZE | curl -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8042/api/v1/owner/messagesize' --header 'Content-Type: text/plain'|
| POST /api/v1/owner/messagesize | Updates the max message size in `ONBOARDING_CONFIG` table | | | MAX_MESSAGE_SIZE | | curl -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8042/api/v1/owner/messagesize?size=1400' --header 'Content-Type: text/plain'|
| GET /api/v1/owner/svisize | Collects the owner svi size from `ONBOARDING_CONFIG` table | | text/plain |  | MAX_MESSAGE_SIZE | curl -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8042/api/v1/owner/svisize' --header 'Content-Type: text/plain' |
| POST /api/v1/owner/svisize | Updates the owner svi size in `ONBOARDING_CONFIG` table | | text/plain | MAX_MESSAGE_SIZE | | curl -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8042/api/v1/owner/svisize?size=1400' --header 'Content-Type: text/plain' |
| GET /api/v1/owner/resource?filename=fileName | Returns the file based on filename from `SYSTEM_RESOURCE` table | Path - filename | | | file |  curl -D - --digest -u ${api_user}: --location --request GET 'http://localhost:8042/api/v1/owner/resource?filename=fileName' --header 'Content-Type: text/plain'  |
| POST /api/v1/owner/resource?filename=fileName | Adds the file to DB based on filename  from `SYSTEM_RESOURCE` table  | Path - filename | text/plain| file in Binary format |  |  curl -D - --digest -u ${api_user}: --location --request POST 'http://localhost:8042/api/v1/owner/resource?filename=fileName' --header 'Content-Type: text/plain' --data-binary '@< path to file >' |
| DELETE /api/v1/owner/resource?filename=fileName | Delete the  file from DB based on filename from `SYSTEM_RESOURCE` table   | Path - filename | | |  |  curl -D - --digest -u ${api_user}: --location --request DELETE 'http://localhost:8042/api/v1/owner/resource?filename=fileName' --header 'Content-Type: text/plain'|


***NOTE***: These REST APIs use Digest authentication. `api_user` and `api_password` properties specify the credentials to be used while making the REST calls. The value for `api_user` is present in `service.yml` file and value for `api_password` is present in `service.env` file. 
 
Following is the list of REST response error codes and it's possible causes :

|     Error Code     |             Possible Causes               |
| -------------------:|:----------------------------------------:|
| `401 Unauthorized`  | When an invalid Authentication header is present with the REST Request. Make sure to use the correct REST credentials. |
| `404 Not Found`     | When an invalid REST request is sent to AIO. Make sure to use the correct REST API endpoint. |
| `405 Method Not Allowed` | When an unsupported REST method is requested. Currently, AIO supports GET, PUT and DELETE only. |
| `406 Not Acceptable` | When an invalid filename is passed through the REST endpoints. |
| `500 Internal Server Error` | Due to internal error, AIO unable to fetch/copy/delete the requested file. |


# Troubleshooting

As the H2 DB grows, larger heap space will be required by the application to run the service. The default configured heap size is `256 MB`. Increase the heap size appropriately in `demo/owner/owner-entrypoint.sh` to avoid heap size issue

# Configuring AIO for HTTPS/TLS Communication

By default, the AIO uses HTTP for all communications on port 8080. In addition to that, the AIO can be configured to handle HTTPS requests from the device.

AIO can generate its own certificate and if you want to override the default certificate, follow these steps:

- Generate the Keystore/Certificate for the AIO. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

    * Ensure that the web certificate is issued to the resolvable domain of the AIO server.

- Copy the generated Keystore/Certificate to `.app-data` folder and update credentials in `service.yml` file.

- Update the SSL keystore password & subject_names in `service.yml` file.

# Configuring AIO workers

|     worker                                              |             Description                                  |
| -------------------------------------------------------:|:--------------------------------------------------------:|
| org.fidoalliance.fdo.protocol.HttpOwnerSchemeSupplier   | Tells owner to use HTTP instead of HTTPS for TO0 protocol|
| org.fidoalliance.fdo.protocol.StandardOwnerSchemeSupplier | Tells owner to use HTTPS for TO0 protocol |
| org.fidoalliance.fdo.protocol.db.StandardVoucherStorageFunction | Stores voucher in the database without performing To0 protocol |
| org.fidoalliance.fdo.protocol.db.AutoInjectVoucherStorageFunction | Automatically extends the voucher to the owner and performs To0 protocol |
