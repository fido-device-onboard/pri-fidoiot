# About

The FDO manufacturer service is designed to generate new ownership vouchers by initializing devices with the FDO device initialization (DI) protocol and assigning those vouchers to another owner. 

***NOTE***:  Appropriate security measures with respect to key-store management and credential management should be considered while performing production deployment of any FDO component.

# Getting Started with the FDO Manufacturer

The following are the system requirements for the FDO Manufacturer.
- Host Operating System: Ubuntu (20.04, 22.04) / RHEL (8.4, 8.6) / Debian 11.4
- Java* Development Kit 17
- Apache Maven* 3.5.4 (Optional) software for building the demo from source
- Java IDE (Optional) for convenience in modifying the source code
- Docker 20.10.10+ / Podman engine  3.4.2+(For RHEL)
- Docker compose 1.29.2  / Podman-compose 1.0.3(For RHEL)
- Haveged

# Configuring JAVA Execution Environment

Appropriate proxy configuration should be updated in **`_JAVA_OPTIONS`** environment variable. (Mandatory, if you are working behind a proxy.)

Update the proxy information in _JAVA_OPTIONS as ```_JAVA_OPTIONS=-Dhttp.proxyHost=http_proxy_host -Dhttp.proxyPort=http_proxy_port -Dhttps.proxyHost=https_proxy_host -Dhttps.proxyPort=https_proxy_port```.

# Getting the Executable

Use the following commands to build FIDO Device Onboard (FDO) source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/manufacturer/.

# Configuring the FDO Manufacturer Service

All the runtime configurations for the services are specified in four files: `service.env`, `hibernate.cfg.xml`, `service.yml` & `WEB-INF/web.xml` and are present in `<fdo-pri-src>/component-samples/demo/manufacturer/`.

`service.env`: consists of all the credentials used by the Manufacturer service. These credential configurations are to be generated freshly for each deployment.

`hibernate.cfg.xml`: consists of all the database configurations used by the FDO Manufacturer service. This file can be configured to pick various database tables and properties.

`service.yml` file is structured into multiple sections:

- `hibernate-properties:` - This section contains *Hibernate related runtime properties including the DB URL, dialect and others.


- `system-properties:` - This section contains the runtime environment variables.


- `http-server:` - This section contains the *Tomcat server related properties including ports, schemes, keystore information and api authentication setup.

- `secrets:` - This section contains path to the service credentials.

- `manufacturer:` - This section contains the configuration related to manufacturer keystore path, type and credentials.


- `workers:` The section contains the configuration to select desired functionality for the services. The deployer can pick and choose the functionality during runtime.


`WEB-INF/web.xml`: consist of the all configurations related to REST endpoints served. The deployer can pick and choose the served endpoints during runtime.

# Running FDO Manufacturer

The FDO manufacturer can be executed as a standalone service as well as a docker service. At the
end of initialization of all services, you will see following statement on the console.

`[INFO] Started Manufacturer Service.`

Follow the below steps to start FDO Manufacturer demo.

***NOTE***: Generate random credentails before starting the service [Refer](https://github.com/fido-device-onboard/pri-fidoiot#generating-random-passwords-using-keys_gensh)

##  Run as Standalone service.
Open a terminal, change directory to `<fdo-pri-src>/component-samples/demo/manufacturer/` and execute following command.

```shell
java -jar aio.jar
```

Make sure to export the credential environment variables set in `service.env` file.

##  Run as Docker Service

Open a terminal, change directory to `<fdo-pri-src>/component-samples/demo/manufacturer/` and execute following command.

```
docker-compose up --build
```

In case you need super user access, prefix 'sudo -E' to above command.

***NOTE :*** To configure OnDie [REFER](https://github.com/fido-device-onboard/pri-fidoiot/tree/master/component-samples/demo#configuring-ondie-optional).

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/manufacturer/app-data/emdb.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

# FDO PRI Manufacturer REST APIs

***NOTE***: These REST APIs use Digest authentication. `api_user` and `api_password` properties specify the credentials to be used while making the REST calls. The value for `api_user` is present in `service.yml` file and value for `api_password` is present in `service.env` file.

***NOTE***: Follow the steps to port DIGEST auth calls with mTLS enabled cURL requests. [READ MORE](../README.MD#executing-curl-request-with-mtls)

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body | Sample cURL call |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|-----------------:|
| POST /api/v1/mfg/vouchers/<serial_no> | Gets extended Ownership Voucher with the serial number. | Path - Device Serial Number | | Owner Certificate | Extended Voucher |   curl -D - --digest -u ${api_user}: --location --request POST "http://host.docker.internal:8039/api/v1/mfg/vouchers/${serial_no}" --header 'Content-Type: text/plain' --data-raw  "$owner_certificate" -o ${serial_no}_voucher.txt |
| GET /api/v1/certificate?filename=fileName | Returns the certificate file based on filename | Query - filename | |  | Keystore file in binary format | curl  -D - --digest -u ${api_user}: --location --request GET 'http://host.docker.internal:8039/api/v1/certificate?filename=ssl.p12' |
| POST /api/v1/certificate?filename=fileName | Adds the certificate file to DB based on filename | Query - filename | text/plain| PKCS12 Certificate file in Binary format |  | curl -D - --digest -u ${api_user}: --location --request POST 'http://host.docker.internal:8039/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' --data-binary '@< path to ssl.p12 >' |
| DELETE /api/v1/certificate?filename=fileName | Delete the certificate file to DB based on filename | Query - filename | | |  | curl  -D - --digest -u ${api_user}: --location --request DELETE 'http://host.docker.internal:8039/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' | 
| POST /api/v1/rvinfo/ | Updates RV Info in `RV_DATA` table | | text/plain | RV Info |   |  curl  -D - --digest -u ${api_user}: --location --request POST 'http://host.docker.internal:8039/api/v1/rvinfo' --header 'Content-Type: text/plain' --data-raw '[[[5,"host.docker.internal"],[3,8040],[12,1],[2,"127.0.0.1"],[4,8041]]]' |
| GET /api/v1/deviceinfo/{seconds} | Serves the serial no. and GUID of the devices that completed DI in the last `n` seconds | Path - `n` seconds |  |  | JSON array of Serial No, GUID ,DI Timestamp and Attestion type. | curl -D - --digest -u apiUser:  --location --request GET 'http://host.docker.internal:8080/api/v1/deviceinfo/30' --header 'Content-Type: text/plain' | 
| GET /api/v1/logs | Serves the log from the manufacturer service | | | | Manufacturer logs| curl  -D - --digest -u ${api_user}:  --location --request GET 'http://host.docker.internal:8039/api/v1/logs' --header 'Content-Type: text/plain'| 
| DELETE /api/v1/logs | Deletes the log from the manufacturer service | | |  | | curl  -D - --digest -u ${api_user}:  --location --request DELETE 'http://host.docker.internal:8039/api/v1/logs' --header 'Content-Type: text/plain'|
| POST /api/v1/certificate/validity?days=no_of_days | Updates certificate validity in `CERTIFICATE_VALIDITY` table | Query - days | text/plain |  | | curl  -D - --digest -u ${api_user}: --location --request POST 'http://host.docker.internal:8039/api/v1/certificate/validity?days=10' --header 'Content-Type: text/plain' |
| GET /api/v1/certificate/validity | Collects certificate validity days from  `CERTIFICATE_VALIDITY` table | |  | | Number of Days| curl  -D - --digest -u ${api_user}: --location --request GET 'http://host.docker.internal:8039/api/v1/certificate/validity' |
| GET /health | Returns the health status |  |  | | Current version |  curl  -D - --digest -u ${api_user}:  --location --request GET 'http://host.docker.internal:8039/health' |

Following is the list of REST response error codes and it's possible causes :

|     Error Code     |             Possible Causes               |
| -------------------:|:----------------------------------------:|
| `401 Unauthorized`  | When an invalid Authentication header is present with the REST Request. Make sure to use the correct REST credentials. |
| `404 Not Found`     | When an invalid REST request is sent to MFG. Make sure to use the correct REST API endpoint. |
| `405 Method Not Allowed` | When an unsupported REST method is requested. Currently, MFG supports GET, POST and DELETE only. |
| `406 Not Acceptable` | When an invalid filename is passed through the REST endpoints. |
| `500 Internal Server Error` | Due to internal error, MFG unable to fetch/copy/delete the requested file. |

# Troubleshooting

Increase the heap size appropriately in case you encounter heap size issues.

# Configuring FDO Manufacturer for HTTPS/TLS Communication

By default, the FDO Manufacturer uses HTTP for all communications on port 8039. In addition to that, the manufacturer can be configured to handle HTTPS requests from the device.

Manufacturer can generate its own certificate and if you want to override the default certificate, follow these steps:

- Generate the Keystore/Certificate for the Manufacturer. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

    * Ensure that the web certificate is issued to the resolvable domain of the Manufacturer server.


- Copy the generated Keystore/Certificate to `.app-data` folder and update credentials in `service.yml` file.


- Update the SSL keystore password & subject_names in `service.yml` file.
