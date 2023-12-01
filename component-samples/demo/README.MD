

# Table of Contents
1. [System Requirements](#system-requirements)
2. [Docker Commands](#docker-commands)
3. [Podman Commands](#podman-commands)
4. [Configuring Proxies](#configuring-proxies)
5. [Configuring Ondie](#configuring-ondie-optional)
6. [Configuring Log Messages](#configuring-log-messages)   
7. [Running Demo](#running-demo)
8. [ServiceInfo Setup](#serviceinfo-setup-between-fdo-pri-http-java-device-sample-and-fdo-pri-owner-sample)
9. [Enabling ServiceInfo transfer](#enabling-serviceinfo-transfer)
10. [Customize for multi machine setup](#customize-for-multi-machine-setup)
11. [Enabling Access to the embedded database](#enabling-access-to-the-embedded-database)
12. [Certificate Validity Checks](#certificate-validity-checks)
13. [Support for multiple Databases](#support-for-multiple-databases)

# System Requirements

* Host Operating System: Ubuntu (20.04, 22.04) / RHEL (8.6, 8.8) / Debian 11.4

* Linux packages:

  - Docker engine (minimum 20.10.10, Supported till version 20.10.21) / Podman engine (For RHEL) 3.4.2+**

  - Docker-compose (minimum version 1.29.2) / Podman-compose 1.0.3(For RHEL)**

  - Haveged

  - Java 17

  - Maven (minimum 3.6.3)
  

# Docker Commands

NOTE:  `sudo` can be removed for docker commands to instill the principle of least privilege by adding a user to docker group in Ubuntu [REFER](https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user).

## Start Docker
* Use the following command to start the docker container.
```
sudo docker-compose up -d --build
```

## Stop Docker

* Use the following command to stop a specific docker container.
```
sudo docker stop <container-name>
```
OR
```
sudo docker stop <container-id>
```

* Use the following command to stop all running docker containers.
```
sudo docker stop $(sudo docker ps -a -q)
```

## Clean up Containers

* Use the following command to remove a specific container.
```
sudo docker rm <container-name>
```
OR
```
sudo docker rm <container-id>
```

* Use the following command to remove the docker image.
```
sudo docker rmi <image-name>
```
OR
```
sudo docker rmi <image-id>
```

* Use the following command to delete all the docker artifacts. (**Note:** Docker containers must be stopped before deleting them)
```
sudo docker system prune -a
```

# Podman Commands

## Start Podman
* Use the following command to start the podman container.
```
sudo podman-compose up -d --build
```

## Stop Podman

* Use the following command to stop a specific podman container.
```
sudo podman stop <container-name>
```
OR
```
sudo podman stop <container-id>
```

* Use the following command to stop all running podman containers.
```
sudo podman stop -a
```

## Clean up Containers

* Use the following command to remove a specific container.
```
sudo podman rm <container-name>
```
OR
```
sudo podman rm <container-id>
```

* Use the following command to remove the podman image.
```
sudo podman rmi <image-name>
```
OR
```
sudo podman rmi <image-id>
```

* Use the following command to delete all the podman artifacts. (**Note:** podman containers must be stopped before deleting them)
```
sudo podman system prune -a
```
# Configuring Proxies

Follow the steps in hyperlink for complete proxy setup. [REFER](https://fido-device-onboard.github.io/docs-fidoiot/latest/implementation-references/proxy-settings)

For basic proxy setup, update the proxy information in `_JAVA_OPTIONS` as

```
_JAVA_OPTIONS=-Dhttp.proxyHost=http_proxy_host -Dhttp.proxyPort=http_proxy_port -Dhttps.proxyHost=https_proxy_host -Dhttps.proxyPort=https_proxy_port -Dhttp.nonProxyHosts=non_proxy_host
```

where

`http_proxy_host`: Represents the HTTP proxy hostname. Typically, it is an IP address or domain name in the proxy URL.

`http_proxy_port`: Represents the HTTP proxy port. Typically, it is the port number in the proxy URL.

`https_proxy_host`: Represents the HTTPS proxy hostname. Typically, it is an IP address or domain name in the proxy URL.

`https_proxy_port`: Represents the HTTPS proxy port. Typically, it is the port number in the proxy URL.

`non_proxy_host`: List of non-proxy hosts values separated by `|` symbol.

Specify the combination of the hostname and the port information together for either HTTP, HTTPS, or both. For example, if the HTTP proxy is 'http://myproxy.com:900', then the following updates will be made to the properties:

```
http_proxy_host: myproxy.com

http_proxy_port: 900

non_proxy_host: myproxy.com | ip 
```

***NOTE:*** For RHEL, proxy variables need to be configured in `/etc/environment`.

If no proxy needs to be specified, do not add these properties to your _JAVA_OPTIONS.

# Configuring OnDie (Optional)

OnDie is a type of device with device RoT added during manufacturing. To support OnDie devices you will need to import the OnDie certificates into the database via the api on both the Manufacturer and Owner components.
In Ubuntu/RHEL, copy the csme.zip/csme-debug.zip file present in `<fdo-pri-src>/component-samples/demo/aio` to Manufacturer & Owner docker containers manually or by adding the below line to Dockerfile/Podmanfile of Manufacturer & Owner : 
```
cp <fdo-pri-src>/component-samples/demo/aio/csme.zip   <fdo-pri-src>/component-samples/demo/manufacturer
cp <fdo-pri-src>/component-samples/demo/aio/csme.zip   <fdo-pri-src>/component-samples/demo/owner
COPY ./csme.zip ./csme.zip/
```

Restart Docker/Podman containers and execute the below CURL command to upload Ondie artifacts to DB.

    - `POST /api/v1/ondie` to upload OnDie certificates into the database.

      - Ex: `(curl -D - --digest -u ${api_user}:${api_passwd} --location --request POST "http://${ip}:{port}/api/v1/ondie" --header 'Content-Type: text/plain' --data-raw "${cert-file}" `

      where ip = ip address of the manufacturer or the owner
      port = port of the manufacturer or the owner
      cert-file = zip file containing the OnDie certificates (this file is located in the component-samples/demo/aio folder). If using any pre-prod devices then import the csme-debug.zip file. Otherwise, for production devices, import the csme.zip file. Example file reference on Windows: "file:///C:/FDO/component-samples/demo/aio/csme-debug.zip" and on Ubuntu/RHEL : "file:///{path within docker container}/csme.zip".

# Configuring log messages

The components use LOG4J2 for logging. The logging configuration can be updated through log4j2.xml
file stored within each component folder. By default, INFO logs are prints on the console and DEBUG logs
are printed in the log file.

Use following command to extract the log file from a running container.
```
docker container cp <container-id>:/home/fdo/app-data/service.log .
```

# Running Demo

***NOTE***: Use the following commands to enable FDO support on RHEL.
```
cd <fdo-pri-src>/component-samples/demo/scripts
bash scripts/enable_podman_support.sh
grep -qxF 'export PODMAN_USERNS=keep-id' ~/.bashrc || echo $'\nexport PODMAN_USERNS=keep-id' >> ~/.bashrc
source ~/.bashrc
```

***NOTE***:  **Before starting the containers on RHEL, assign your machine IP to host.docker.internal in "/etc/hosts" and replace "host.docker.internal:host-gateway" with "host.docker.internal:your-machine-IP" from the extra_hosts field in the docker-compose.yml file for each component.**

1. Start the FDO Manufacturer Sample as per the steps outlined in [Manufacturer README](manufacturer/README.md).

2. Complete Device Initialization (DI) by starting the FDO HTTP Java Device Sample as per the steps outlined in [Device README](device/README.md). Delete any existing 'credentials.bin' before starting the device.

3. Start the PRI RV Sample as per the steps outlined in [RV README](rv/README.md).

4. Start the PRI Owner Sample as per the steps outlined in [Owner README](owner/README.md).
   
5. Complete Ownership Voucher Extension by using the APIs

    - `GET /api/v1/certificate?alias=<attestation-type>` to collect Owner's certificate.
      
      - Ex: `curl -D - --digest -u ${api_user}: --location --request GET 'http://${owner_ip}:8042/api/v1/certificate?alias=SECP256R1'`
 
    - `POST /api/v1/mfg/vouchers/<serial_no>` to collect voucher. 
 
       - Ex: `curl -D - --digest -u ${api_user}:${api_passwd} --location --request POST "http://${mfg_ip}:8039/api/v1/mfg/vouchers/${serial_no}" --header 'Content-Type: text/plain' --data-raw "$owner_certificate" -o ${serial_no}_voucher.txt` 
    
    - `POST /api/v1/owner/vouchers/` to upload ownership voucher to Owner.
      
      - Ex: `curl -D - --digest -u ${api_user}:${api_passwd} --location --request POST "http://${owner_ip}:8042/api/v1/owner/vouchers/" --header 'Content-Type: text/plain' --data-raw "$extended_voucher" -o ${serial_no}_guid.txt`
  
    - `GET /api/v1/to0/${device_guid}` to initiate TO0
  
      - Ex: `curl -D - --digest -u ${api_user}:${api_passwd} --location --request GET "http://${owner_ip}:8042/api/v1/to0/${device_guid}" `
  
     - Or Extend and upload ownership voucher using `extend_upload.sh` script present in `<fdo-pri-src>/component-samples/demo/scripts`
  
       - Ex: `bash extend_upload.sh -m ${mfg_ip} -o ${owner_ip} -s abcdef`
   
    - Please refer to [PRI Manufacturer REST APIs](manufacturer/README.md/#fdo-pri-manufacturer-rest-apis) for more information about the API. Optionally, if ServiceInfo transfer is needed, please refer to [Enabling ServiceInfo transfer](#enabling-serviceinfo-transfer).

6. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FDO PRI HTTP Java Device Sample again. The previously created 'credentials.bin' from Step#2 will be used directly by the Device.

***NOTE***: Credential Reuse protocol is enabled by default, that is, after a successful onboarding the device credentials remain unchanged.

# ServiceInfo setup between FDO PRI HTTP Java Device Sample and FDO PRI Owner Sample

The FDO PRI HTTP Java Device Sample currently supports `fdo_sys` and `FSIM` module for interpreting received owner ServiceInfo and `devmod` module to share device ServiceInfo with Owner.

- `fdo_sys` Owner ServiceInfo module: This module supports the following 4 message names as listed below to interpret the ServiceInfo as received from the Owner. The basic functionality of this module is to support payload/script transfers and basic command execution.  A sample format looks like 'fdo_sys:filedesc=filename, fdo_sys:write=filecontent,fdo_sys:exec=command-to-execute,fdo_sys:exec_cb=command-to-execute'.

    *filedesc* - The name to be given to the file once it is transferred. Upon receiving this, device creates a file with the given name and opens stream to write into it.

    *write* - The payload/content (script, binaries, and others) that is sent to the device. Upon receiving this, device writes the content into the open stream as given by the preceding 'filedesc' message.

    *exec* - The command that will be executed at the device. Device executes the command as received.

    *exec-cb* - This command returns the status / progress of the process running.

    *fetch* - This command returns the fetched file from client filesystem.

    - The fetched file is stored in `owner/app-data/<device_guid>`

    *owner_exec* - This command is a generic execution module that can perform various tasks, such as downloading files, running scripts, and executing commands. For executing specific package add the package in the respective Dockers.


    Sample SVI instruction :
  
    `{"filedesc" : "sample.txt","resource" : "http/https url"},
     {"exec" :"cat sample.txt"}`

    or
  
     `{"filedesc" : "sample.txt","resource" : "database resource"},
      {"exec" :"cat sample.txt"}`

    Sample SVI instruction (with owner_exec):

    `{ "owner_exec" : ["curl", "—location —digest", "-U username: -P:xyz", "https://sample:9999/files"] }`

***NOTE***: The comma-separated values must be ordered such that the 'filedesc' and 'write' objects are one after the other pair-wise, followed by the 'exec' commands.

***NOTE***: You can filter SVI transfer based on Device parameters. [Learn more](aio/README.md#service-info-filters)

- `devmod` Device ServiceInfo module: This module supports multiple messages as listed down in the protocol specification[REFER](https://fidoalliance.org/specs/FDO/FIDO-Device-Onboard-PS-v1.1-20220419/FIDO-Device-Onboard-PS-v1.1-20220419.html#the-devmod-module), that is sent to the Owner as Device ServiceInfo. A sample format looks like 'devmod:active=1'.

The FDO PRI Owner Sample currently supports `fdo_sys` and `FSIM` modules for sending serviceInfo to the PRI Device and `devmod` module to store the received Device ServiceInfo. When device information is inserted into the database table 'ONBOARDING_CONFIG', it'll not have any association with the ServiceInfo values, and so by default, no ServiceInfo is transferred to the Device.

# Preview implementation of FSIM ServiceInfo modules

The FDO PRI Owner and PRI Java Device  currently supports FSIM modules: `fdo.download`, `fdo.command`.

  *fdo.download* - sends payload/content (script, binaries, and others) from Owner to the device. Upon receiving this, device writes the content into the open stream as given by the preceding 'filedesc' message.

  *fdo.command* - The command that will be executed at the device. Device executes the command as received.

  ### Configuring FSIM ServiceInfo modules

Activate the following workers in Device's `service.yml`:
```yaml
- org.fidoalliance.fdo.sample.FdoSimDownloadDeviceModule
- org.fidoalliance.fdo.sample.FdoSimCommandDeviceModule
```
and Activate the following workers in Owner's `service.yml`

```yaml
- org.fidoalliance.fdo.sample.FdoSimDownloadOwnerModule
- org.fidoalliance.fdo.sample.FdoSimCommandOwnerModule
```

 ### Sample SVI instruction :

  `[{"filedesc" : "script.bat","resource" : "script.bat", "module":"fdo.download"}]`

  or

  `[{"filedesc" : "script.bat","resource" : "script.bat", "module":"fdo.download"}, {"exec_cb" : ["cmd", "/c", "script.bat"], "module":"fdo.command"}]`

***NOTE***: Currently, FDO_SYS and FSIM modules cannot be used simultaneously. Disable FSIM modules if you are using FDO_SYS. You can filter SVI transfer based on Device parameters. [Learn more](aio/README.md#service-info-filters)

# Executing cURL request with mTLS

Generic sample Digest authentication call:

  ```shell
    curl -D - --digest -u ${apiUser}:${api_password} --location --request POST 'http://host.docker.internal:8039/api/v1/rvinfo' \
    --header 'Content-Type: text/plain' \
    --data-raw '[[[5,"{rv-dns}"],[3,8040],[12,1],[2,"{rv-ip}"],[4,8040]]]'
  ```

Replace `-D - --digest -u ${apiUser}:${api_password}` with `--cacert path-to-CA --cert path-to-client-Certificate`

Finally, the mTLS enabled cURL request will be:

  ```shell
    curl \
    --cacert scripts/secrets/ca-cert.pem --cert scripts/api-user.pem \
     --location --request POST 'http://host.docker.internal:8039/api/v1/rvinfo' \
    --header 'Content-Type: text/plain' \
    --data-raw '[[[5,"{rv-dns}"],[3,8040],[12,1],[2,"{rv-ip}"],[4,8040]]]'
  ```

# Loading Keystores from FileSystem

1. Copy the required Keystores into `demo/<fdo-component>/app-data` folder.

2. Disable the following workers.

```
org.fidoalliance.fdo.protocol.db.StandardKeyStoreInputStream
org.fidoalliance.fdo.protocol.db.StandardKeyStoreOutputStream
```

3. Enable the following workers.

```
org.fidoalliance.fdo.FileKeyStoreInputStream
org.fidoalliance.fdo.FileKeyStoreOutputStream
```

4. Update `service.yml` with the new Keystore filename and credentials in `service.env`

```
   keystore:
     path: new-keystore-filename
     store-type: PKCS12
     password: $(encrypt_password)
```

# Enabling ServiceInfo Transfer

To enable ServiceInfo transfer to a Device with a given GUID, follow the steps below:

Insert required ServiceInfo resources into the database table 'SYSTEM_RESOURCE' using the API `POST /api/v1/owner/resource?filename=fileName`. More information about the same is provided in section [FDO PRI Owner REST APIs](owner/README.md/#fdo-pri-owner-rest-apis). If the required ServiceInfo already exists in the table with appropriate tags, start TO1.

```
curl -D - --digest -u apiUser:${api_password} --location --request POST 'http://{owner_ip}:{owner_port}/api/v1/owner/resource?filename=<file-name>' 
--header 'Content-Type: text/plain' --data-binary '@<path-to-file>'
```

**NOTE:** In case of transferring large payload file via ServiceInfo, there is a possibility that runnning container might hit memory limit set in the respective docker-compose.yml file.
This can be confirmed using the `docker stats` command that shows live resource statistics for running containers.

During SVI Transfer, observe the column `MEM USAGE / LIMIT` for respective container when SVI transmission is in progress.
If the failure is observed when MEM USAGE reaches the LIMIT, it is due to the memory limit we have allocated for the container.
To resolve this, edit fields `mem_limit` and `mem_reservation` in the respective docker-compose.yml according to the needs and restart the container.

# Connecting to external resources during SVI

When connecting to the external signed resource, the application should be using the default java ca-certs instead of the custom truststore. The following properties in `service.yml` need to be commented out to pick the default java ca-certs.

  ```
    #javax.net.ssl.keyStorePassword: default
    #javax.net.ssl.trustStorePassword: default
    #javax.net.ssl.keyStore: $(secrets.path)/api-user.pem
    #javax.net.ssl.trustStore: $(secrets.path)/ca-cert.pem
    #javax.net.ssl.keyStoreType: PEM
    #javax.net.ssl.trustStoreType: PEM
  ```

To support external signed resources along with self-signed resource, add the self-signed certificate to the default java ca-certs. Add following lines to the `Dockerfile` of individual component.

```
COPY secrets/ca-cert.pem /home/fdo/ca-cert.pem

RUN keytool -import -trustcacerts -noprompt -keystore "/usr/lib/jvm/java-17-openjdk-amd64/lib/security/cacerts" -storepass changeit -alias tomcat -file /home/fdo/ca-cert.pem
```


# Customize for multi-machine setup

While customizing the system configuration for multi machine setup, ensure that all `host.docker.internal` and `172.17.0.1` references are replaced with respective DNS and IP address of the host machine. The required changes includes :

- Updating RVInfo blob in Manufacturer

  - Sample cURL command to update RVInfo in Manufacturer running in the host.docker.internal.
    ```shell
    curl -D - --digest -u ${apiUser}:${api_password} --location --request POST 'http://host.docker.internal:8039/api/v1/rvinfo' \
    --header 'Content-Type: text/plain' \
    --data-raw '[[[5,"{rv-dns}"],[3,8040],[12,1],[2,"{rv-ip}"],[4,8040]]]'
    ```

- Updating T02RVBlob in Owner

    - Sample cURL command to update T02RVBlob in Owner running in the host.docker.internal.
      ```shell
      curl -D - --digest -u ${apiUser}:${api_password} --location --request POST 'http://host.docker.internal:8042/api/v1/owner/redirect' \
      --header 'Content-Type: text/plain' \
      --data-raw '[["{owner-ip}","{owner-dns}",8042,3]]'
      ```

# Enable Embedded H2 Database Server

1. Activate `org.fidoalliance.fdo.protocol.EmbeddedDatabaseServer` worker in `service.yml` file.

2. Change hibernate properties in `service.yml` to connect with H2 database:

    ```
    hibernate-properties:
      connection.driver_class: org.h2.Driver
      hibernate.connection.username: $(db_user)
      hibernate.connection.password: $(db_password)
      hibernate.connection.url: jdbc:h2:tcp://localhost:9092/./app-data/emdb
      hibernate.dialect: org.hibernate.dialect.H2Dialect
    ```

3. Append the following `h2-database` configuration to `service.yml` file.

    ```
    h2-database:
       tcp-server:
         - "-ifNotExists"
       web-server:
         - "-webPort"
         - "8082"
    ```

4. In `hibernate.cfg.yml`, ensure that property `hibernate.connection.driver_class` is mapped to `org.h2.Driver`.

***NOTE:*** It is recommended to configure H2 DB with the default settings for properties like webAllowOthers, either in a config file or as a parameter passed in, when the servlet is invoked

# Certificate Validity checks

For out of the box demo purposes, FDO services are configured to trust self-signed certificates.
In production environments, the configurators should disable the trust for these self-signed certificates by updating the worker list in `service.yml` file of respective component.

Sample `service.yml` file:

Disable the following workers
```
#- org.fidoalliance.fdo.protocol.SelfSignedHttpClientSupplier
#- org.fidoalliance.fdo.protocol.db.BasicServiceInfoClientSupplier
```
and enable
```
- org.fidoalliance.fdo.protocol.StandardHttpClientSupplier
- org.fidoalliance.fdo.protocol.db.StandardServiceInfoClientSupplier
```

The Device certificate chain is checked during TO0 by the rendezvous server and by the owner during TO2.  This check includes verifying the certificate chain hash and if the leaf certificate has expired. 
Owner certificates with X5Chain encoding are not analyzed by this implementation.

The FDO spec states "When processing an Ownership Voucher with X5CHAIN encoding, the Rendezvous Server and Device SHOULD verify the certificate chain as much as possible, and MAY decide to accept or reject the Ownership Voucher based on a trust analysis of the X5CHAIN". [REFER](https://fidoalliance.org/specs/FDO/FIDO-Device-Onboard-PS-v1.1-20220419/FIDO-Device-Onboard-PS-v1.1-20220419.html#the-devmod-module:~:text=When%20processing%20an%20Ownership%20Voucher%20with%20X5CHAIN%20encoding%2C%20the%20Rendezvous%20Server%20and%20Device%20SHOULD%20verify%20the%20certificate%20chain%20as%20much%20as%20possible%2C%20and%20MAY%20decide%20to%20accept%20or%20reject%20the%20Ownership%20Voucher%20based%20on%20a%20trust%20analysis%20of%20the%20X5CHAIN.)

This implementation will trust Ownership vouchers with X5Chain encoding without verifying the ownership chain. 

# Support for multiple Databases.

FDO services support multiple databases for the storage layer. Hibernate framework is used to bridge the application layer with backend database storage layer. Out of the box, the following databases are supported by the FDO service:

  + H2 Database
  + MariaDB
  + MySQL
  + PostgreSQL


1. Follow the below steps to configure FDO services with MySQL database: 

   a. Make sure that `connection.driver_class` is commented out in `hibernte.cfg.xml`.

   b. Update `hibernate-properties` in `service.yml`

      ```yaml
      hibernate-properties:
        hibernate.connection.username: fdo
        hibernate.connection.password: sample_password
        hibernate.connection.driver_class: com.mysql.cj.jdbc.Driver
        hibernate.connection.url: jdbc:mysql://host.docker.internal:3306/fdo?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
        hibernate.dialect: org.hibernate.dialect.MySQLDialect
        hibernate.dialect.storage_engine: innodb
        hibernate.connection.requireSSL: false
        hibernate.connection.autoReconnect: true
      ```

    c. Make sure that `useSSL` and `requireSSL` is set to `false` in `service.env` file.

    d. Use the below command to start MySQL server

      ```shell
      docker run -d -p 3306:3306 --name mysql-docker-container -e MYSQL_ROOT_PASSWORD=root-password -e MYSQL_DATABASE=fdo -e MYSQL_USER=sample-user -e MYSQL_PASSWORD=sample-password mysql/mysql-server:8.0.31
      ```

      For RHEL environment,

      ```shell
      sudo podman run -d -p 3306:3306 --name mysql-docker-container -e MYSQL_ROOT_PASSWORD=root-password -e MYSQL_DATABASE=fdo -e MYSQL_USER=sample-user -e MYSQL_PASSWORD=sample-password mysql/mysql-server:8.0.31
      ```


2. Follow the below steps to configure FDO services with PostgreSQL database: 

   a. Make sure that `connection.driver_class` is commented out in `hibernte.cfg.xml`.

   b. Update `hibernate-properties` in `service.yml`

      ```yaml
      hibernate-properties:
        hibernate.connection.username: fdo
        hibernate.connection.password: sample_password
        connection.driver_class: org.postgresql.Driver
        hibernate.connection.autoReconnect: true
        hibernate.connection.requireSSL: false
        hibernate.connection.url: jdbc:postgresql://host.docker.internal:5432/fdo?createDatabaseIfNotExist=true&useSSL=false
        hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
      ```


    c. Make sure that `useSSL` and `requireSSL` is set to `false` in `service.env` file.

    d. Use the below command to start PostgreSQL server

      ```shell
      docker run -d --name postgres-db -e POSTGRES_ROOT_PASSWORD=root-password -e POSTGRES_DB=fdo -e POSTGRES_USER=sample-user -e POSTGRES_PASSWORD=sample-password -p 5432:5432 -d postgres
      ```

      For RHEL environment,

      ```shell
      sudo podman run -d --name postgres-db -e POSTGRES_ROOT_PASSWORD=root-password -e POSTGRES_DB=fdo -e POSTGRES_USER=sample-user -e POSTGRES_PASSWORD=sample-password -p 5432:5432 -d postgres
      ```

3. Follow the [guide](./db/README.md) to configure FDO services with MariaDB. By default, FDO services are configured to support MariaDB.

4. Follow the [Embedded DB guide](#enable-embedded-h2-database-server) to configure FDO services with H2 Database.
 
