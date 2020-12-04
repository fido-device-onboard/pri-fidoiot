# Table of Contents
1. [System Requirements](#system-requirements)
2. [Docker Commands](#docker-commands)
3. [Inserting Keys into Keystore](#inserting-keys-into-keystore)
4. [Running Demo](#running-demo)
5. [Service Info Setup](#service-info-setup-between-fido-iot-http-java-device-sample-and-fido-iot-owner-sample)
6. [Enabling Service info transfer](#enabling-service-info-transfer)

# System Requirements

* Operating system: Ubuntu 20.04.

*  Linux packages:<br/>
`Docker engine (minimum version 18.09)` <br/>
`Docker-compose (minimum version 1.21.2)`<br/>

# Docker commands

## Start Docker
* Use the following command to start the docker container.
```
$ sudo docker-compose up -d --build
```

## Stop Docker

* Use the following command to stop a specific docker container.
```
$ sudo docker stop <container-name>
OR
$ sudo docker stop <container-id>
```

* Use the following command to stop all running docker containers.
```
$ sudo docker stop $(sudo docker ps -a -q)
```

## Clean up Containers

* Use the following command to remove a specific container.
```
$ sudo docker rm <container-name>
OR
$ sudo docker rm <container-id>
```

* Use the following command to remove the docker image.
```
$ sudo docker rmi <image-name>
OR
$ sudo docker rmi <image-id>
```

* Use the following command to delete all the docker artifacts. (Note: docker containers must be stopped before deleting them)
```
$ sudo docker system prune -a
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

# Inserting Keys into Keystore

Assuming that there is already an existing certificate named 'certificate.pem' and the corresponding private key 'private-key.pem', follow these steps to insert them as 'PrivateKeyEntry' into the keystore 'dest-keystore.p12':

**Step 1:** Convert the certificate and private key into 'PKCS12' format:

`$ openssl pkcs12 -export -in certificate.pem -inkey private-key.pem -name newkeypair -out src-keystore.p12`

**Step 2:** Delete an existing alias (only needed if there is a need to replace PrivateKeyEntry having a particular algorithm):

`$ keytool -delete -alias newkeypair -keystore keystore.jks`

**Step 3:** Import the above generated source PKCS12 file into the existing destination keystore file 'dest-keystore.p12' located with alias 'newkeypair'.

`$ keytool -importkeystore -destkeystore path/to/dest-keystore.p12 -srckeystore src-keystore.p12 -srcstoretype PKCS12 -alias newkeypair`

***NOTE*** The password entered in Step 1 to generate the src-keystore.p12 must be the same as that of dest-keystore.p12, that is, the password of the newly created keystore must match the existing keystore where it will be imported to.

# Running Demo

1. Start the FIDO IoT Manufacturer Sample as per the steps outlined in [Manufacturer README](manufacturer/README.md).

2. Complete Device Initialization (DI) by starting the FIDO IoT HTTP Java Device Sample as per the steps outlined in [Device README](device/README.md). Delete any existing 'credential.bin' before starting the device.

3. Complete Ownership Voucher Extension by using the API `GET /api/v1/vouchers/<serial_no>` and save the Ownership Voucher. By default, existing customer with customer Id '1', is assigned to the device. To add a new customer and assign the inserted customer to the device, please refer to [FIDO IoT Manufacturer REST APIs](manufacturer/README.md/#fido-iot-manufacturer-rest-apis) for more information about the API.

4. Start the FIDO IoT RV Sample as per the steps outlined in [RV README](rv/README.md).

5. Start the FIDO IoT Owner Sample as per the steps outlined in [Owner README](owner/README.md). Import the extended Ownership voucher from Step#3 into the Owner database by using the API `POST /api/v1/owner/vouchers/`. Please refer to [FIDO IoT Owner REST APIs](owner/README.md/#fido-iot-owner-rest-apis) for more information about the API. Optionally, if service info transfer is needed, please refer to [Enabling Service info transfer](#enabling-service-info-transfer).

6. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FIDO IoT HTTP Java Device Sample again. The previously created 'credential.bin' from Step#2 will be used directly by the Device.

**NOTE** Reuse is enabled by default.

# Running Resale Demo

There are 2 methods of performing resale:

**Method 1**:

1. Ensure that TO2 is complete using the instructions listed in [Running Demo](#running-demo) section.

2. To disable reuse and enable resale, update the `REPLACEMENT_GUID` field in Owner `TO2_DEVICES` field.

3. Get the new ownership voucher using the owner API `GET /api/v1/owner/newvoucher/?id=<device_guid>`.

4. Start the FIDO IoT Reseller Sample as per the steps outlined in [Reseller README](reseller/README.md).

5. Add the new size 0 ownership voucher to the reseller database table `RT_DEVICES` using reseller API `POST /api/v1/resell/vouchers/<serial_number>`

6. If the reseller key to be used for voucher extension is not present in `RT_CUSTOMER` table, add it using reseller API `POST /api/v1/resell/keys/?alias=<keystore_alias>`.

7. Extend the voucher using the reseller API `GET /api/v1/resell/vouchers/<serial_number>?id=<customer_id>`.

8. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FIDO IoT HTTP Java Device Sample.

**Method 2**:

1. Start the FIDO IoT Manufacturer Sample as per the steps outlined in [Manufacturer README](manufacturer/README.md).

2. Complete Device Initialization (DI) by starting the FIDO IoT HTTP Java Device Sample as per the steps outlined in [Device README](device/README.md). Delete any existing 'credential.bin' before starting the device.

3. Complete Ownership Voucher Extension by using the API `GET /api/v1/vouchers/<serial_no>` and save the Ownership Voucher. By default, existing customer with customer Id '1', is assigned to the device. To add a new customer and assign the inserted customer to the device, please refer to [FIDO IoT Manufacturer REST APIs](manufacturer/README.md/#fido-iot-manufacturer-rest-apis) for more information about the API.

4. Start the FIDO IoT Reseller Sample as per the steps outlined in [Reseller README](reseller/README.md).

5. Add the extended ownership voucher to the reseller database table `RT_DEVICES` using reseller API `POST /api/v1/resell/vouchers/<serial_number>`.

6. If the reseller key to be used for voucher extension is not present in `RT_CUSTOMER` table, add it using reseller API `POST /api/v1/resell/keys/?alias=<keystore_alias>`.

7. Extend the voucher using the reseller API `GET /api/v1/resell/vouchers/<serial_number>?id=<customer_id>`.

8. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FIDO IoT HTTP Java Device Sample again.

# Service info setup between FIDO IoT HTTP Java Device Sample and FIDO IoT Owner Sample

The FIDO IoT HTTP Java Device Sample currently supports `sdo_sys` module for interpreting received owner service info and `devmod` module to share device service info with Owner.

- `sdo_sys` Owner service info module: This module supports the following 3 message names as listed below to interpret the service info as received from the Owner. The basic functionality of this module is to support payload/script transfers and basic command execution.  A sample format looks like 'sdo_sys:filedesc=filename, sdo_sys:write=filecontent,sdo_sys:exec=command-to-execute'.

    *filedesc* - The name to be given to the file once it is transferred. Upon receiving this, device creates a file with the given name and opens stream to write into it.

    *write* - The payload/content (script, binaries, and others) that is sent to the device. Upon receiving this, device writes the content into the open stream as given by the preceeding 'filedesc' message.

    *exec* - The command that will be executed at the device. Device executes the command as received.

***NOTE*** The comma-separated values must be ordered such that the 'filedesc' and 'write' objects are one after the other pair-wise, followed by the 'exec' commands.

- `devmod` Device service info module: This module supports multiple messages as listed down in the protocol specification, that are sent to the Owner as Device Service info. A sample format looks like 'devmod:active=1'.

The FIDO IoT Owner Sample currently supports the same `sdo_sys` module to send Owner service info to the Device and `devmod` module to store the received Device Service info. Few sample service info values, as present in \<fido-iot-src\>/demo/owner/serviceinfo/sample-values/ are populated into the database table 'OWNER_SERVICEINFO' as byte arrays. For keeping the association between the Device and the service info values to transfer, 'GUID_OWNERSVI' database table is used. When a Device is inserted into the database table 'TO2_DEVICES', it'll not have any association with the service info values, and so by default, no service info is transferred to the Device.

# Enabling Service info transfer

To enable service info transfer to a Device with a given GUID, follow the steps below:

1. (Optional) Insert required Service info values into the database table 'OWNER_SERVICEINFO' using the API `POST /api/v1/owner/svivalues/?id=<serviceinfo_id>&isCborEncoded=<boolean_value>`. More information about the same is provided in section [FIDO IoT Owner REST APIs](owner/README.md/#fido-iot-owner-rest-apis). If the required service info already exists in the table, go on to the next step.

2. (Mandatory) Insert required association between the Device and Service info values to transfer using the API `POST /api/v1/owner/svi/?guid=<guid>`. More information about the same is provided in section [FIDO IoT Owner REST APIs](owner/README.md/#fido-iot-owner-rest-apis). As a referance, please see \<fido-iot-src\>/demo/owner/serviceinfo/sample-svi.csv, which says that Owner will transfer the column 'Content' of serviceinfoIds, 'payload.bin', and 'package.sh', which the device will store in files named by the column 'Content' of serviceinfoids 'payload_name' and 'package_name'. Additionally, the Owner transfers the command as specified in column 'Content' of serviceinfoId 'binsh-linux', to be executed by the Device.