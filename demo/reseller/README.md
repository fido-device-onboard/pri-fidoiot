# Getting the executable

Use following commands to build FIDO IoT Reseller Component sample source.
```
$ cd <fido-iot-src>/service/component-samples/reseller/
$ mvn clean install
```

This will copy the required executables and libraries into \<fido-iot-src\>/demo/reseller/.

# Starting the Reseller service

Refer the [Docker Commands](../README.md/#docker-commands) to start the service.

***NOTE*** The database file located at \<fido-iot-src\>/demo/reseller/target/data/reseller.mv.db is not deleted during 'mvn clean'. As as result the database schema and tables are persisted across docker invocations. Please delete the file manually, in case you encounter any error due to the persisted stale data.

# Inserting keys into Reseller keystore

The PKCS12 keystore file \<fido-iot-src\>/demo/reseller/reseller_keystore.p12 contains the default reseller keys that are imported into the softHSM keystore inside the container, during startup. It contains 3 PrivateKeyEntry with algorithm types: EC-256, EC-384 and RSA-2048, and should continue to hold PrivateKeyEntry with different algorithms. To insert/replace an existing PrivateKeyEntry of any particular algorithm, refer to the section [Inserting Keys into Keystore](../README.md/#inserting-keys-into-keystore) to insert new certificate/private-key pair into \<fido-iot-src\>/demo/reseller/reseller_keystore.p12.