
# FIDO Device Onboard (FDO) Protocol Reference Implementation (PRI) Quick Start

This is a reference implementation of the
[FDO v1.1 Proposed Standard](https://fidoalliance.org/specs/FDO/FIDO-Device-Onboard-PS-v1.1-20220419)
published by the FIDO Alliance. It provides production-ready implementation for the protocol defined
by the specification. It also provides example implementation for different components to
demonstrate end-to-end execution of the protocol. Appropriate security measures should be taken while
deploying the example implementation for these components.

## System Requirements:

* **Ubuntu (20.04, 22.04) / RHEL (8.4, 8.6) / Debian 11.4**. +
* **Maven 3.6.3**.
* **Java 17**.
* **Haveged**.
* **Docker engine (minimum 20.10.10, Supported till version 20.10.21) / Podman engine (For RHEL) 3.4.2+**
* **Docker-compose (minimum version 1.29.2) / Podman-compose 1.0.3(For RHEL)**

+Supported list of Host operating systems.

***NOTE***: FDO service require strong random number generation in order to perform the required cryptographic functions.  The FDO servers will hang on startup waiting for sufficient entropy unless the system continuously supplied random data.
- `sudo apt-get install -y haveged` will ensure the system fdo services are running on have sufficient entropy.



## Source Layout

For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.

FDO PRI source code is organized into the following sub-folders.

- `component-samples`: It contains all the normative and non-normative server and client implementation with all specifications listed in the base profile.

- `protocol`: It contains implementations related to protocol message processing.


## Building FDO PRI Source

FDO PRI source is written in [Java 17](https://openjdk.java.net/projects/jdk/17/) and uses the
[Apache Maven* software](http://maven.apache.org).

The list of ports that are used for unit tests and sample code:

| Port | Description    |
| ---- | -------------- |
| 8038 | manufacturer https port |
| 8039 | manufacturer http port |
| 8040 | rv http port |
| 8041 | rv https port |
| 8042 | owner http port |
| 8043 | owner https port |
| 3306 | PRI Service Database port |
| 8070 | reseller http port |
| 8072 | reseller https port |
| 8080 | aio http port |
| 8082 | aio H2 Console port |
| 8083 | manufacturer H2 Console port |
| 8084 | rv H2 Console port |
| 8085 | owner H2 Console port |
| 8073 | reseller H2 Console port |
| 8443 | aio https port |


Use the following commands to build FDO PRI source.
```
$ cd <fdo-pri-src>
$ mvn clean install
```

or FDO PRI source can be built using docker container. [REFER](./build/README.md)

The build creates artifacts which will be used in the rest of this guide.

The runnable artifacts can be found in `<fdo-pri-src>/component-samples/demo/`.

### Credential storage

Credentials are defined in the `<fdo-pri-src>/component-sample/demo/{component}/service.env` for each service and will be made available as environment variables to each docker/podman container.

aio/service.env
manufacturer/service.env
owner/service.env
reseller/service.env
rv/service.env

The following passwords are defined in each service.env:

| Environment Variable | Description                          |
| ---------------------| -------------------------------------|
| db_user              | database user account.               |
| db_password          | database password                    |
| api_password         | defines the DIGEST REST API password |
| encrypt_password     | keystore encryption password         |
| ssl_password         | Https/web server keystore password   |
| user-cert            | File containing Client keypair (mTLS)   |
| ssl-ca               | File containing the CA certificate of Server (mTLS) |
| api_user             | Field containing Client's certificate details (mTLS) |
| useSSL               | Boolean value specifying SSL connection with Database |
| requireSSL           | Boolean value specifying SSL connection with Hibernate ORM |


Keystores containing private keys can be stored in the database - `<fdo-pri-src>/component-sample/demo/{component}/app-data/emdb.mv.db`
as well as in the mounted file system. During runtime, the deployer can decide the mode of Keystore IO by activating the required worker class.

keys_gen.sh can be used to generate random passwords for each service.env.

***NOTE***: Changing the database password after the H2 database has been created requires the database file to be deleted and recreated.


### Generating random passwords using keys_gen.sh

1. Generating demo certificate authority KeyPair and certificate

    ```shell
      bash demo_ca.sh
    ```

    **NOTE**: Configure the properties of `demo-CA` by updating `root-ca.conf`.

2. Generating Server and Client Keypair and certificates.

    ```shell
    bash web_csr_req.sh
    bash user_csr_req.sh
    ```

    **NOTE**: Both Server and Client certificates are signed by the previously generated demo-CA. Moreover, we can configure the properties of Server and Client certificates by updating `web-server.conf` and `client.conf` respectively. [Learn how to configure Server and Client Certificates.](#specifying-subject-alternate-names-for-the-webhttps-self-signed-certificate)

3. Running keys_gen.sh will generate random passwords for the all http servers and creates `secrets` folder containing all the required `.pem` files of Client, CA and Server component.

    ```
    $ cd <fdo-pri-src>/component-samples/demo/scripts
    $ ./keys_gen.sh
    ```

    A message "Key generation completed." will be displayed on the console.

    Credentials will be stored in the `secrets` directory within `<fdo-pri-src>/component-sample/demo/scripts`.

4. Copy both `secrets/` and `service.env` file from  `<fdo-pri-src>/component-sample/demo/scripts`  folder to the individual components.

    **NOTE:** Don't replace `service.env` present in the database component with generated `service.env` in `scripts` folder.

    ```
    $ cd <fdo-pri-src>/component-samples/demo/scripts
    $ cp -r ./secrets/. ../<component>
    $ cp service.env ../<component>
    ```

    **NOTE**: Component refers to the individual FDO services like aio, manufacturer, rv , owner and reseller.

    **NOTE**: Docker secrets are only available to swarm services, not to standalone containers. To use this feature, consider adapting your container to run as a service. Stateful containers can typically run with a scale of 1 without changing the container code.

### Specifying Subject alternate names for the Web/HTTPS self-signed certificate.

When the http server starts for the first time it will generate a self-signed certificate for https protocol.

The   subject name of the self-signed certificate is defined in the  `web-server.conf` and `client.conf`.

Uncomment `subjectAltName` and allowed list of IP and DNS in `[alt_names]` section. Example:

```
#[ req_ext ]
  subjectAltName = @alt_names

[ alt_names ]
  #Replace or add new DNS and IP entries with the ones required by the HTTPs/Web Service.
  DNS.1 = www.example.com
  DNS.2 = test.example.com
  DNS.3 = mail.example.com
  DNS.4 = www.example.net
  IP.1 = 127.0.0.1
  IP.2 = 200.200.200.200
  IP.3 = 2001:DB8::1
```

**NOTE**: Self-signed certificates created using the script is not recommended for use in production environments.

### Starting Standalone Database for PRI servers

1. Copy generated `secrets/` folder to `<fdo-pri-src>/component-sample/demo/db` folder. [Generate secrets](https://github.com/fido-device-onboard/pri-fidoiot#generating-random-passwords-using-keys_gensh)

2. Start the Database service
```shell
cd <fdo-pri-src>/component-samples/demo/db
docker-compose up --build -d/ podman-compose up --build -d
```

**NOTE**: By default, Database uses mTLS connection for jdbc. MariaDB* is used as the default database. For non-mTLS jdbc connection, set `use_ssl` and `require_ssl` property to `false` in `service.yml` of individual services.

**NOTE**: Follow the steps to [Enable embedded H2 database](./component-samples/demo/README.MD#enable-embedded-h2-database-server)

### Starting FDO PRI HTTP Servers


#### Starting the FDO PRI All-In-One (AIO) HTTP Server

To start the server as a docker/podman container.

```
$ cd <fdo-pri-src>/component-samples/demo/aio
$ docker-compose up --build  -d / podman-compose up --build -d
```

To start the server as a standalone java application.

  ```
  $ cd <fdo-pri-src>/component-samples/demo/aio
  $ java -jar aio.jar
  ```

  The server will listen for FDO PRI http & https messages on ports 8080 and 8443 respectively.

  The all-in-one demo supports all FDO protocols in a single service by default.


#### Starting the FDO PRI Rendezvous (RV) HTTP Server

To start the server as a docker/podman container.

```
$ cd <fdo-pri-src>/component-samples/demo/rv
$ docker-compose up --build -d/ podman-compose up --build -d
```

To start the server as a standalone java application.

```
$ cd <fdo-pri-src>/component-samples/demo/rv
$ java -jar aio.jar
```

The server will listen for FDO PRI HTTP & HTTPS  messages on port 8040 and 8041 respectively.

#### Starting the FDO PRI Owner HTTP Server

```
$ cd <fdo-pri-src>/component-samples/demo/owner
$ docker-compose up --build -d/ podman-compose up --build -d
```

To start the server as a standalone java application.

```
$ cd <fdo-pri-src>/component-samples/demo/owner
$ java -jar aio.jar
```

The server will listen for FDO PRI HTTP & HTTPS messages on port 8042 and 8043 respectively.

#### Starting the FDO PRI Manufacturer Server

```
$ cd <fdo-pri-src>/component-samples/demo/manufacturer
$ docker-compose up --build -d/ podman-compose up --build -d
```

To start the server as a standalone java application.

```
$ cd <fdo-pri-src>/component-samples/demo/manufacturer
$ java -jar aio.jar
```

The server will listen for FDO PRI HTTP & HTTPS  messages on port 8039 and 8038 respectively.

### Running the FDO PRI HTTP Device

#### Staring the FDO PRI HTTP Device

***NOTE***: By default the device is configured to run with the All-In-One (AIO) ports.  You must edit the service.yml in the demo device directory to run with the  manufacturer demo.

```shell
$ cd <fdo-pri-src>/component-samples/demo/device
$ docker-compose up --build -d
```

To start the Client as a standalone java application.

```
$ cd <fdo-pri-src>/component-samples/demo/device
$ java -jar device.jar
```
Running the device for the first time will result in device keys being generated and the device keys are stored in the `app-data` directory.
Once device keys are generated the device will run the DI protocol and store the DI credentials in a file called `credentials.bin`.

Running device for a second time will result in the device performing TO1/TO2 protocols.

Deleting the `credentials.bin` file will force the device to re-run DI protocol.


#### Configuring FDO PRI HTTP Device

`<fdo-pri-src>/component-samples/demo/device/service.yml` contains the configuration of the device.


#### Creating Ownership Vouchers using All-In-One (AIO) demo

Before running the device for the first time start the demo aio server.

Run the demo device

As auto injection of ownership voucher is enabled in AIO by default; the ownership voucher is extended and stored in `ONBOARDING_CONFIG` table and the device is ready for TO1/2.

#### Switching between mTLS and Digest Authentication for REST endpoints

1. Update `WEB-INF/web.xml` to support Digest authentication
    ```
    <security-constraint>
        <web-resource-collection>
            <web-resource-name>apis</web-resource-name>
            <url-pattern>/api/v1/*</url-pattern>
        </web-resource-collection>
        <auth-constraint>
            <role-name>api</role-name>
        </auth-constraint>
        <user-data-constraint>
          <transport-guarantee>NONE</transport-guarantee>
        </user-data-constraint>
      </security-constraint>

      <login-config>
          <auth-method>DIGEST</auth-method>
      </login-config>
    ```

2. Update `{server.api.user}` and `{server.api.password}` in `demo/<component>/tomcat-users.xml` file.

#### Creating Ownership Vouchers using Individual Component Demos

Before running the device for the first time start the demo manufacturer.

Use the following REST api to specify the rendezvous instructions for demo rv server.

POST https://host.docker.internal:8038/api/v1/rvinfo (or http://host.docker.internal:8039/api/v1/rvinfo)
The post body content-type header `text/plain`

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the manufacturer's service.env or can use CLIENT-CERT AUTH (mTLS).

POST content
```
[[[5,"host.docker.internal"],[3,8041],[12,2],[2,"127.0.0.1"],[4,8041]]]
```

Change the `di-url: http://host.docker.internal:8080` in the demo device service.yml to `di-url: http://host.docker.internal:8039`

After Running the device the successful output would be as follows:

```
$ cd <fdo-pri-src>/component-samples/demo/device
$ java -jar device.jar
...
13:50:21.846 [INFO ] Type 13 []
13:50:21.850 [INFO ] Starting Fdo Completed
```


Next get the owners public key by starting the demo owner service and use the following REST API.

GET https://host.docker.internal:8043/api/v1/certificate?alias=SECP256R1 (or http://host.docker.internal:8042/api/v1/certificate?alias=SECP256R1)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the owner's service.env or can use CLIENT-CERT AUTH (mTLS).

Response body will be the Owner's certificate in PEM format


```
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
```


For EC384 based vouchers use the following API:

GET https://host.docker.internal:8043/api/v1/certificate?alias=SECP384R1 (or http://host.docker.internal:8042/api/v1/certificate?alias=SECP384R1)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the owner's service.env or can use CLIENT-CERT AUTH (mTLS).

Result body will be the owners certificate in PEM format

[REFER](https://github.com/fido-device-onboard/pri-fidoiot/tree/master/component-samples/demo/aio#list-of-key-store-alias-values) for the other supported attestation type.

Next, collect the serial number of the last manufactured voucher

GET https://host.docker.internal:8038/api/v1/deviceinfo/{seconds} (or http://host.docker.internal:8039/api/v1/deviceinfo/100000)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the manufacturer's service.env or can use CLIENT-CERT AUTH (mTLS).

Result will contain the device info
```
[{"serial_no":"43FF320A","timestamp":"2022-02-18 21:50:21.838","uuid":"24275cd7-f9f5-4d34-a2a5-e233ac38db6c"}]
```

Post the PEM Certificate obtained form the owner to the manufacturer to get the ownership voucher transferred to the owner.
POST https://host.docker.internal:8038/api/v1/mfg/vouchers/43FF320A(or http://host.docker.internal:8039api/v1/mfg/vouchers/43FF320A)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the manufacturer's service.env or can use CLIENT-CERT AUTH (mTLS).

POST content-type `text\plain`

In the request body add owner's certificate.

```
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
```
Response will contain the ownership voucher
```
-----BEGIN OWNERSHIP VOUCHER-----
-----END OWNERSHIP VOUCHER-----
```

Post the extended ownership found obtained from the manufacturer to the owner
POST https://host.docker.internal:8043/api/v1/owner/vouchers (or http://host.docker.internal:8042/api/v1/owner/vouchers)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the owner's service.env or can use CLIENT-CERT AUTH (mTLS).

POST content-type `text\plain`

In the request body add extended ownership voucher
```
-----BEGIN OWNERSHIP VOUCHER-----
-----END OWNERSHIP VOUCHER-----
```
Response body be the uuid of the voucher
Eg: 24275cd7-f9f5-4d34-a2a5-e233ac38db6c

Configure the Owners TO2 address using the following API:

POST https://host.docker.internal:8043/api/v1/owner/redirect (or http://host.docker.internal:8042/api/v1/owner/redirect)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the owner's service.env or can use CLIENT-CERT AUTH (mTLS).

POST content-type `text\plain`

In the request body add Owner T02RedirectAddress.
```
[[null,"host.docker.internal",8043,5]]
```
Response `200 OK`

Trigger owner to perform To0 with the voucher and post the extended ownership found obtained from the manufacturer to the owner

GET https://host.docker.internal:8043/api/v1/to0/24275cd7-f9f5-4d34-a2a5-e233ac38db6c (or http://host.docker.internal:8042/api/v1/to0/24275cd7-f9f5-4d34-a2a5-e233ac38db6c)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the owner's service.env or can use CLIENT-CERT AUTH (mTLS).

Response `200 OK`



#### Configure the owner service info package

Use the following API to configure a service info package.
POST https://host.docker.internal:8043/api/v1/owner/svi (or http://host.docker.internal:8042/api/v1/owner/svi)

For authorization, users can use DIGEST AUTH with "apiUser" and api_password as defined in the owner's service.env or can use CLIENT-CERT AUTH (mTLS).

POST content
```
[
  {"filedesc" : "setup.sh", "resource" : "https://google.com"},
  {"exec" : ["sh","setup.sh"] }
]
```
Response `200 OK`

Now run the device again to onboard the device
```
$ cd <fdo-pri-src>/component-samples/demo/device
$ java -jar device.jar
```


