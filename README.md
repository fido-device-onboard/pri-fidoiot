# Secure Device Onboard (SDO)Quick Start

## Building SDO

SDO is written in [Java 11](https://openjdk.java.net/projects/jdk/11/) and uses the 
[Apache Maven* software](http://maven.apache.org).  The instructions which follow describe a simple 
build and assume familiarity with 
[the Maven build lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).

To build SDO, run `mvn package` from the root directory of the SDO source.
The build creates files which will be used in the rest of this guide, including:

`device/target/device-1.8.jar`

An implementation of the SDO Device described in the Secure Device Onboard Protocol 
Specification v1.13b, section 2.3.  Referred to in this document as 'the device', this JAR
can be executed via the `java -jar` command.

`owner/target/owner-1.8.war`

An implementation of the SDO Owner Server described in the Secure Device Onboard Protocol 
Specification v1.13b, section 2.3.  Referred to in this document as 'the owner server', this WAR
can be deployed in a servlet container like [Apache Tomcat](http://tomcat.apache.org/)
or executed via the `java -jar` command.

`rendezvous/target/rendezvous-1.8.war`

An implementation of the SDO Rendezvous Server described in the Secure Device Onboard Protocol 
Specification v1.13b, section 2.3.  Referred to in this document as 'the rendezvous server', this 
WAR can be deployed in a servlet container like [Apache Tomcat](http://tomcat.apache.org/)
or executed via the `java -jar` command.

`to0client/target/to0client-1.8.jar`

An implementation of the SDO Owner Client described in the Secure Device Onboard Protocol 
Specification v1.13b, section 2.3.  Referred to in this document as 'the owner client', this JAR
can be executed via the `java -jar` command.

## Prerequisites

In order to follow the steps in this guide, you must have the SDO Manufacturer Toolkit
installed and active.  See that product's documentation for details.
You will need this information in order to proceed:

- the toolkit's database hostname, user, and password
- the address of the toolkit's web server

This guide describes a toolkit configured with a [MariaDB](https://mariadb.org/) database.
You must have the [MariaDB command-line client](https://mariadb.com/kb/en/library/mysql-client)
installed to follow the steps in this README.

## Configuring SDO

### Preparing Device and Owner Attestation

The device and owner require a key and certificate for identification and attestation (signing).
This software supports the following key types:
- ECDSA [P-384](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf)
- ECDSA [P-256](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf)
- RSA

This guide will use ECDSA P-384 keys.

Generate a key/certificate pair for the device and one for the owner.
To do this with [OpenSSL 1.1.1d](https://www.openssl.org/), type the following:

```
openssl ecparam -name secp384r1 -genkey -noout -out <YOUR KEY FILENAME HERE>
openssl req -new -x509 -key <YOUR KEY FILENAME HERE> -out <YOUR CERTIFICATE FILENAME HERE> -days <YOUR CERTIFICATE VALIDITY PERIOD HERE> -batch
```

Replace the text `<YOUR KEY FILENAME HERE>` with the name of the key file to create.

Replace the text `<YOUR CERTIFICATE FILENAME HERE>` with the name of the certificate file to create.

Replace the text `<YOUR CERTIFICATE VALIDITY PERIOD HERE>` with the certificate's desired validity 
period, in days.

Do this twice: once to create the files for the device, and once to create the files for the owner.
When this task is complete you will have four new files in total.

For example:

```
$ openssl ecparam -name secp384r1 -genkey -noout -out device.key
$ openssl req -new -x509 -key device.key -out device.crt -days 7 -batch
$ openssl ecparam -name secp384r1 -genkey -noout -out owner.key
$ openssl req -new -x509 -key owner.key -out owner.crt -days 7 -batch
$ ls
device.crt  device.key  owner.crt  owner.key
```

The SDO Manufacturer's toolkit needs the owner's public key in order to complete 
device initialization.  Add the owner's public key to the toolkit's database.

Type the following to add the owner's public key to a MariaDB database:

```
mysql --host=<YOUR MANUFACTURER TOOLKIT DATABASE HOST HERE> \
      --user=<YOUR MANUFACTURER TOOLKIT DATABASE USER HERE> \
      --password=<YOUR MANUFACTURER TOOLKIT DATABASE PASSWORD HERE> << EOF
call rt_add_customer_public_key("README owner", "<YOUR OWNER PUBLIC KEY HERE>");
EOF
```

Replace the text `<YOUR MANUFACTURER TOOLKIT DATABASE HOST HERE>` with the hostname or address
of your SDO Manufacturer Toolkit database server.

Replace the text `<YOUR MANUFACTURER TOOLKIT DATABASE USER HERE>` with your Manufacturer
Toolkit database username.

Replace the text `<YOUR MANUFACTURER TOOLKIT DATABASE PASSWORD HERE>` with your Manufacturer
Toolkit database password.

Replace the text `<YOUR OWNER PUBLIC KEY HERE>` with your PEM-encoded owner public key.

For example:

```
mysql --host=sdo.example --user=sdo --password=never_use_this_password << EOF
call rt_add_customer_public_key("README owner", "$(openssl x509 -in owner.crt -pubkey -noout)");
EOF
```

### Preparing Working Directories

The SDO device and owner will not output files unless provided a location to do so.
Create or select a directory to store SDO data files.

For example:

```
mkdir sdo-data
```

### Configuring Java Properties

The SDO source distribution includes the file `application.properties.sample`.  This file
contains detailed information about each of the Java properties which control the SDO
software.  This guide assumes familiarity with that document.

Copy the following into a text file named `application.properties`:

```
org.sdo.device.cert = <YOUR DEVICE CERTIFICATE HERE>
org.sdo.device.key = <YOUR DEVICE KEY HERE>
org.sdo.device.output-dir = <YOUR DEVICE OUTPUT HERE>
org.sdo.di.uri = <YOUR MANUFACTURER URL HERE>
org.sdo.owner.cert = <YOUR OWNER CERTIFICATE HERE>
org.sdo.owner.key = <YOUR OWNER KEY HERE>
org.sdo.owner.output-dir = <YOUR OWNER OUTPUT HERE>
org.sdo.owner.proxy-dir = <YOUR OWNER INPUT HERE>

org.sdo.pkix.revocation-checking-enabled = false
org.sdo.secure-random = SHA1PRNG
org.sdo.to0.ownersign.to1d.bo.port1 = 8042
```

Replace the text `<YOUR DEVICE CERTIFICATE HERE>` with the path of your device certificate file.

Replace the text `<YOUR DEVICE KEY HERE>` with the path of your device key file.

Replace the text `<YOUR DEVICE OUTPUT HERE>` with the path of your working directory.

Replace the text `<YOUR MANUFACTURER URL HERE>` with the address of your SDO Manufacturer
Toolkit web server.

Replace the text `<YOUR OWNER CERTIFICATE HERE>` with the path of your owner certificate file.

Replace the text `<YOUR OWNER KEY HERE>` with the path of your owner key file.

Replace the text `<YOUR OWNER OUTPUT HERE>` with the path of your working directory.

Replace the text `<YOUR OWNER INPUT HERE>` with the path of your working directory.

For example:

```
org.sdo.device.cert = ./device.crt
org.sdo.device.key = ./device.key
org.sdo.device.output-dir = ./sdo-data
org.sdo.di.uri = http://sdo.example:8039/
org.sdo.owner.cert = ./owner.crt
org.sdo.owner.key = ./owner.key
org.sdo.owner.output-dir = ./sdo-data
org.sdo.owner.proxy-dir = ./sdo-data

org.sdo.pkix.revocation-checking-enabled = false
org.sdo.secure-random = SHA1PRNG
org.sdo.to0.ownersign.to1d.bo.port1 = 8042
```

More properties than these are available, but these are enough for basic operation.
Advanced settings and information are available in `application.properties.sample`.

## Running SDO

### Starting the Rendezvous Server

Type the following to start the SDO Rendezvous Server:

```
java -jar <YOUR RENDEZVOUS SERVER WAR HERE>
```

Replace the text `<YOUR RENDEZVOUS SERVER WAR HERE>` with the path of the rendezvous server WAR
you built in a previous step.

For example:

```
$ java -jar rendezvous/target/rendezvous-1.8.war
2020-03-13 11:52:49.981  INFO 12148 --- [           main] org.rendezvous.RendezvousApp   : Starting RendezvousApp
...
2020-03-13 11:52:55.170  INFO 12148 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8040 (http) with context path ''
2020-03-13 11:52:55.180  INFO 12148 --- [           main] org.sdo.rendezvous.RendezvousApp   : Started RendezvousApp in 6.199 seconds (JVM running for 7.1)
```

Record the server port from the log messages for the next step.
In the example above, the server port is 8040.

You can stop the rendezvous server by interrupting it (`Ctrl-C` on most platforms).

The SDO Manufacturer's toolkit needs the rendezvous server's address in order to generate
valid SDO vouchers.  Add the rendezvous server's address to the toolkit's database.

Type the following to add the owner's public key to a MariaDB database:

```
mysql --host=<YOUR MANUFACTURER TOOLKIT DATABASE HOST HERE> \
      --user=<YOUR MANUFACTURER TOOLKIT DATABASE USER HERE> \
      --password=<YOUR MANUFACTURER TOOLKIT DATABASE PASSWORD HERE> << EOF
call mt_add_server_settings("http://<YOUR RENDEZVOUS SERVER HOST>:<YOUR RENDEZVOUS SERVER PORT>", "P1D");
EOF
```

Replace the text `<YOUR MANUFACTURER TOOLKIT DATABASE HOST HERE>` with the hostname or address
of your SDO Manufacturer Toolkit database server.

Replace the text `<YOUR MANUFACTURER TOOLKIT DATABASE USER HERE>` with your Manufacturer
Toolkit database username.

Replace the text `<YOUR MANUFACTURER TOOLKIT DATABASE PASSWORD HERE>` with your Manufacturer
Toolkit database password.

Replace the text `<YOUR RENDEZVOUS SERVER HOST>` with the name of the host on which you are
running the rendezvous server.

Replace the text `<YOUR RENDEZVOUS SERVER PORT>` with the server port you recorded in the previous 
step.

For example:

```
mysql --host=sdo.example --user=sdo --password=never_use_this_password << EOF
call mt_add_server_settings("http://sdo.example:8040", "P1D");
EOF
```

### Starting the Owner Server

Type the following to start the SDO Owner Server:

```
java -jar <YOUR OWNER SERVER WAR HERE>
```

Replace the text `<YOUR OWNER SERVER WAR HERE>` with the path of the owner server WAR
you built in a previous step.

For example:

```
$ java -jar owner/target/owner-1.8.war
Picked up _JAVA_OPTIONS: -Djava.net.preferIPv4Stack=true
2020-03-13 12:08:16.769  INFO 3524 --- [           main] org.sdo.owner.OwnerApp             : Starting OwnerApp
...
2020-03-13 12:08:21.766  INFO 3524 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8042 (http) with context path ''
2020-03-13 12:08:21.772  INFO 3524 --- [           main] org.sdo.owner.OwnerApp             : Started OwnerApp in 5.934 seconds (JVM running for 6.786)
```

You can stop the owner server by interrupting it (`Ctrl-C` on most platforms).

### Initializing the Device

Type the following to initialize the SDO device:

```
java -jar <YOUR DEVICE JAR HERE>
```

Replace the text `<YOUR DEVICE JAR HERE>` with the path of the device JAR
you built in a previous step.

The device will run the Device Initialization (DI) protocol and exit.

For example:

```
$ java -jar device/target/device-1.8.jar
Picked up _JAVA_OPTIONS: -Djava.net.preferIPv4Stack=true
2020-03-13 12:16:10.726  INFO 3032 --- [           main] org.sdo.device.DeviceApp           : Starting DeviceApp
...
2020-03-13 12:16:13.662  INFO 3032 --- [           main] org.sdo.device.DeviceApp           : Started DeviceApp in 3.741 seconds (JVM running for 4.493)
...
2020-03-13 12:16:12.103  INFO 3032 --- [           main] org.sdo.device.DeviceApp           : property org.sdo.device.serial = 12345678
...
2020-03-13 12:16:13.664  INFO 3032 --- [           main] org.sdo.device.DeviceApp           : device initialization begins
...
2020-03-13 12:16:17.371  INFO 3032 --- [           main] c.i.sdo.device.DeviceCredentialsStorage  : credentials saved to ./sdo-data/616d5ba0-d139-426f-9cbf-4997d644268a.oc
2020-03-13 12:16:17.373  INFO 3032 --- [           main] org.sdo.device.DeviceApp           : device initialization ends
```

Record the device's serial number and the path of the stored device credentials from the log.  
You will need this information to onboard the device.  In the example above, the device
serial number is `12345678` and the device credentials are in
`./sdo-data/616d5ba0-d139-426f-9cbf-4997d644268a.oc`.

### Downloading the Ownership Voucher

Once the device has been initialized, the SDO Manufacturer Toolkit can supply our owner's
Ownership Voucher.

To download the voucher using [curl](https://curl.haxx.se/), type the following:

```
curl -G <YOUR MANUFACTURER TOOLKIT WEB SERVER HERE>/api/v1/vouchers/<YOUR DEVICE SERIAL NUMBER HERE> > <YOUR OWNER INPUT HERE>/readme.op
```

Replace the text `<YOUR MANUFACTURER TOOLKIT WEB SERVER HERE>` with the address of SDO Manufacturer Toolkit web server.

Replace the text `<YOUR DEVICE SERIAL NUMBER HERE>` with the serial number from the previous step.

Replace the text `<YOUR OWNER INPUT HERE>` with the path of your working directory.

For example:

```
$ curl -G http://sdo.example/api/v1/vouchers/12345678 > ./sdo-data/readme.op
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  2270  100  2270    0     0   6185      0 --:--:-- --:--:-- --:--:--  6305
```

### Registering the Owner

Before the device can be onboarded, the owner must register its new voucher with the rendezvous
server.  To register the owner's voucher, type:

```
java -jar <YOUR OWNER CLIENT JAR HERE> <YOUR OWNER INPUT HERE>/readme.op
```

Replace the text `<YOUR OWNER CLIENT JAR HERE>` with the path of the owner client jar from
a previous step.

Replace the text `<YOUR OWNER INPUT HERE>` with the path of your working directory.

For example:

```
$ java -jar target/to0client-1.8.jar ./sdo-data/readme.op
Picked up _JAVA_OPTIONS: -Djava.net.preferIPv4Stack=true
2020-03-13 12:16:21.830  INFO 17300 --- [           main] org.sdo.to0client.To0ClientApp     : Starting To0ClientApp
...
2020-03-13 12:16:25.506  INFO 17300 --- [           main] c.i.sdo.OwnerTransferOwnershipClient     : HttpResponse: (POST http://localhost:8040/mp/113/msg/22) 200
{content-length=[11], content-type=[text/plain;charset=UTF-8], date=[Fri, 13 Mar 2020 20:16:24 GMT]}
{"ws":3600}
```

In the log above, the final `OK` shows that the owner's voucher will be held by the rendezvous 
service for one hour, or 3600 seconds.  The Secure Device Onboard Protocol Specification names 
this value 'wait seconds' (ws).

### Onboarding the Device

Once the owner's voucher has been registered, the device can be onboarded.  To onboard the device,
type:

```
java -Dorg.sdo.device.credentials=<YOUR DEVICE CREDENTIALS HERE> -jar <YOUR DEVICE JAR HERE>
```

Replace the text `<YOUR DEVICE CREDENTIALS HERE>` with the path of the device credential file
generated during device initialization.

Replace the text `<YOUR DEVICE JAR HERE>` with the path of the device JAR
you built in a previous step.

For example:

```
$ java -Dorg.sdo.device.credentials=./sdo-data/616d5ba0-d139-426f-9cbf-4997d644268a.oc -jar device/target/device-1.8.jar
2020-03-13 12:16:28.544  INFO 14368 --- [           main] org.sdo.device.DeviceApp           : Starting DeviceApp
...
2020-03-13 12:16:33.996  INFO 14368 --- [           main] org.sdo.device.DeviceApp           : device onboarding ends
```

The device is now onboarded.
