# Table of Contents
1. [System Requirements](#system-requirements)
2. [Docker Commands](#docker-commands)
3. [Podman Commands](#podman-commands)
4. [Running Demo](#running-demo)
5. [ServiceInfo Setup](#serviceinfo-setup-between-fdo-pri-http-java-device-sample-and-fdo-pri-owner-sample)
6. [Enabling ServiceInfo transfer](#enabling-serviceinfo-transfer)
7. [Customize for multi machine setup](#)

# System Requirements

* Operating System: Ubuntu 20.04 / RHEL 8.4

* Linux packages:

  - Docker engine (minimum version 18.09, Supported till version 20.10.7) / Podman engine (For RHEL)

  - Docker-compose (minimum version 1.21.2) / Podman-compose (For RHEL)

  - Haveged

  - Java 11

  - Maven (minimum 3.6.3)
  

# Docker Commands

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
Update the proxy information in `_JAVA_OPTIONS` as

```
_JAVA_OPTIONS=-Dhttp.proxyHost=http_proxy_host -Dhttp.proxyPort=http_proxy_port -Dhttps.proxyHost=https_proxy_host -Dhttps.proxyPort=https_proxy_port
```

where

`http_proxy_host`: Represents the HTTP proxy hostname. Typically, it is an IP address or domain name in the proxy URL.

`http_proxy_port`: Represents the HTTP proxy port. Typically, it is the port number in the proxy URL.

`https_proxy_host`: Represents the HTTPS proxy hostname. Typically, it is an IP address or domain name in the proxy URL.

`https_proxy_port`: Represents the HTTPS proxy port. Typically, it is the port number in the proxy URL.

Specify the combination of the hostname and the port information together for either HTTP, HTTPS, or both. For example, if the HTTP proxy is 'http://myproxy.com:900', then the following updates will be made to the properties:

http_proxy_host: myproxy.com

http_proxy_port: 900

If no proxy needs to be specified, do not add these properties to your _JAVA_OPTIONS.

# Configuring OnDie (Optional)

OnDie is a type of device that makes use of the MAROE prefix. If you need to support such devices then you will need to configure the FDO PRI demo components by adding/updating the following properties. The values can be specified via Java -Doptions, entries in application.properties or entries in the .env files.

OnDie requires several certificates and CRLs. 

`ondie_cache`: (required if supporting OnDie, optional otherwise) Specifies the path to the directory containing the OnDie certificates and CRLs.


Finally, the `ondie_cache` directory needs to be copied into the docker/podman container, uncomment the following line in `Dockerfile/Podmanfile` of Manufacturer and Owner.
```
COPY ./ondie_cache ./ondie_cache/
```


# Configuring log messages

The components use LOG4J2 for logging. The logging configuration can be updated through log4j2.xml
file stored within each component folder. By default, INFO logs are prints on the console and DEBUG logs
are printed in the log file.

Use following command to extract the log file from a running container.
```
docker container cp <container-id>:/home/fdo/log-filename .
```

# Running Demo

***NOTE***: Use the following commands to enbale FDO support on RHEL.
```
bash scripts/enable_rhel_support.sh
echo $'\nexport PODMAN_USERNS=keep-id' >> ~/.bashrc
source ~/.bashrc
```

1. Start the FDO Manufacturer Sample as per the steps outlined in [Manufacturer README](manufacturer/README.md).

2. Complete Device Initialization (DI) by starting the FDO HTTP Java Device Sample as per the steps outlined in [Device README](device/README.md). Delete any existing 'credentials.bin' before starting the device.

3. Complete Ownership Voucher Extension by using the API `GET /api/v1/mfg/vouchers/<serial_no>` and save the Ownership Voucher. Please refer to [PRI Manufacturer REST APIs](manufacturer/README.md/#fdo-pri-manufacturer-rest-apis) for more information about the API.

4. Start the PRI RV Sample as per the steps outlined in [RV README](rv/README.md).

5. Start the PRI Owner Sample as per the steps outlined in [Owner README](owner/README.md). Import the extended Ownership Voucher from Step#3 into the Owner database by using the API `POST /api/v1/owner/vouchers/`. Please refer to [FDO PRI Owner REST APIs](owner/README.md/#fdo-pri-owner-rest-apis) for more information about the API. Optionally, if ServiceInfo transfer is needed, please refer to [Enabling ServiceInfo transfer](#enabling-serviceinfo-transfer).

6. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FDO PRI HTTP Java Device Sample again. The previously created 'credentials.bin' from Step#2 will be used directly by the Device.

***NOTE***: Credential Reuse protocol is enabled by default, that is, after a successful onboarding the device credentials remain unchanged. To enable the Resale protocol instead, wherein, after a successful onboarding the device credentials are replaced, update the fields `REPLACEMENT_GUID` and/or `REPLACEMENT_RVINFO` in Owner `TO2_DEVICES` table by using the API `POST /api/v1/owner/setupinfo?id=<device_guid>` before starting TO2. Please refer to [FDO PRI Owner REST APIs](owner/README.md/#fdo-pri-owner-rest-apis) for more information about the API.

# ServiceInfo setup between FDO PRI HTTP Java Device Sample and FDO PRI Owner Sample

The FDO PRI HTTP Java Device Sample currently supports `fdo_sys` and `fidoalliance` module for interpreting received owner ServiceInfo and `devmod` module to share device ServiceInfo with Owner.

- `fdo_sys` Owner ServiceInfo module: This module supports the following 3 message names as listed below to interpret the ServiceInfo as received from the Owner. The basic functionality of this module is to support payload/script transfers and basic command execution.  A sample format looks like 'fdo_sys:filedesc=filename, fdo_sys:write=filecontent,fdo_sys:exec=command-to-execute'.

    *filedesc* - The name to be given to the file once it is transferred. Upon receiving this, device creates a file with the given name and opens stream to write into it.

    *write* - The payload/content (script, binaries, and others) that is sent to the device. Upon receiving this, device writes the content into the open stream as given by the preceding 'filedesc' message.

    *exec* - The command that will be executed at the device. Device executes the command as received.

    *exec-cb* - This command returns the status / progress of the process running.


***NOTE***: The comma-separated values must be ordered such that the 'filedesc' and 'write' objects are one after the other pair-wise, followed by the 'exec' commands.

- `devmod` Device ServiceInfo module: This module supports multiple messages as listed down in the protocol specification, that is sent to the Owner as Device ServiceInfo. A sample format looks like 'devmod:active=1'.

- Ensure that before each module the `module_name:active` message is sent as `true` for a successful ServiceInfo transfer. The device will ignore all ServiceInfo module messages if module_name:active doesn't precede the messages of the module.

The FDO PRI Owner Sample currently supports the same `fdo_sys` module to send Owner ServiceInfo to the Device and `devmod` module to store the received Device ServiceInfo. Few sample ServiceInfo values, as present in owner/serviceinfo/sample-values/ are populated into the database table 'OWNER_SERVICEINFO' as byte arrays. For keeping the association between the Device and the ServiceInfo values to transfer, 'GUID_OWNERSVI' database table is used. When a Device is inserted into the database table 'TO2_DEVICES', it'll not have any association with the ServiceInfo values, and so by default, no ServiceInfo is transferred to the Device.

# Enabling ServiceInfo Transfer

To enable ServiceInfo transfer to a Device with a given GUID, follow the steps below:

Insert required ServiceInfo resources into the database table 'SYSTEM_MODULE_RESOURCE' using the API `POST /api/v1/owner/resource?filename=fileName`. More information about the same is provided in section [FDO PRI Owner REST APIs](owner/README.md/#fdo-pri-owner-rest-apis). If the required ServiceInfo already exists in the table with appropriate tags, start TO1.


# Customize for multi-machine setup

While testing for a scenario where different components are running in different
machines, the SSL certificate needs to be updated to include the DNS / IP of
these machines in its SubjectAltName.

