
# About

Docker/Podman Script for building `pri-fidoiot` repository. Using this script you can build the local copy of the repository as well as the latest upstream of the repository.

## Prerequisites

- Operating system: **Ubuntu (20.04, 22.04) / RHEL (8.6, 8.8)**

- Docker engine : (minimum 20.10.10, Supported till version 20.10.21) / Podman engine (For RHEL) 3.4.2+**

- Docker-compose : (minimum version 1.29.2) / Podman-compose 1.0.3(For RHEL)**

***NOTE***: Use the following commands to enable podman build support on RHEL.
```
cd <fdo-pri-src>/build
bash enable_podman_support.sh
echo $'\nexport PODMAN_USERNS=keep-id' >> ~/.bashrc
source ~/.bashrc
```

## Usage
####  Docker Commands

NOTE:  `sudo` can be removed for docker commands to instill the principle of least privilege by adding a user to docker group in Ubuntu [REFER](https://docs.docker.com/engine/install/linux-postinstall/#manage-docker-as-a-non-root-user).

When you want to build a local copy of the repository.

``` sudo docker-compose up --build ```

When you want to build the latest upstream of the repository.

``` sudo use_remote=1 docker-compose up --build ```

#### Podman Commands
When you want to build a local copy of the repository.

``` sudo podman-compose up --build ```

When you want to build the latest upstream of the repository.

``` sudo use_remote=1 podman-compose up --build ```

You also have the option to change the remote repository address as well as the remote repository branch in build.sh file.

    REMOTE_URL=link-to-your-fork
    REMOTE_BRANCH=branch-name
    
## Expected Outcome
As the docker/podman script finishes its execution successfully, the build artifacts would be present in ```<fdo-pri-src>/component-samples/demo/``` folder.

## Updating Proxy Info (Optional)
If you are working behind a proxy network, ensure that both http and https proxy variables are set.

    export http_proxy=http-proxy-host:http-proxy-port
    export https_proxy=https-proxy-host:https-proxy-port
