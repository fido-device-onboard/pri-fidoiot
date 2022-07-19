
# FIDO Device Onboard (FDO) Protocol Reference Implementation (PRI) Quick Start

This is a reference implementation of the
[FDO v1.1 Review Draft](https://fidoalliance.org/specs/FDO/FIDO-Device-Onboard-RD-v1.1-20211214)
published by the FIDO Alliance. It provides production-ready implementation for the protocol defined
by the specification. It also provides example implementation for different components to
demonstrate end-to-end execution of the protocol. Appropriate security measures should be taken while
deploying the example implementation for these components.

## System Requirements:

* **Ubuntu (20.04, 22.04) / RHEL (8.4, 8.6) / Debian 11.4**. +
* **Maven 3.6.3**.
* **Java 11**.
* **Haveged**.
* **Docker engine (minimum version 20.10.X, Supported till version 20.10.7) / Podman engine (For RHEL)**
* **Docker-compose (minimum version 1.21.2) / Podman-compose (For RHEL)**

+Supported list of Host operating systems.

***NOTE***: FDO service require strong random number generation in order to perform the required cryptographic functions.  The FDO servers will hang on startup waiting for sufficient entropy unless the system continuously supplied random data.
- `sudo apt-get install -y haveged` will ensure the system fdo services are running on have sufficient entropy.



## Source Layout

For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.

FDO PRI source code is organized into the following sub-folders.

- `component-samples`: It contains all the normative and non-normative server and client implementation with all specifications listed in the base profile.

- `protocol`: It contains implementations related to protocol message processing.


## Building FDO PRI Source

FDO PRI source is written in [Java 11](https://openjdk.java.net/projects/jdk/11/) and uses the
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
| 8049 | manufacturer database port |
| 8050 | rv database port |
| 8051 | owner database port |
| 8070 | reseller http port |
| 8071 | reseller database port |
| 8072 | reseller https port |
| 8080 | aio http port |
| 8082 | aio H2 Console port |
| 8083 | manufacturer H2 Console port |
| 8084 | rv H2 Console port |
| 8085 | owner H2 Console port |
| 8073 | reseller H2 Console port |
| 8443 | aio https port |
| 9092 | aio database port |


Use the following commands to build FDO PRI source.
```
$ cd <fdo-pri-src>
$ mvn clean install
```

or FDO PRI source can be built using docker container. [REFER](./build/README.MD)

The build creates artifacts which will be used in the rest of this guide.

The runnable artifacts can be found in /<fdo-pri-src/>/component-samples/demo/

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

Keystores containing private keys are stored in the H2 database - `<fdo-pri-src>/component-sample/demo/{component}/app-data/emdb.mv.db` file.

keys_gen.sh can be used to generate random passwords for each service.env.

***NOTE***: Changing the database password after the H2 database has been created requires the database file to be deleted and recreated.


### Generating random passwords using keys_gen.sh

Running keys_gen.sh will generate random passwords for the all http servers.

```
$ cd <fdo-pri-src>/component-samples/demo/scripts
$ ./keys_gen.sh
```

A message "Key generation completed." will be displayed on the console.

Credentials for each service will be generated the creds directory `<fdo-pri-src>/component-sample/demo/`.

```
creds/aio/service.env
creds/manufacturer/service.env
creds/owner/service.env
creds/reseller/service.env
creds/rv/service.env
```

Replace the service.env in the demo folders with the generated ones in the creds folder.

```
$ cd <fdo-pri-src>/component-samples/demo/scripts
$ cp -r ./creds/. ../
```

### Specifying Subject alternate names for the Web/HTTPS self-signed certificate.

When the http server starts for the first time it will generate a self-signed certificate for https protocol.

The subject name of the self-signed certificate is defined in the service.yml under 
```
http-server:
    subject_names:
    - DNS:host.docker.internal
    - IP:127.0.0.1
```

Replace or add new DNS and IP entries with the ones required by the HTTPs/Web Service.

```
http-server:
    subject_names:
    - DNS:myhost
    - DNS:myhost2
    - IP:127.0.0.1
    - IP:127.0.0.2
```

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

The server will listen for FDO PRI http & https messages on port 8080 and 8443 respectively.

The H2 database will listen on TCP port 9092.
The H2 Web Console will be available at http://host.docker.internal:8082

The all-in-one supports all FDO protocols in a single service by default. 


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
The H2 database will listen on TCP port 8050.
The H2 Web Console will be available at http://host.docker.internal:8084

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
The H2 database will listen on TCP port 8051.
The H2 Web Console will be available at http://host.docker.internal:8085

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
The H2 database will listen on TCP port 8049.
The H2 Web Console will be available at http://host.docker.internal:8083

You can allow remote database console connections by uncommenting the line containing "-webAllowOthers" in the service.yml


### Running the FDO PRI HTTP Device

#### Staring the FDO PRI HTTP Device

***NOTE***: By default the device is configured to run with the All-In-One (AIO) ports.  You must edit the service.yml in the demo device directory to run with the  manufacturer demo.

To start the PRI device as a standalone java application.

```
$ cd <fdo-pri-src>/component-samples/demo/device
$ java -jar device.jar
```
Running the device for the first time will result in device keys being generated and stored in the current directory in the device.p12 file.
Once device keys are generated the device will run the DI protocol and store the DI credentials in a file called `credentials.bin`.

Running device for a second time will result in the device performing TO1/TO2 protocols.

Deleting the `credentials.bin` file will force the device to re-run DI protocol.


#### Configuring FDO PRI HTTP Device

`<fdo-pri-src>/component-samples/demo/device/service.yml` contains the configuration of the device.


#### Creating Ownership Vouchers using All-In-One (AIO) demo

Before running the device for the first time start the demo aio server.

Run the demo device

As auto injection is enable in AIO by default; the ownership voucher is extended and stored in `ONBOARDING_CONFIG` table and the device is ready for TO1/2.

#### Creating Ownership Vouchers using Individual Component Demos

Before running the device for the first time start the demo manufacturer.

Use the following REST api to specify the rendezvous instructions for demo rv server.

POST https://host.docker.internal:8038/api/v1/rvinfo (or http://host.docker.internal:8039/api/v1/rvinfo)
The post body content-type header `text/plain`
Authorization DIGEST with "apiUser" and api_password defined in the manufacturer's service.env
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
Authorization DIGEST with "apiUser" and api_password defined in the Owner's service.env
Response body will be the Owner's certificate in PEM format


```
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
```


For EC384 based vouchers use the following API:

GET https://host.docker.internal:8043/api/v1/certificate?alias=SECP384R1 (or http://host.docker.internal:8042/api/v1/certificate?alias=SECP384R1)
Authorization DIGEST with "apiUser" and api_password defined in the owners service.env
Result body will be the owners certificate in PEM format

[REFER](https://github.com/secure-device-onboard/pri-fidoiot/tree/master/component-samples/demo/aio#list-of-key-store-alias-values) for the other supported attestation type.

Next, collect the serial number of the last manufactured voucher

GET https://host.docker.internal:8038/api/v1/deviceinfo/{seconds} (or http://host.docker.internal:8039/api/v1/deviceinfo/100000)
Authorization DIGEST with "apiUser" and api_password defined in the manufacturer's service.env
Result will contain the device info
```
[{"serial_no":"43FF320A","timestamp":"2022-02-18 21:50:21.838","uuid":"24275cd7-f9f5-4d34-a2a5-e233ac38db6c"}]
```

Post the PEM Certificate obtained form the owner to the manufacturer to get the ownership voucher transferred to the owner.
POST https://host.docker.internal:8038/api/v1/mfg/vouchers/43FF320A(or http://host.docker.internal:8039api/v1/mfg/vouchers/43FF320A)
Authorization DIGEST with "apiUser" and api_password defined in the manufacturer's service.env
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
Authorization DIGEST with "apiUser" and api_password defined in the owner's service.env
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
Authorization DIGEST with "apiUser" and api_password defined in the owner's service.env
POST content-type `text\plain`

In the request body add Owner T02RedirectAddress.
```
[[null,"host.docker.internal",8043,5]]
```
Response `200 OK`

Trigger owner to perform To0 with the voucher and post the extended ownership found obtained from the manufacturer to the owner

GET https://host.docker.internal:8043/api/v1/to0/24275cd7-f9f5-4d34-a2a5-e233ac38db6c (or http://host.docker.internal:8042/api/v1/to0/24275cd7-f9f5-4d34-a2a5-e233ac38db6c)
Authorization DIGEST with "apiUser" and api_password defined in the owner's service.env
Response `200 OK`



#### Configure the owner service info package

Use the following API to configure a service info package.
POST https://host.docker.internal:8043/api/v1/owner/svi (or http://host.docker.internal:8042/api/v1/owner/svi)
Authorization DIGEST with "apiUser" and api_password defined in the owner's service.env
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


