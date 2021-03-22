# Table of Contents
1. [System Requirements](#system-requirements)
2. [Docker Commands](#docker-commands)
3. [Running Demo](#running-demo)
4. [Running Demo using Reseller](#running-demo-using-reseller)
5. [ServiceInfo Setup](#serviceinfo-setup-between-fido-iot-http-java-device-sample-and-fido-iot-owner-sample)
6. [Enabling ServiceInfo transfer](#enabling-serviceinfo-transfer)
7. [Working with Keystore](#working-with-keystore)

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

# Configuring OnDie (optional)

OnDie is a type of device that makes use of the MAROE prefix. If you need to support such devices then you will need to configure the PRI demo components by adding/updating the following properties. The values can be specified via java -Doptions, entries in application.properties or entries the the .env files.

OnDie requires several certs and CRLs. These artifacts can be downloaded from the cloud with the script provided in the component-samples/scripts directory (onDieCache.py). They can also be downloaded direct from java if the ondie_autoupdate property is set to true.

`ondie_cache`: (required if supporting OnDie, optional otherwise) specifies the path to the directory containing the OnDie cert and CRLs.

`ondie_autoupdate`: (optional, default = false) if "true" then the OnDie certs and CRLs are downloaded from the cloud at start up into the directory specified by ondie_cache.
Note that this requires internet access by the component. Should a component be run in on-prem mode then this setting should be set to "false". In such cases, the artifacts can be preloaded by running the script in component-sample/scripts/onDieScript.py when access is available or from another machine with access and then copied into the ondie_cache directory.

`ondie_zip_artifact`: (optional, default = https://tsci.intel.com/content/csme.zip). Specifies the URL containing of the zip file that contains the OnDie certs and CRLs.

`ondie_check_revocations`: (optional, default = true for Manufacturer and Owner, false for RV) if "true" then revocations are checked by the component, no revocation checking is done if "false".

# Running Demo

1. Start the FIDO IoT Manufacturer Sample as per the steps outlined in [Manufacturer README](manufacturer/README.md).

2. Complete Device Initialization (DI) by starting the FIDO IoT HTTP Java Device Sample as per the steps outlined in [Device README](device/README.md). Delete any existing 'credential.bin' before starting the device.

3. Complete Ownership Voucher Extension by using the API `GET /api/v1/vouchers/<serial_no>` and save the Ownership Voucher. By default, existing customer with customer Id '1', is assigned to the device. To add a new customer and assign the inserted customer to the device, please refer to [FIDO IoT Manufacturer REST APIs](manufacturer/README.md/#fido-iot-manufacturer-rest-apis) for more information about the API.

4. Start the FIDO IoT RV Sample as per the steps outlined in [RV README](rv/README.md).

5. Start the FIDO IoT Owner Sample as per the steps outlined in [Owner README](owner/README.md). Import the extended Ownership Voucher from Step#3 into the Owner database by using the API `POST /api/v1/owner/vouchers/`. Please refer to [FIDO IoT Owner REST APIs](owner/README.md/#fido-iot-owner-rest-apis) for more information about the API. Optionally, if ServiceInfo transfer is needed, please refer to [Enabling ServiceInfo transfer](#enabling-serviceinfo-transfer).

6. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FIDO IoT HTTP Java Device Sample again. The previously created 'credential.bin' from Step#2 will be used directly by the Device.

**NOTE** Credential Reuse protocol is enabled by default, that is, after a successful onboarding the device credentials remain unchanged. To enable the Resale protocol instead, wherein, after a successful onboarding the device credentials are replaced, update the fields `REPLACEMENT_GUID` and/or `REPLACEMENT_RVINFO` in Owner `TO2_DEVICES` table by using the API `POST /api/v1/owner/setupinfo?id=<device_guid>` before starting TO2. Please refer to [FIDO IoT Owner REST APIs](owner/README.md/#fido-iot-owner-rest-apis) for more information about the API.

# Running Demo using Reseller

The FIDO IoT Reseller Sample can be used in different ways depending on its positioning in the Supply-Chain.
In the following use-case (use-case: 1), the FIDO IoT Manufacturer provisions the Device using DI and sells the Device to FIDO IoT Reseller, that later sells the Device to the FIDO IoT Owner, that completes the device onboarding (TO0 and TO2):

1. Start the FIDO IoT Manufacturer Sample as per the steps outlined in [Manufacturer README](manufacturer/README.md).

2. Complete Device Initialization (DI) by starting the FIDO IoT HTTP Java Device Sample as per the steps outlined in [Device README](device/README.md). Delete any existing 'credential.bin' before starting the device.

3. Complete Ownership Voucher Extension by using the API `GET /api/v1/vouchers/<serial_no>` and save the Ownership Voucher. Assign customer with customer Id '2' to the device. Please refer to [FIDO IoT Manufacturer REST APIs](manufacturer/README.md/#fido-iot-manufacturer-rest-apis) for more information about assigning customer to a device.

4. Start the FIDO IoT Reseller Sample as per the steps outlined in [Reseller README](reseller/README.md).

5. Add the extended Ownership Voucher to the reseller database table `RT_DEVICES` using reseller API `POST /api/v1/resell/vouchers/<serial_number>`.

6. If the reseller key to be used for Ownership Voucher extension is not present in `RT_CUSTOMER` table, add it using reseller API `POST /api/v1/resell/keys/?alias=<keystore_alias>`. By default, the next Owner keys are present in the table `RT_CUSTOMERS`, that are same as the keys present in Owner's keystore.

7. Extend the Ownership Voucher using the reseller API `GET /api/v1/resell/vouchers/<serial_number>?id=<customer_id>` to the next owner.

8. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FIDO IoT HTTP Java Device Sample.

In an another use-case (use-case: 2), the FIDO IoT Manufacturer provisions the Device using DI and sells the Device to the FIDO IoT Owner, that completes the device onboarding (TO0 and TO2) by triggering the Resale protocol. The FIDO IoT Owner then uses the FIDO IoT Reseller to sell the Device to the next Owner, that again, completes the device onboarding (TO0 and TO2):

1. Ensure that the Device is initialized and onboarded as per the instructions listed in [Running Demo](#running-demo) section by enabling the 'Resale' protocol.

2. Get the new Ownership Voucher using the Owner API `GET /api/v1/owner/newvoucher/?id=<device_guid>`.

3. Start the FIDO IoT Reseller Sample as per the steps outlined in [Reseller README](reseller/README.md). Prior to the startup, configure the FIDO IoT Reseller to use the FIDO IoT Owner's keystore. This can be done by copying the \<fido-iot-src\>/demo/owner/owner_keystore.p12 into \<fido-iot-src\>/demo/reseller/ and renaming it to reseller_keystore.p12. Additionally,  update the property 'reseller_keystore_password' in \<fido-iot-src\>/demo/reseller/reseller.env, with value of 'owner_keystore_password' in \<fido-iot-src\>/demo/owner/owner.env.

4. Add the new size 0 Ownership Voucher to the reseller database table `RT_DEVICES` using reseller API `POST /api/v1/resell/vouchers/<serial_number>`

5. Add a new set of Owner PEM-formatted public keys in `RT_CUSTOMER` table using reseller API `POST /api/v1/resell/keys/?alias=<keystore_alias>`. Refer to [Generating Key-Pair](#generating-key-pair) for generating PEM-formatted key-pairs.

6. Extend the Ownership Voucher using the reseller API `GET /api/v1/resell/vouchers/<serial_number>?id=<customer_id>`.

7. Start the FIDO IoT Owner Sample as per the steps outlined in [Owner README](owner/README.md). Prior to the startup, if an instance of FIDO IoT Owner is already running on the same port, terminate the previous instance. Configure the FIDO IoT Owner to use the new Owner's key-pairs. This can be done by deleting any existing 'alias' from the \<fido-iot-src\>/demo/owner/owner_keystore.p12, and inserting the key-pairs created in Step-6. Refer to [Inserting Keys into Keystore](#inserting-keys-into-keystore) and [Removing an Existing Key-Pair from Keystore](#removing-an-existing-key-pair-from-keystore). Additionally,  update the properties 'owner_keystore' and 'owner_keystore_password' in \<fido-iot-src\>/demo/owner/owner.env, with the keystore file-system path and password, respectively.

8. Complete Transfer Ownership 1 and 2 (TO1 and TO2) by starting the FIDO IoT HTTP Java Device Sample.

In a special case of use-case: 2, the owner generates the size 0 ownership voucher using a different public key other than the one used for onboarding (say Owner2 key). In this particular implementation, Owner2 key is same as the keys used in Reseller instance. In such a scenario, resale can be performed using the same steps as listed above, except step 5. User doesn't need to perform step 5 as ownership will already be with the reseller in which case copying owner's key in reseller keystore will not be required. Use the setupinfo API listed in [Owner README](owner/README.md#fido-iot-owner-rest-apis) for the same.

# ServiceInfo setup between FIDO IoT HTTP Java Device Sample and FIDO IoT Owner Sample

The FIDO IoT HTTP Java Device Sample currently supports `sdo_sys` module for interpreting received owner ServiceInfo and `devmod` module to share device ServiceInfo with Owner.

- `sdo_sys` Owner ServiceInfo module: This module supports the following 3 message names as listed below to interpret the ServiceInfo as received from the Owner. The basic functionality of this module is to support payload/script transfers and basic command execution.  A sample format looks like 'sdo_sys:filedesc=filename, sdo_sys:write=filecontent,sdo_sys:exec=command-to-execute'.

    *filedesc* - The name to be given to the file once it is transferred. Upon receiving this, device creates a file with the given name and opens stream to write into it.

    *write* - The payload/content (script, binaries, and others) that is sent to the device. Upon receiving this, device writes the content into the open stream as given by the preceeding 'filedesc' message.

    *exec* - The command that will be executed at the device. Device executes the command as received.

***NOTE*** The comma-separated values must be ordered such that the 'filedesc' and 'write' objects are one after the other pair-wise, followed by the 'exec' commands.

- `devmod` Device ServiceInfo module: This module supports multiple messages as listed down in the protocol specification, that are sent to the Owner as Device ServiceInfo. A sample format looks like 'devmod:active=1'.

The FIDO IoT Owner Sample currently supports the same `sdo_sys` module to send Owner ServiceInfo to the Device and `devmod` module to store the received Device ServiceInfo. Few sample ServiceInfo values, as present in \<fido-iot-src\>/demo/owner/serviceinfo/sample-values/ are populated into the database table 'OWNER_SERVICEINFO' as byte arrays. For keeping the association between the Device and the ServiceInfo values to transfer, 'GUID_OWNERSVI' database table is used. When a Device is inserted into the database table 'TO2_DEVICES', it'll not have any association with the ServiceInfo values, and so by default, no ServiceInfo is transferred to the Device.

# Enabling ServiceInfo transfer

To enable ServiceInfo transfer to a Device with a given GUID, follow the steps below:

1. (Optional) Insert required ServiceInfo values into the database table 'OWNER_SERVICEINFO' using the API `POST /api/v1/owner/svivalues/?id=<serviceinfo_id>&isCborEncoded=<boolean_value>`. More information about the same is provided in section [FIDO IoT Owner REST APIs](owner/README.md/#fido-iot-owner-rest-apis). If the required ServiceInfo already exists in the table, go on to the next step.

***NOTE*** The current implementation at FIDO IoT Owner only supports the value 'false' for the query parameter 'isCborEncoded', and the value 'true' should not be used. The implementation will be updated in the future to support both values.

2. (Mandatory) Insert required association between the Device and ServiceInfo values to transfer using the API `POST /api/v1/owner/svi/?guid=<guid>`. More information about the same is provided in section [FIDO IoT Owner REST APIs](owner/README.md/#fido-iot-owner-rest-apis). As a reference, please see \<fido-iot-src\>/demo/owner/serviceinfo/sample-svi.csv. As per the sample, that Owner will transfer the value of column 'CONTENT' corresponding to the specified 'SVI_ID': 'payload.bin' and 'package.sh'. The device will store/save the received data in files named by the column 'CONTENT' corresponding to the specified 'SVI_ID': 'payload_name' and 'package_name'. Additionally, the Owner transfers the command as specified in column 'CONTENT' corresponding to the specified values of column 'SVI_ID', i.e 'binsh-linux', to be executed by the Device.

# Generating Key-Pair

## Generating ECDSA Key-Pair

FIDO IoT spec supports the National Institute of Standards and Technology (NIST) P-256 curve and P-384 types.

**Step 1:** Generate the private key.

Generate the NIST-256 key by running the following command:

`$ openssl ecparam -genkey -name secp256r1 -out eckey.pem`

Alternatively, generate the NIST-384 key by running the following command:

`$ openssl ecparam -genkey -name secp384r1 -out eckey.pem`

**Step 2:** Generate a self-signed certificate.

Generate the self-signed certificate for the previously generated NIST-256 key as:

`$ openssl req -x509 -sha256 -nodes -days 3650 -key eckey.pem -out eccert.crt`

Alternatively, generate the self-signed certificate for the previously generated NIST-384 key as:

`$ openssl req -x509 -sha384 -nodes -days 3650 -key eckey.pem -out eccert.crt`

**Step 3:** Convert the key to public key cryptography standards (PKCS\#8) format (optional):

`$ openssl pkcs8 -topk8 -nocrypt -in eckey.pem -out eckey.key`

**Step 4:** Create a certificate signing request to send for generating a certificate chain (optional):

`$ openssl x509 -x509toreq -in eccert.crt -out CSR.csr -signkey eckey.key`

## Generating RSA Key-Pair

FIDO IoT spec supports the RSA2048

**Step 1:** Generate the private key.

Generate the NIST-256 key by running the following command:

`$ openssl genrsa -out rsakey.pem 2048`

**Step 2:** Generate a self-signed certificate.

Generate the self-signed certificate for the previously generated NIST-256 key as:

`$ openssl req -x509 -key rsakey.pem -days 365 -out rsacert.pem`

# Working with Keystore

## Inserting Keys into Keystore

Assuming that there is already an existing certificate named 'certificate.pem' and the corresponding private key 'private-key.pem', follow these steps to insert them as 'PrivateKeyEntry' into the keystore 'dest-keystore.p12':

**Step 1:** Convert the certificate and private key into 'PKCS12' format:

`$ openssl pkcs12 -export -in certificate.pem -inkey private-key.pem -name newkeypair -out src-keystore.p12`

**Step 2:** Delete an existing alias (only needed if there is a need to replace PrivateKeyEntry having a particular algorithm):

`$ keytool -delete -alias newkeypair -keystore keystore.jks`

**Step 3:** Import the above generated source PKCS12 file into the existing destination keystore file 'dest-keystore.p12' located with alias 'newkeypair'.

`$ keytool -importkeystore -destkeystore path/to/dest-keystore.p12 -srckeystore src-keystore.p12 -srcstoretype PKCS12 -alias newkeypair`

***NOTE*** The password entered in Step 1 to generate the src-keystore.p12 must be the same as that of dest-keystore.p12, that is, the password of the newly created keystore must match the existing keystore where it will be imported to.

## Exporting an Existing Certificate from Keystore

Assuming that there is an existing certificate and private key stored in the keystore as a PrivateKeyEntry under the alias 'newkeypair', run the following command to extract the certificate into <ertificate.pem\>:

**Step 1:** Get the list of key-pairs, along with their respective aliases, from the keystore:

`$ keytool -list -v -keystore /path/to/dest-keystore.p12`

**Step 2:** Export the certificate from the keystore using any of the aliases present in the keystore**:**

`$ keytool -exportcert -alias newkeypair -file
<certificate.pem> -rfc -keystore path/to/dest-keystore.p12`

## Removing an Existing Key-Pair from Keystore

Assuming that there is an existing owner's certificate and private key stored in the keystore as a PrivateKeyEntry under the alias 'newkeypair', run the following command to remove the key-pair corresponding to the alias:

`$ keytool -delete -alias newkeypair -keystore path/to/dest-keystore.p12`