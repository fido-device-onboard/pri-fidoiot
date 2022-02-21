# About

The FDO Rendezvous Service is designed to acts as a rendezvous point between a newly powered on Device and the Owner Onboarding Service.

***NOTE***: Appropriate security measures with respect to key-store management and credential management should be considered while performing production deployment of any FDO component.

# Getting Started with the FDO Rendezvous Service

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

Use the following commands to build FIDO Device Onboard (FDO) Rendezvous Service source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/
$ mvn clean install
```

This will copy the required executables and libraries into \<fdo-pri-src\>/component-samples/demo/rv/.

# Configuring the FDO Rendezvous Service

All the runtime configurations for the services are specified in four files: `service.env`, `hibernate.cfg.xml`, `service.yml` and `WEB-INF/web.xml`.

`service.env`: consists of all the credentials used by the FDO Rendezvous service. These credential configurations are to be generated freshly for each deployment.

`hibernate.cfg.xml`: consists of all the database configurations used by the FDO Rendezvous service. This file can be configured to pick various database tables and properties.

`service.yml` file is structured into multiple sections:

- `hibernate-properties:` - This section contains *Hibernate related runtime properties including the DB URL, dialect and others.


- `system-properties:` - This section contains the runtime environment variables.


- `http-server:` - This section contains the *Tomcat server related properties including ports, schemes, keystore information and api authentication setup.


- `manufacturer:` - This section contains the configuration related to manufacturer keystore path, type and credentials.


- `cwt:` - This section contains the configuration related to CBOR web token (cwt) keystore path, type and credentials.


- `workers:` The section contains the configuration to select desired functionality for the services. The deployer can pick and choose the functionality during runtime.


`WEB-INF/web.xml`: consist of the all configurations related to REST endpoints served. The deployer can pick and choose the served endpoints during runtime.

# Running FDO Rendezvous service

FDO Rendezvous service demo can be executed as a standalone service as well as a docker service. At the
end of initialization of all services, you will see following statement on the console.

`[INFO] Started Rendezvous Service.`

Follow the below steps to start All-In-One demo.

##  Run as Standalone service.
Open a terminal, change directory to `<fdo-pri-src>/component-samples/rv/` and execute following command.

```shell
java -jar rv.jar
```

Make sure to export the credential environment variables set in `service.env` file.

##  Run as Docker Service

Open a terminal, change directory to `<fdo-pri-src>/component-samples/rv/` and execute following command.

```
docker-compose up --build
```

In case you need super user access, prefix 'sudo -E' to above command.

***NOTE :*** To support OnDie ECDSA Device attestation, copy the required certificates and crls to `<fdo-pri-src>/component-samples/rv/ondiecache` folder.

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/rv/app-data/emdb.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.


# FDO PRI Rendezvous REST APIs

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|
| GET /api/v1/certificate?filename=fileName | Returns the certificate file based on filename | Path - filename | | | Certificate file in PKCS12 format |
| POST /api/v1/certificate?filename=fileName | Adds the certificate file to DB based on filename | Path - filename | text/plain| PKCS12 Certificate file in Binary format |  |
| DELETE /api/v1/certificate?filename=fileName | Delete the certificate file to DB based on filename | Path - filename | | |  |
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

# Configuring FDO Rendezvous service for HTTPS/TLS Communication

By default, the Rendezvous service uses HTTP for all communications on port 8040. In addition to that, the Rendezvous service can be configured to handle HTTPS requests from the Owner & device.

Rendezvous service can generate its own certificate and if you want to override the default certificate, follow these steps:

- Generate the Keystore/Certificate for the Rendezvous service. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

    * Ensure that the web certificate is issued to the resolvable domain of the Rendezvous service .


- Copy the generated Keystore/Certificate to `.app-data` folder and update credentials in `service.yml` file.


- Update the SSL keystore password & subject_names in `service.yml` file.
