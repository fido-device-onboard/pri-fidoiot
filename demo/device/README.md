# System Requirements:

* **Ubuntu 20.04**.
* **Maven**.
* **Java 11**.
* **Haveged**.

# Getting the executable

Use the following commands to build FIDO IoT HTTP Device Component sample source.
```
$ cd <fido-iot-src>/service/component-samples/http-device-sample/
$ mvn clean install
```

This will copy the required executables and libraries into <fido-iot-src>/demo/device/.

### Configuring the device service

Some software settings are runtime-configurable via Java properties.  They include:

- `fido.iot.randoms`

  A comma-separated list of Java `SecureRandom` algorithm names for random number generation.
  Values are in order from least to most preferred.

  Default is `"NativePRNG,Windows-PRNG"`.

- `fido.iot.url.di`

  The URL at which the Device Initialization (DI) server may be found.

  Default is `http://localhost:8039/`.

- `fido.iot.pem.dev`

  The location of the PEM file containing the device keys (private and public).
  If not set, a hardcoded key is used - see the Java source for details.

  There is no default configured. Provide value `./device.pem` to use the existing default EC-256 key-pair.

# Starting the Device service

```
$ cd <fido-iot-src>/demo/device
$ mvn -Dfido.iot.url.di=<di-server-URL> -Dfido.iot.pem.dev=<device-PEM-file> exec:java
```

The `device-PEM-file` must contain the following PEM-encoded data:
- The device's private key
- The device's public key or certificate.

The device will initialize and exit.  A `credential.bin` file will be created containing the device state.
Removing this file will make the device re-initialize the next time it runs.

The initialization (manufacturer) server must be available during this step.

```
$ mvn -Dfido.iot.pem.dev=<device-PEM-file> exec:java
```

The device will be onboarded.

The rendezvous and owner servers must be available during this step.