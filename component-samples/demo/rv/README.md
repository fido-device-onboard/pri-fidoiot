# About

The FDO Rendezvous Service is designed to acts as a rendezvous point between a newly powered on Device and the Owner Onboarding Service.

***NOTE***: Appropriate security measures with respect to key-store management and credential management should be considered while performing production deployment of any FDO component.

# Getting Started with the FDO Rendezvous Service

The following are the system requirements for the All-in-One demo.
- Host Operating System: Ubuntu (20.04, 22.04) / RHEL (8.4, 8.6) / Debian 11.4
- Java* Development Kit 17
- Apache Maven* 3.5.4 (Optional) software for building the demo from source
- Java IDE (Optional) for convenience in modifying the source code
- Docker 20.10.10+ / Podman engine 3.4.2+(For RHEL)
- Docker compose 1.29.2 / Podman-compose 1.0.3(For RHEL)
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

All the runtime configurations for the services are specified in four files: `service.env`, `hibernate.cfg.xml`, `service.yml` & `WEB-INF/web.xml` and are present in `<fdo-pri-src>/component-samples/demo/rv/`.

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

Follow the below steps to start FDO Rendezvous demo.

***NOTE***: Generate random credentails before starting the service [Refer](https://github.com/fido-device-onboard/pri-fidoiot#generating-random-passwords-using-keys_gensh)

##  Run as Standalone service.
Open a terminal, change directory to `<fdo-pri-src>/component-samples/demo/rv/` and execute following command.

```shell
java -jar aio.jar
```

Make sure to export the credential environment variables set in `service.env` file.

##  Run as Docker Service

Open a terminal, change directory to `<fdo-pri-src>/component-samples/demo/rv/` and execute following command.

```
docker-compose up --build
```

In case you need super user access, prefix 'sudo -E' to above command.

***NOTE :*** To support OnDie ECDSA Device attestation, copy the required certificates and crls to `<fdo-pri-src>/component-samples/rv/ondiecache` folder.

***NOTE***: The database file located at \<fdo-pri-src\>/component-samples/demo/rv/app-data/emdb.mv.db is not deleted during 'mvn clean'. As a result, the database schema and tables are persisted across docker invocations. Please delete the file manually, if you encounter any error due to persisted stale data.

***NOTE***: By default, `TrustedRendezvousAcceptFunction` worker is enabled. So we need to add the Owner's certificate to RV via `api/v1/rv/allow` endpoint to accept TO0 requests from Owner.

# FDO PRI Rendezvous REST APIs

***NOTE***: These REST APIs use Digest authentication. `api_user` and `api_password` properties specify the credentials to be used while making the REST calls.

***NOTE***: Follow the steps to port DIGEST auth calls with mTLS enabled cURL requests. [READ MORE](../README.MD#executing-curl-request-with-mtls)

| Operation                      | Description                        | Path/Query Parameters    | Content Type   |Request Body  | Response Body | Sample cURL call |
| ------------------------------:|:----------------------------------:|:------------------------:|:--------------:|-------------:|--------------:|-----------------:|
| GET /api/v1/certificate?filename=fileName | Returns the certificate file based on filename | Query - filename | | | Certificate file in PKCS12 format | curl  -D - --digest -u ${api_user}: --location --request GET 'http://host.docker.internal:8040/api/v1/certificate?filename=ssl.p12' |
| POST /api/v1/certificate?filename=fileName | Adds the certificate file to DB based on filename | Query - filename | text/plain| PKCS12 Certificate file in Binary format |  | curl -D - --digest -u ${api_user}: --location --request POST 'http://host.docker.internal:8040/api/v1/certificate?filename=ssl.p12' --data-binary '@< path to ssl.p12 >' |
| DELETE /api/v1/certificate?filename=fileName | Delete the certificate file to DB based on filename | Query - filename | | |  | curl  -D - --digest -u ${api_user}: --location --request DELETE 'http://host.docker.internal:8040/api/v1/certificate?filename=ssl.p12' --header 'Content-Type: text/plain' | 
| GET /api/v1/logs | Serves the log from the RV service | | | | RV logs| curl  -D - --digest -u ${api_user}:  --location --request GET 'http://host.docker.internal:8040/api/v1/logs' | 
| DELETE /api/v1/logs | Deletes the log from the RV service | | |  | | curl  -D - --digest -u ${api_user}:  --location --request DELETE 'http://host.docker.internal:8040/api/v1/logs' |
| POST /api/v1/certificate/validity?days=no_of_days | Updates certificate validity in `CERTIFICATE_VALIDITY` table | | text/plain|  | | curl  -D - --digest -u ${api_user}: --location --request POST 'http://host.docker.internal:8040/api/v1/certificate/validity?days=10' |
| GET /api/v1/certificate/validity | Collects certificate validity days from  `CERTIFICATE_VALIDITY` table | |  | | Number of Days| curl  -D - --digest -u ${api_user}: --location --request GET 'http://host.docker.internal:8040/api/v1/certificate/validity' |
| GET /health | Returns the health status |  |  | | Current version |  curl  -D - --digest -u ${api_user}:  --location --request GET 'http://host.docker.internal:8040/health'|
| POST /api/v1/rv/allow | Adds public key to allowed list of Owners in RV |  |  text/plain |  certificate in pem format | |   curl  -D - --digest -u ${api_user}:  --location --request POST 'http://host.docker.internal:8040/api/v1/rv/allow` --data-raw  "$owner_certificate" |
| DELETE /api/v1/rv/allow | delete public key to allowed list of Owners in RV |  |  text/plain |  certificate in pem format | |   curl  -D - --digest -u ${api_user}:  --location --request POST 'http://host.docker.internal:8040/api/v1/rv/allow` --data-raw  "$owner_certificate" |
| POST /api/v1/rv/deny | Adds public key to denied list of Owners in RV |  |  text/plain |  certificate in pem format | |   curl  -D - --digest -u ${api_user}:  --location --request POST 'http://host.docker.internal:8040/api/v1/rv/deny` --data-raw  "$owner_certificate" |


Following is the list of REST response error codes and it's description :

|     Error Code     |             Possible Causes               |
| -------------------:|:----------------------------------------:|
| `401 Unauthorized`  | When an invalid Authentication header is present with the REST Request. Make sure to use the correct REST credentials. |
| `404 Not Found`     | When an invalid REST request is sent to RV. Make sure to use the correct REST API endpoint. |
| `405 Method Not Allowed` | When an unsupported REST method is requested. Currently, RV supports GET, PUT and DELETE only. |
| `406 Not Acceptable` | When an invalid filename is passed through the REST endpoints. |
| `500 Internal Server Error` | Due to internal error, RV unable to fetch/copy/delete the requested file. |


# Troubleshooting

Increase the heap size appropriately in case you encounter heap size issues.

# Configuring FDO Rendezvous service for HTTPS/TLS Communication

By default, the Rendezvous service uses HTTP for all communications on port 8040. In addition to that, the Rendezvous service can be configured to handle HTTPS requests from the Owner & device.

Rendezvous service can generate its own certificate and if you want to override the default certificate, follow these steps:

- Generate the Keystore/Certificate for the Rendezvous service. [REFER](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html)

    * Ensure that the web certificate is issued to the resolvable domain of the Rendezvous service .


- Copy the generated Keystore/Certificate to `.app-data` folder and update credentials in `service.yml` file.


- Update the SSL keystore password & subject_names in `service.yml` file.
