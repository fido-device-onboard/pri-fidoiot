# Getting the Executable

Use the following commands to build FIDO Device Onboard (FDO) Protocol Reference Implementation (PRI) HTTP Device Component sample source.
For the instructions in this document, `<fdo-pri-src>` refers to the path of the FDO PRI folder 'pri-fidoiot'.
```
$ cd <fdo-pri-src>/component-samples/http-device-sample/
$ mvn clean install
```

This will copy the required executables and libraries into <fdo-pri-src>/component-samples/demo/device/.

### Configuring the Device Service

Device runtime arguments:

- `fidoalliance.fdo.randoms`

  A comma-separated list of Java `SecureRandom` algorithm names for random number generation.
  Values are in order from least to most preferred.

  Default is `"NativePRNG,Windows-PRNG"`.

- `fidoalliance.fdo.url.di`

  The URL at which the Device Initialization (DI) server may be found.

  Default is `http://localhost:8039/`.

- `fidoalliance.fdo.pem.dev`

  The location of the PEM file containing the device keys (private and public).
  If not set, file `./device.pem` is used by default.

- `fidoalliance.fdo.device.service.info.mtu`

  Maximum MTU Size for ServiceInfo that owner can send to the device.
  If not set, default MTU size of 1300 bytes will be used for ServiceInfo transfers to device.

- `fidoalliance.fdo.device.cred.reuse`

  Property to enable or disable support for device credential reuse.

  Default is true.

# Starting the Device Service

```
$ cd <fdo-pri-src>/component-samples/demo/device
$ java -Dfidoalliance.fdo.url.di=<di-server-URL> -Dfidoalliance.fdo.pem.dev=<device-PEM-file> -jar device.jar
```

The `device-PEM-file` must contain the following PEM-encoded data:
- The device's private key
- The device's public key or certificate.

The device will initialize and exit.  A `credential.bin` file will be created containing the device state.
Removing this file will make the device re-initialize the next time it runs.

The initialization (manufacturer) server must be available during this step.

```
$ java -Dfidoalliance.fdo.pem.dev=<device-PEM-file> -jar device.jar
```

The device will be onboarded.

The rendezvous and owner servers must be available during this step.

# Configuring Device for HTTPS/TLS Communication

- Copy the truststore containing all the required certificates to `demo/device` folder.

- You can execute the device in two modes:

  * `TEST` mode where certificate verification is skipped. Useful for https development or testing.
```
    java -D<other-flags> -Dfido_ssl_mode=TEST -jar device.jar
  ```
Make sure to add the `-Dfido_ssl_mode=TEST`.

  * `PROD` mode where certificate verification is carried out. Useful for production deployment.
  ```
    java -D<other-flags> -Dfido_ssl_mode=PROD -Dssl_trustore=<trust-store-path> -Dssl_truststore_password=<truststore-pass> -Dssl_truststore_type=<truststore-type> -jar device.jar
  ```

  Make sure to add all required truststore flags if the ssl_mode is `PROD`. Default ssl_truststore_type is PKCS12.
