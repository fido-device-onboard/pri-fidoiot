# Getting the executable

Use following commands to build FIDO IoT Owner Component sample source.
```
$ cd <fido-iot-src>/service/component-samples/owner/
$ mvn clean install
```

This will copy the required executables and libraries into <fido-iot-src>/demo/owner/.

# Starting the owner service

Open a new terminal window and run the following from :
```
$ cd <fido-iot-src>/demo/owner/
$ docker-compose up --build
```