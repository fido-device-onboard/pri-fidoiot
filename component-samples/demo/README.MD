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

# System Requirements

* Operating System: Ubuntu 20.04 / RHEL 8.4

* Linux packages:

  - Docker engine (minimum version 18.09, Supported till version 20.10.7) / Podman engine (For RHEL)

  - Docker-compose (minimum version 1.21.2) / Podman-compose (For RHEL)

  - Haveged

  - Java 11

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
podman-compose up -d --build
```

## Stop Podman

* Use the following command to stop a specific podman container.
```
podman stop <container-name>
```
OR
```
podman stop <container-id>
```

* Use the following command to stop all running podman containers.
```
podman stop -a
```

## Clean up Containers

* Use the following command to remove a specific container.
```
podman rm <container-name>
```
OR
```
podman rm <container-id>
```

* Use the following command to remove the podman image.
```
podman rmi <image-name>
```
OR
```
podman rmi <image-id>
```

* Use the following command to delete all the podman artifacts. (**Note:** podman containers must be stopped before deleting them)
```
podman system prune -a
```
# Configuring Proxies

Follow the steps in hyperlink for complete proxy setup. [REFER](https://secure-device-onboard.github.io/docs-fidoiot/latest/implementation-references/proxy-settings)

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

If no proxy needs to be specified, do not add these properties to your _JAVA_OPTIONS.

# Configuring OnDie (Optional)

OnDie is a type of device with device RoT added during manufacturing. To support OnDie devices you will need to import the OnDie certificates into the database via the api on both the Manufacturer and Owner components.
In Ubuntu/RHEL, copy the csme.zip/csme-debug.zip file present in `<fdo-pri-src>/component-samples/demo/aio` to Manufacturer & Owner docker containers manually or by adding the below line to Dockerfile/Podmanfile of Manufacturer & Owner : 
```
cp <fdo-pri-src>/component-samples/demo/aio/csme.zip   <fdo-pri-src>/component-samples/demo/manufacturer
cp <fdo-pri-src>/component-samples/demo/aio/csme.zip   <fdo-pri-src>/component-samples/demo/owner
COPY ./csme.zip ./csme.zip`/
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
docker container cp <container-id>:/home/fdo/log-filename .
```

# Running Demo

***NOTE***: Use the following commands to enable FDO support on RHEL.
```
cd <fdo-pri-src>/component-samples/demo/scripts
bash scripts/enable_podman_support.sh
grep -qxF 'export PODMAN_USERNS=keep-id' ~/.bashrc || echo $'\nexport PODMAN_USERNS=keep-id' >> ~/.bashrc
source ~/.bashrc
```

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

The FDO PRI HTTP Java Device Sample currently supports `fdo_sys` module for interpreting received owner ServiceInfo and `devmod` module to share device ServiceInfo with Owner.

- `fdo_sys` Owner ServiceInfo module: This module supports the following 4 message names as listed below to interpret the ServiceInfo as received from the Owner. The basic functionality of this module is to support payload/script transfers and basic command execution.  A sample format looks like 'fdo_sys:filedesc=filename, fdo_sys:write=filecontent,fdo_sys:exec=command-to-execute,fdo_sys:exec_cb=command-to-execute'.

    *filedesc* - The name to be given to the file once it is transferred. Upon receiving this, device creates a file with the given name and opens stream to write into it.

    *write* - The payload/content (script, binaries, and others) that is sent to the device. Upon receiving this, device writes the content into the open stream as given by the preceding 'filedesc' message.

    *exec* - The command that will be executed at the device. Device executes the command as received.

    *exec-cb* - This command returns the status / progress of the process running.

    Sample SVI instruction :
  
    `{"filedesc" : "sample.txt","resource" : "http/https url"},
     {"write": "sample.txt"},
     {"exec" :"cat sample.txt"}`

    or
  
     `{"filedesc" : "sample.txt","resource" : "database resource"},
      {"write": "sample.txt"},
      {"exec" :"cat sample.txt"}`

***NOTE***: The comma-separated values must be ordered such that the 'filedesc' and 'write' objects are one after the other pair-wise, followed by the 'exec' commands.

- `devmod` Device ServiceInfo module: This module supports multiple messages as listed down in the protocol specification[REFER](https://fidoalliance.org/specs/FDO/FIDO-Device-Onboard-RD-v1.1-20211214/#the-devmod-module), that is sent to the Owner as Device ServiceInfo. A sample format looks like 'devmod:active=1'.

- Ensure that before each module the `module_name:active` message is sent as `true` for a successful ServiceInfo transfer. The device will ignore all ServiceInfo module messages if module_name:active doesn't precede the messages of the module.

The FDO PRI Owner Sample currently supports `fdo_sys` module for sending serviceInfo to the PRI Device and `devmod` module to store the received Device ServiceInfo. When device information is inserted into the database table 'ONBOARDING_CONFIG', it'll not have any association with the ServiceInfo values, and so by default, no ServiceInfo is transferred to the Device.

# Enabling ServiceInfo Transfer

To enable ServiceInfo transfer to a Device with a given GUID, follow the steps below:

Insert required ServiceInfo resources into the database table 'SYSTEM_RESOURCE' using the API `POST /api/v1/owner/resource?filename=fileName`. More information about the same is provided in section [FDO PRI Owner REST APIs](owner/README.md/#fdo-pri-owner-rest-apis). If the required ServiceInfo already exists in the table with appropriate tags, start TO1.

```
curl -D - --digest -u apiUser:${api_password} --location --request POST 'http://{owner_ip}:{owner_port}/api/v1/owner/resource?filename=<file-name>' 
--header 'Content-Type: text/plain' --data-binary '@<path-to-file>'
```


# Customize for multi-machine setup

While customizing the system configuration for multi machine setup, ensure that all `localhost` and `127.0.0.1` references are replaced with respective DNS and IP address of the host machine. The required changes includes :

- Updating RVInfo blob in Manufacturer

  - Sample cURL command to update RVInfo in Manufacturer running in the localhost.
    ```shell
    curl -D - --digest -u ${apiUser}:${api_password} --location --request POST 'http://localhost:8039/api/v1/rvinfo' \
    --header 'Content-Type: text/plain' \
    --data-raw '[[[5,"{rv-dns}"],[3,8040],[12,1],[2,"{rv-ip}"],[4,8040]]]'
    ```

- Updating T02RVBlob in Owner

    - Sample cURL command to update T02RVBlob in Owner running in the localhost.
      ```shell
      curl -D - --digest -u ${apiUser}:${api_password} --location --request POST 'http://localhost:8042/api/v1/owner/redirect' \
      --header 'Content-Type: text/plain' \
      --data-raw '[["{owner-ip}","{owner-dns}",8042,3]]'
      ```

- Updating subject_name in SSL certificate via `service.yml`

  - 
    ``` 
    //Updates subject alternative information in the certificate.
    subject_name:
      DNS:<DNS-of-host-machine>
      IP:<IP-address-of-host-machine>
    ```


# Enabling access to the Embedded database

Remote access to H2 Storage DB has been disabled by default. Enabling the remote access creates a security hole in the system which makes it vulnerable to Remote Code Execution.

To enable remote access to DB update the webAllowOthers properties in the `service.yml` :

```shell
# sample H2 DB configurations
h2-database:
   tcp-server:
     - "-ifNotExists"
     - "-tcpPort"
     - "9093"
   web-server:
     - "-webAllowOthers"
     - "-webExternalNames"
     - "-ifNotExists"
     - "-webPort"
     - "8083"
```

**NOTE:** Access of DB from local machine is enabled by default and can be accessed from browser using the address `http://localhost:<webport>`.

NOTE: Not recommended to enable this setting especially on production systems.


# Certificate Validity checks

The Device certificate chain is checked during TO0 by the rendezvous server and by the owner during TO2.  This check includes verifying the certificate chain hash and if the leaf certificate has expired. 
Owner certificates with X5Chain encoding are not analyzed by this implementation.

The FDO spec states "When processing an Ownership Voucher with X5CHAIN encoding, the Rendezvous Server and Device SHOULD verify the certificate chain as much as possible, and MAY decide to accept or reject the Ownership Voucher based on a trust analysis of the X5CHAIN". [REFER](https://fidoalliance.org/specs/FDO/FIDO-Device-Onboard-RD-v1.1-20211214/#:~:text=When%20processing%20an%20Ownership%20Voucher%20with%20X5CHAIN%20encoding%2C%20the%20Rendezvous%20Server%20and%20Device%20SHOULD%20verify%20the%20certificate%20chain%20as%20much%20as%20possible%2C%20and%20MAY%20decide%20to%20accept%20or%20reject%20the%20Ownership%20Voucher%20based%20on%20a%20trust%20analysis%20of%20the%20X5CHAIN)

This implementation will trust Ownership vouchers with X5Chain encoding without verifying the ownership chain. 


