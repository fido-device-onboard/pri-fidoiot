#!/bin/bash
#
# Copyright 2021 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
# ***WARNING***: The script generates the credentials using default system
# configurations and might not provide necessary security strength for a
# production deployment. Care must be taken to maintain necessary cryptographic
# strength while generating keys for production deployment.
#
# Summary:
# keys_gen.sh script is implemented to generate necessary keys and credentials
# for different PRI components.
#
# These include following artifacts.
# - Keystore files for AIO, Manuacturer, Reseller, Owner components
# - TLS credentials (SSL keystore and truststore)
# - Customer Public-key files AIO, Manufacturer, Reseller, Owner components
# - Public-key hash values (for RV allow-list configuration)
# - Password for keystore, truststore and REST APIs updated in respective
# creds.env file
#
# Following tests have been performed with the generated credentials.
# - Sanity end-to-end(E2E) with HTTP
# - Sanity E2E with HTTPS
# - Resale scenario with Owner / Owner2 keys
# - E2E with Size-2 voucher (Manufacturer -> Reseller -> Owner)
# - Multi-machine E2E with HTTPS

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

#######################################
# Helper function for env file update #
#######################################

# Update the component creds.env file with the new credentials
pass_update_env_files()
{
  # Arg1: Path of Component
  comp=$1

  # Arg2: Environment statement that needs to be added
  statement=$2

  mkdir -p ${comp}
  echo ${statement} >> ${comp}/creds.env
}

#######################################
# Helper functions for key generation #
#######################################

# Generate hash of public key (takes certificate as input)
# Certificate -> Encrypted .der file -> Hash
# Redirect hashes to the corresponding output file
generate_hash()
{
  # Arg1: Certificate file
  certfile=$1

  # Arg2: Output hash file
  hashfile=$2

  # Generate public key hash
  openssl x509 -in $certfile -pubkey -noout | openssl enc -base64 -d > public.der
  cat public.der | openssl dgst -sha256 | awk '/s/{print toupper($2)}' > $hashfile

  rm -f public.der
}

# Generate RSA keypair
generate_keypair_rsa()
{
  # Arg1: Curve name
  keylen=$1

  # Remove temporary files form last call
  rm -f private.key public.key cert.pem

  # Generate RSA2048 Keys and include them in the keystore
  openssl genrsa -F4 -out private.key ${keylen} > /dev/null 2>&1

  openssl rsa -in private.key -outform PEM -pubout \
    -out public.key > /dev/null 2>&1

  openssl req -x509 -key private.key -out cert.pem -days 3650 -batch

  # Generate public key hash
  generate_hash cert.pem rsa${keylen}_public.hash

  cp public.key rsa${keylen}_public.key
}

# Generate RSA keystore
generate_keystore_rsa()
{
  # Arg1: Curve name
  keylen=$1

  # Arg2: Keystore password
  pass=$2

  # Arg3: Keystore name prefix
  name=$3

  rm -f rsa${keylen}.p12

  generate_keypair_rsa $keylen

  openssl pkcs12 -export -in cert.pem -inkey private.key -name ${name}_rsa${keylen} \
    -out rsa${keylen}.p12 -password pass:${pass}
}

# Generate EC keypair
generate_keypair_ec()
{
  # Arg1: Curve name
  keylen=$1

  if [ $keylen == "256" ]; then
    curve=prime256v1
  else
    curve=secp384r1
  fi

  # Remove temporary files form last call
  rm -f private.key public.key cert.pem

  # Keys Generations
  openssl ecparam -name $curve -genkey -noout -out private.key > /dev/null 2>&1

  openssl ec -in private.key -pubout -out public.key > /dev/null 2>&1

  openssl req -x509 -key private.key -out cert.pem -days 3650 -batch

  # Generate public key hash
  generate_hash cert.pem ec${keylen}_public.hash

  cp public.key ec${keylen}_public.key
}

# Generate EC keystore
generate_keystore_ec()
{
  # Arg1: Curve name
  keylen=$1

  # Arg2: Keystore password
  pass=$2

  # Arg3: Keystore name prefix
  name=$3

  rm -f ec${keylen}.p12

  generate_keypair_ec $keylen

  openssl pkcs12 -export -in cert.pem -inkey private.key -name ${name}_ec${keylen} \
    -out ec${keylen}.p12 -password pass:${pass}
}

# Import one keystore into another keystore
import_keystore()
{
  # Arg1: Destination keystore
  dest=$1

  # Arg3: Destination Keystore password
  destpass=$2

  # Arg3: Source keystore
  src=$3

  # Arg4: Source Keystore password
  srcpass=$4

  keytool -importkeystore -srckeystore ${src} -srcstorepass ${srcpass} \
    -destkeystore ${dest} -deststorepass ${destpass} \
    -deststoretype pkcs12 -noprompt > /dev/null 2>&1
}

#####################################
# Component specific key generation #
#####################################

# Generation of keystore with RSA2048, EC256 and EC384 keys as shown below
# ---------------------------------------------------------------------------
# Private key    -> Public Key    -> Certificate -> P12 Key   -> P12 Keystore
# ---------------------------------------------------------------------------
# rsa_priv_key   -> rsa_pub_key   -> rsa_cert    -> rsa.p12   -> keystore.p12
# ec256_priv_key -> ec256_pub_key -> ec256_cert  -> ec256.p12 -> keystore.p12
# ec384_priv_key -> ec384_pub_key -> ec384_cert  -> ec384.p12 -> keystore.p12
# ---------------------------------------------------------------------------
# Merging the above generated public keys into one file with respective to single component
generate_keystore()
{
  comp=${1}

  PASS_KEY=`openssl rand --base64 12 | tr -dc 0-9A-Za-z`

  rm -f keystore.p12 ec256.p12 ec384.p12 rsa2048.p12
  rm -f pub_keys.pem

  # RSA2048 Keys Generations
  generate_keystore_ec 256 ${PASS_KEY} ${comp}
  generate_keystore_ec 384 ${PASS_KEY} ${comp}
  generate_keystore_rsa 2048 ${PASS_KEY} ${comp}

  # Import rsa2048, ec256, and ec384 p12 keystores to one p12 keystore
  import_keystore keystore.p12 ${PASS_KEY} ec256.p12 ${PASS_KEY}
  import_keystore keystore.p12 ${PASS_KEY} ec384.p12 ${PASS_KEY}
  import_keystore keystore.p12 ${PASS_KEY} rsa2048.p12 ${PASS_KEY}

  # Verify the keys from p12 keystore
  keytool -list -keystore keystore.p12 -storepass ${PASS_KEY} > /dev/null 2>&1

  # Creating a keys.pem files
  cat ec256_public.key | tee pub_keys.pem > /dev/null
  cat ec384_public.key | tee -a pub_keys.pem > /dev/null
  cat rsa2048_public.key | tee -a pub_keys.pem > /dev/null

  cat ec256_public.hash | tee pub_keys.hash > /dev/null
  cat ec384_public.hash | tee -a pub_keys.hash > /dev/null
  cat rsa2048_public.hash | tee -a pub_keys.hash > /dev/null

  # Preserve necessary artifacts
  mv keystore.p12 ${1}_keystore.p12
  mv pub_keys.pem ${1}_pub_keys.pem
  mv pub_keys.hash ${1}_pub_keys.hash

  # Delete temporary files
  rm -f ec256.p12 ec384.p12 rsa2048.p12
  rm -f ec256_public.key ec384_public.key rsa2048_public.key
  rm -f ec256_public.hash ec384_public.hash rsa2048_public.hash

  # Redirecting password into one file
  echo "${PASS_KEY}" > ${1}_keystore.pass
}

# Generate component specific keystores and update the password in creds.env
# file
generate_component_keys()
{
  # Cleaning all component keys that only exists in order to regenerate fresh keys
  if [[ -f ${1}_keystore.p12 ]]; then
    rm -rf ${1}*
  fi

  printf "%.40s" "Generating ${1}_keystore.p12 ..................."

  # Generating keystores and hashes of the component
  generate_keystore $1

  # Copying the keystores to the destination paths
  mkdir -p $CREDS_PATH/${1}
  cp ${1}_keystore.p12 $CREDS_PATH/${1}/${1}_keystore.p12

  # Updating the password in the component environment file
  pass_update_env_files $CREDS_PATH/${1} "${1}_keystore_password=${PASS_KEY}"

  # Logging the path of component hashes and it password
  printf " successful\n"
}

# Generation of TLS keystore and truststore
generate_tls_keystore()
{
  # Cleaning all existing component keys to regenerate fresh keys
  rm -f ssl.p12 ssl.pass truststore

  printf "%.40s" "Generating ssl.p12 and truststore ..................."

  # Configuring list of IPs
  # Get IP of current machine
  if=$(ip route | grep default | sed -e "s/^.*dev.//" -e "s/.proto.*//")
  ip=$(ip addr | grep inet | grep " $if" | cut -d' ' -f6 | cut -d'/' -f1)

  # The SSL certificate would include all these DNS/IP in the subjectAlName field
  # Apppend additional DNSs at the end within double-quotes
  # Example: dns_list=("rv1.fdo" "rv2.fdo")
  # Apppend additional IPs at the end within double-quotes
  # Example: ip_list=("${ip}" "127.0.0.1" "192.168.0.1)
  subjectAltName="DNS:localhost"
  dns_list=()
  ip_list=("${ip}" "127.0.0.1")
  for i in ${dns_list[@]}; do
    subjectAltName+=",DNS:${i}"
  done
  for i in ${ip_list[@]}; do
    subjectAltName+=",IP:${i}"
  done

  # TLS keystore and truststore creation steps
  openssl req \
    -x509 \
    -newkey rsa:2048 \
    -sha256 \
    -days 3560 \
    -nodes \
    -keyout tls.key \
    -out tls.crt \
    -subj '/CN=fdo' \
    -extensions san \
    -config <( \
    echo '[req]'; \
    echo 'distinguished_name=req'; \
    echo '[san]'; \
    echo 'subjectAltName='${subjectAltName}) > /dev/null 2>&1

  export SSL_PASS=`openssl rand --base64 8 | tr -dc 0-9A-Za-z`
  openssl pkcs12 -export -in tls.crt -inkey tls.key -out ssl.p12 -password pass:${SSL_PASS}
  keytool -import -alias fdo -file tls.crt -storetype PKCS12 -keystore truststore \
    -storepass ${SSL_PASS} -noprompt > /dev/null 2>&1
  echo "${SSL_PASS}" > ssl.pass

  # Copying the ssl keystore and truststores to the destination paths
  for comp in "manufacturer" "rv" "owner" "reseller" "aio"; do
    mkdir -p $CREDS_PATH/$comp/certs/
    cp ssl.p12 $CREDS_PATH/$comp/certs/
    pass_update_env_files $CREDS_PATH/$comp "${comp}_ssl_keystore-password=${SSL_PASS}"
  done

  for comp in "owner" "aio"; do
    mkdir -p $CREDS_PATH/$comp/certs
    cp truststore $CREDS_PATH/$comp/certs/
    pass_update_env_files $CREDS_PATH/$comp "ssl_truststore_password=${SSL_PASS}"
  done

  printf " successful\n"

  rm -rf tls.???
}

# Generating new device keys and updating it into the device.pem file
device_pem_files()
{
  mkdir -p $CREDS_PATH/device

  printf "%.40s" "Generating device keys ..................."
  generate_keypair_ec 256
  cat cert.pem > $CREDS_PATH/device/device_ec256.pem
  cat private.key >> $CREDS_PATH/device/device_ec256.pem

  generate_keypair_ec 384
  cat cert.pem > $CREDS_PATH/device/device_ec384.pem
  cat private.key >> $CREDS_PATH/device/device_ec384.pem
  printf " successful\n"

  cp $CREDS_PATH/device/device_ec256.pem $CREDS_PATH/device/device.pem
}

#####################################
# Component specific configurations #
#####################################

# Generate password for REST API authentication
generate_api_pass()
{
  printf "%.40s" "Generating API password .........................."
  api_pass=`openssl rand --base64 12 | tr -dc 0-9A-Za-z`

  # Update API password
  for comp in "aio" "manufacturer" "owner" "reseller"; do
    pass_update_env_files $CREDS_PATH/$comp "${comp}_api_password=${api_pass}"
  done

  printf " successful\n"
}

# Updating all component key hashes to the RV config.properties
prepare_rv_config()
{
  printf "%.40s" "Generating rv/config.properties ..................."
  # Capturing all component keystore hashes into one environmental variable HASHES
  for i in *.hash; do
    if [[ $i =~ ".hash" ]]; then
      sed -n '1,3'p $i | tee -a all_hashes.txt > /dev/null
    fi
  done
  sed -i "1,$((`cat all_hashes.txt | wc -l`-1))s/$/,\\\\/g" all_hashes.txt
  HASHES=`cat all_hashes.txt`; rm -rf all_hashes.txt;

  # Updating the RV config.properties file with the new hashes of 3 components (mfg, own, & reseller)
  mkdir -p $CREDS_PATH/rv
  printf "guid.denylist=\n\n" > $CREDS_PATH/rv/config.properties
  printf "allowlist.publickeyhash=\n\n" >> $CREDS_PATH/rv/config.properties
  printf "denylist.publickeyhash=\n" >> $CREDS_PATH/rv/config.properties

  ALLOW_LINE=`grep -rn "allowlist.publickeyhash" $CREDS_PATH/rv/config.properties | cut -d ':' -f 1`
  DENY_LINE=`grep -rn "denylist.publickeyhash" $CREDS_PATH/rv/config.properties | cut -d ':' -f 1`
  sed -i "s/allowlist.publickeyhash\=.*/allowlist.publickeyhash\=\\\\/g" $CREDS_PATH/rv/config.properties
  sed -i "$((ALLOW_LINE+1)),$((DENY_LINE-1))d" $CREDS_PATH/rv/config.properties
  sed -i "$((ALLOW_LINE)) a ${HASHES}" $CREDS_PATH/rv/config.properties;
  sed -i "s/,/,\\\\/g" $CREDS_PATH/rv/config.properties;

  printf " successful\n"
}

#########################################
# Copy artifacts to destination folders #
#########################################

# Copy only if source file exists
# Create destination folder if it doesn't exist
# Copy to destination with given filename
copy_failsafe_rename()
{
  # Arg1: Source file
  srcfile=$1

  # Arg2: Destination directory
  destdir=$2

  # Arg3: Destination file
  destfile=$3

  if [ -e $srcfile ]; then
    mkdir -p $destdir
    cp $srcfile $destdir/$destfile
  fi
}

# Create a folder if the destination folder doesn't exist
copy_failsafe()
{
  copy_failsafe_rename $1 $2 $1
}

# Copy remaining files that didn't get copied through earlier calls.
misc_copy()
{
  printf "%.40s" "Copying files to components ..................."

  # Copy owner_pub_keys.pem
  copy_failsafe_rename owner_pub_keys.pem $CREDS_PATH/aio/resources owner_customer1.pem
  copy_failsafe owner_pub_keys.pem $CREDS_PATH/manufacturer
  copy_failsafe owner_pub_keys.pem $CREDS_PATH/owner
  copy_failsafe owner_pub_keys.pem $CREDS_PATH/reseller

  # Copy reseller_pub_keys.pem
  copy_failsafe reseller_pub_keys.pem $CREDS_PATH/manufacturer
  copy_failsafe_rename reseller_pub_keys.pem $CREDS_PATH/owner owner2_pub_keys.pem

  # Import reseller keystore into owner keystore, to allow use of Owner2 key
  import_keystore owner_keystore.p12 $(cat owner_keystore.pass) \
    reseller_keystore.p12 $(cat reseller_keystore.pass)
  copy_failsafe owner_keystore.p12 $CREDS_PATH/owner

  if [ -e manufacturer_keystore.p12 ]; then
    copy_failsafe manufacturer_keystore.p12 $CREDS_PATH/aio
    PASS_KEY=$(cat manufacturer_keystore.pass)
    pass_update_env_files $CREDS_PATH/aio "manufacturer_keystore_password=${PASS_KEY}"
  fi

  if [ -e owner_keystore.p12 ]; then
    copy_failsafe owner_keystore.p12 $CREDS_PATH/aio
    PASS_KEY=$(cat owner_keystore.pass)
    pass_update_env_files $CREDS_PATH/aio "owner_keystore_password=${PASS_KEY}"
  fi

  printf " successful\n"
}

############################
# Top level function calls #
############################

# Main functionality is defined in this function
start_generation()
{
  components=("manufacturer" "reseller" "owner")
  for i in ${components[@]}; do
    if [[ "${components[@]}" =~ "$i" ]]; then
      generate_component_keys $i
    fi
  done
  generate_api_pass
  prepare_rv_config;
  generate_tls_keystore
  device_pem_files
  misc_copy
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
CERTS_PATH=$CREDS_PATH/tmp

rm -rf $CERTS_PATH $CREDS_PATH
mkdir -p $CERTS_PATH $CREDS_PATH
cd $CERTS_PATH

start_generation

# Remove temporary files
rm -rf $CERTS_PATH
echo "Generated artifacts are available in '"$CREDS_PATH"'"
