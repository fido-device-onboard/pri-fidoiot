# Getting the executable

Use following commands to build FIDO IoT Manufacturer Component sample source.
```
$ cd <fido-iot-src>/service/component-samples/manufacturer/
$ mvn clean install
```

This will copy the required executables and libraries into <fido-iot-src>/demo/manufacturer/.

# Starting the Manufacturer service

Open a new terminal window and run the following from :
```
$ cd <fido-iot-src>/demo/manufacturer/
$ docker-compose up --build
```