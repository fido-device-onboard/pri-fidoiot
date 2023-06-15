# About

The FDO DB is designed to run database service in a containerized **docker** environment. Currently, MariaDB* is used as the default database image.

***NOTE***: The FDO DB is provided to support out-of-the-box operation of FDO components.  Appropriate security measures with respect to key-store management and credential management should be considered while performing production deployment of any FDO component.

# Getting Started with the FDO DB

The following are the system requirements for FDO DB.
- Host Operating System: Ubuntu (20.04, 22.04) / RHEL (8.4, 8.6) / Debian 11.4
- Docker 20.10.10+ / Podman engine 3.4.2+ (For RHEL)
- Docker compose 1.29.2 / Podman-compose 1.0.3(For RHEL)

# Running FDO DB

The FDO DB can be executed as a docker service only. 


***NOTE***: If MariaDB's `latest` or `lts` tag is pulling 11.* version. Make sure to remove the deprecated configuration variables in `custom/config-file.cnf` [Check for deprecated variables](https://mariadb.com/kb/en/server-system-variables/#list-of-server-system-variables).

Generate the required credentials keypair and certificates using the 'keys_gen.sh' script present  in `<fdo-pri-src>/component-samples/demo/scripts/`.
[Learn more about key generation](https://github.com/fido-device-onboard/pri-fidoiot#generating-random-passwords-using-keys_gensh)

At the end of initialization of DB service, you will see following statement on the console. 

```
fdo-db_1  | 2022-09-29 20:21:12 0 [Note] mariadbd: ready for connections.
fdo-db_1  | Version: '10.9.3-MariaDB-1:10.9.3+maria~ubu2204-log'  socket: '/tmp/mysql.sock'  port: 3306  mariadb.org binary distribution
```

Follow the below steps to start FDO DB.

Make sure to update the credential environment variables set in `service.env` file.

##  Run as Docker Service

Open a terminal, change directory to `<fdo-pri-src>/component-samples/demo/db/` and execute following command.

```
docker-compose up --build
```

In case you need super user access, prefix 'sudo -E' to above command.


# Troubleshooting

If passwords of database is updated. It's advised to clear `<fdo-pri-src>/component-samples/demo/db/app-data` after stopping the service and restart the DB service again.

