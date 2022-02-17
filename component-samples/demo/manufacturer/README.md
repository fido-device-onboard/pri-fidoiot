# About

The FDO manufacturer service is designed to generate new ownership vouchers by initializing devices with the FDO device initialization (DI) protocol and assigning those vouchers to another owner. 

***NOTE***:  Appropriate security measures with respect to key-store management and credential management should be considered while performing production deployment of any FDO component.

# Getting Started with the FDO Manufacturer

The following are the system constraints for the FDO Manufacturer.
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

Use the following commands to build FIDO Device Onboard (FDO) source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/manufacturer/.

# Configuring the FDO Manufacturer Service

All the runtime configurations for the services are specified in four files: `service.env`, `hibernate.cfg.xml`, `service.yml` and `WEB-INF/web.xml`.

`service.env`: consists of all the credentials used by the Manufacturer service. These credential configurations are to be generated freshly for each deployment.

`hibernate.cfg.xml`: consists of all the database configurations used by the FDO Manufacturer service. This file can be configured to pick various database tables and properties.

`service.yml` file is structured into multiple sections:

- `hibernate-properties:` - This section contains hibernate related runtime properties including the DB URL, dialect and others.


- `system-properties:` - This section contains the runtime environment variables.


- `http-server:` - This section contains the *Tomcat server related properties including ports, schemes, keystore information and api authentication setup.


- `manufacturer:` - This section contains the configuration related to manufacturer keystore path, type and credentials.


- `workers:` The section contains the configuration to select desired functionality for the services. The deployer can pick and choose the functionality during runtime.


`WEB-INF/web.xml`: consist of the all configurations related to REST endpoints served. The deployer can pick and choose the served endpoints during runtime.

# Running FDO Manufacturer

The FDO Manufacturer can be executed as a standalone service as well as a docker service. At the
end of initialization of all services, you will see following statement on the console.

`[INFO] Started Manufacturer Service.`

Follow the below steps to start All-In-One demo.

##  Run as Standalone service.
Open a terminal, change directory to `<fdo-pri-src>/component-samples/manufacturer/` and execute following command.

```shell
java -jar manufacturer.jar
```

Make sure to export the credential environment variables set in `service.env` file.

##  Run as Docker Service

Open a terminal, change directory to `<fdo-pri-src>/component-samples/manufacturer/` and execute following command.

```
docker-compose up --build
```

In case you need super user access, prefix 'sudo -E' to above command.

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/manufacturer/app-data/emdb.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.


# FDO PRI Manufacturer REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| GET /api/v1/mfg/vouchers/<serial_no> | Gets extended Ownership Voucher with the serial number. | Path - Device Serial Number || | Ownership Voucher |
| GET /api/v1/certificate?filename=fileName | Returns the certificate file based on filename | Path - filename | | | Certificate file in PKCS12 format |
| POST /api/v1/certificate?filename=fileName | Adds the certificate file to DB based on filename | Path - filename | text/plain| PKCS12 Certificate file in Binary format |  |
| DELETE /api/v1/certificate?filename=fileName | Delete the certificate file to DB based on filename | Path - filename | | |  |
| POST /api/v1/rvinfo/ | Updates RV Info in `RV_DATA` table | | text/plain; charset=us-ascii | RV Info |  | |
| GET /api/v1/deviceinfo/{seconds} | Serves the serial no. and GUID of the devices that completed DI in the last `n` seconds | | | | JSON array of Serial No, GUID and DI Timestamp. |
| GET /api/v1/logs | Serves the log from the manufacturer service | || Manufacturer logs|
| DELETE /api/v1/logs | Deletes the log from the manufacturer service | |||
| POST /api/v1/certificate/validity?days=no_of_days | Updates certificate validity in `CERTIFICATE_VALIDITY` table | | text/plain; charset=us-ascii |  | | |
| GET /api/v1/certificate/validity | Collects certificate validity days from  `CERTIFICATE_VALIDITY` table | |s |  | | Number of Days|
| GET /health | Returns the health status | || Current version |

***NOTE***: These REST APIs use Digest authentication. `api_user` and `api_password` properties specify the credentials to be used while making the REST calls.

Following is the list of REST response error codes and it's description :

|     Error Code     |             Description                  |
| -------------------:|:----------------------------------------:|
| `401 Unauthorized`  | When an invalid Authentication header is present with the REST Request. Make sure to use the correct REST credentials. |
| `404 Not Found`     | When an invalid REST request is sent to AIO. Make sure to use the correct REST API endpoint. |
| `405 Method Not Allowed` | When an unsupported REST method is requested. Currently, AIO supports GET, PUT and DELETE only. |
| `406 Not Acceptable` | When an invalid filename is passed through the REST endpoints. |
| `500 Internal Server Error` | Due to internal error, AIO unable to fetch/copy/delete the requested file. |


# Troubleshooting

As the H2 DB grows, larger heap space will be required by the application to run the service. The default configured heap size is `256 MB`. Increase the heap size appropriately in `demo/owner/owner-entrypoint.sh` to avoid heap size issue

# Configuring FDO Manufacturer for HTTPS/TLS Communication

By default, the FDO Manufacturer uses HTTP for all communications on port 8039. In addition to that, the manufacturer can be configured to handle HTTPS requests from the device.

Manufacturer can generate its own certificate and if you want to override the default certificate, follow these steps:

- Generate the Keystore/Certificate for the Manufacturer. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

    * Ensure that the web certificate is issued to the resolvable domain of the Manufacturer server.


- Copy the generated Keystore/Certificate to `.app-data` folder and update credentials in `service.yml` file.


- Update the SSL keystore password & subject_names in `service.yml` file.
