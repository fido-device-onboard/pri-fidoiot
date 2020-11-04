# Getting the executable

Use following commands to build FIDO IoT RV Component sample source.
```
$ cd <fido-iot-src>/service/component-samples/rv/
$ mvn clean install
```

This will copy the required executables and libraries into <fido-iot-src>/demo/rv/.

# Starting the rv service

Open a new terminal window and run the following from :
```
$ cd <fido-iot-src>/demo/rv/
$ docker-compose up --build
```