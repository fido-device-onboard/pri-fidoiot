# Table of Contents
1. [System Requirements](#system-requirements)
2. [Docker Commands](#docker-commands)
3. [Start Manufacturer Service](#start-manufacturer-service)
4. [Start Reseller Service](#start-reseller-service)
5. [Start Rendezvous Service](#start-rendezvous-service)
6. [Start Owner Service](#start-owner-service)
7. [Start Device Service](#start-device-service)

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

# Start Manufacturer Service

Refer [Demo Manufacturer README](manufacturer/README.md) to start the service.

# Start Reseller Service

Refer [Demo Reseller README](reseller/README.md) to start the service.

# Start Rendezvous Service

Refer [Demo Rendezvous README](rv/README.md) to start the service.

# Start Owner Service

Refer [Demo Owner README](owner/README.md) to start the service.

# Start Device Service

Refer [Demo Device README](device/README.md) to start the service.