#!/bin/bash
#
# Copyright 2021 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
# ***WARNING***: The script generates the credentials using default system
# configurations and might not provide necessary security strength for a
# production deployment. Care must be taken to maintain necessary cryptographic
# strength while generating keys for production deployment.

# Summary:
# keys_gen.sh script is implemented to generate necessary keys and credentials
# for different PRI components.

shopt -s extglob
set -e


########
# Help #
########

# Usage message to be displayed whenever we provide wrong inputs
usage()
{
  echo -e "
  WARNING:
  The script generates the credentials using default system configurations and
  might not provide necessary security strength for a production deployment.
  Care must be taken to maintain necessary cryptographic strength while
  generating keys for production deployment.
  Usage:
    $0 [destdir | -h]
      destdir: Credentials are created destdir/creds folder.
               destdir can be either a relative path or an absolute path.
               destdir is created if not present.
               Defaults to present working directory if not present.
      -h: Prints help message.
  Examples:
    $ bash keys_gen.sh
    Creates the credentials folder 'creds' in current director.
    $ bash keys_gen.sh destdir
    Creates the credentials folder 'creds' in 'destdir'.
    $ bash keys_gen.sh -h
    Prints the help message."
}

gen_credentials() 
{

api_user='C=US, ST=OR, L=Hillsboro, O=LF Edge, OU=FDO project, CN=apiUser'
db_user=root
api_pass=default
db_password=`openssl rand -base64 12 | tr -dc 0-9A-Za-z`
encrypt_password=`openssl rand -base64 12 | tr -dc 0-9A-Za-z`
ssl_password=



rm -f service.env && touch service.env

echo "## environment variables" >> service.env
echo "db_user=$db_user" >> service.env
echo "api_password=$api_pass" >> service.env
echo "db_password=db_password.txt" >> service.env
echo "encrypt_password=$encrypt_password" >> service.env
echo "ssl_password=$ssl_password" >> service.env
echo "api_user=$api_user" >> service.env
echo "useSSL=true" >> service.env
echo "requireSSL=true" >> service.env
echo -n `openssl rand -base64 12 | tr -dc 0-9A-Za-z` > db_password.txt

mkdir -p secrets
touch ./secrets/api-user.pem
cp ca-cert.pem ./secrets/ca-cert.pem
cp server-cert.pem ./secrets/server-cert.pem
cp server-key.pem ./secrets/server-key.pem
cp db_password.txt ./secrets/db_password.txt
}

gen_credentials


echo "Key generation completed."
