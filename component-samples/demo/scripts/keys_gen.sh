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

db_user=sa
api_pass=`openssl rand --base64 12 | tr -dc 0-9A-Za-z`
db_password=`openssl rand --base64 12 | tr -dc 0-9A-Za-z`
encrypted_password=`openssl rand --base64 12 | tr -dc 0-9A-Za-z`
ssl_password=`openssl rand --base64 12 | tr -dc 0-9A-Za-z`

cd $CREDS_PATH
mkdir -p $1 && cd $1

rm service.env && touch service.env

echo "## environment variables" >> service.env
echo "db_user=$db_user" >> service.env
echo "api_password=$api_pass" >> service.env
echo "db_password=$db_password" >> service.env
echo "encrypted_password=$encrypted_password" >> service.env
echo "ssl_password=$ssl_password" >> service.env

}

# Environmental variables
if [ $# -gt 1 ]; then
  usage
  exit
fi

COMP_PATH=`pwd`
if [ $# -eq 1 ]; then
  if [ $1 == "-h" ]; then
    usage
    exit
  fi
  COMP_PATH=$(realpath $1)
fi
CREDS_PATH=$COMP_PATH/creds

mkdir -p $CREDS_PATH

components=("manufacturer" "rv" "owner" "aio" "reseller")
for i in ${components[@]}; do
  if [[ "${components[@]}" =~ "$i" ]]; then
    gen_credentials $i
  fi
done

echo "Key generation completed."