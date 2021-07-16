#!/bin/bash
#
# Copyright 2021 Intel Corporation
# SPDX-License-Identifier: Apache 2.0
#
# Summary:
#    keys_gen.sh script is mainly implemented to generate a new keystores,
#    device keys, ssl keystore, ssl truststore for all PRI components to
#    onboard PRI device or any other device with the fresh keys.
#
# List of output files:
#
# PRI FIDOIOT component output files. xxx -> [manufacturer, owner, reseller]
#
# /path/to/root/of/pri/binaries/creds/xxx_pub_keys.pem  -> Public Keys file
# /path/to/root/of/pri/binaries/creds/xxx_pub_keys.hash -> Hash file
# /path/to/root/of/pri/binaries/creds/xxx_keystore.p12  -> Keystore file
# /path/to/root/of/pri/binaries/creds/xxx_keystore.pass -> Keystore password file
#
# TLS truststore and keystore output files.
#
# /path/to/root/of/pri/binaries/creds/ssl.p12    -> SSL Keystore
# /path/to/root/of/pri/binaries/creds/ssl.pass   -> SSL Keystore password file
# /path/to/root/of/pri/binaries/creds/truststore -> Truststore
#
# PRI Device pem output files. xxx -> [256, 384]
# /path/to/root/of/pri/binaries/creds/device_ecxxx_cert.pem    -> Device certificate
# /path/to/root/of/pri/binaries/creds/device_ecxxx_private.key -> Device private key
#
# Commands we use for execution:
# 1) ./keys_gen.sh
#   Gives a message to make use of -h option
# 2) ./keys_gen.sh -h
#   Displays the usage message with all available options
# 3) ./keys_gen.sh /path/to/root/of/pri/binaries
#   Generates new credentials and update all destination paths
# 4) ./keys_gen.sh /path/to/root/of/pri/binaries -m
#   Generates new credentials for manufacturer and update all files within manufacturer component
# 5) ./keys_gen.sh /path/to/root/of/pri/binaries -o
#   Generates new credentials for owner and update all files within owner component
# 6) ./keys_gen.sh /path/to/root/of/pri/binaries -r
#   Generates new credentials for reseller and update all files within reseller component
# 7) ./keys_gen.sh /path/to/root/of/pri/binaries -t
#   Generates new ssl keystore and its truststore for signing and update all destination paths
# 8) ./keys_gen.sh /path/to/root/of/pri/binaries -d
#   Generates new device keys and updates all files.
# 9) ./keys_gen.sh /path/to/root/of/pri/binaries <combination of options>
#   Generates the new credentials based upon the options that we provide in a combination
#
# ToDo List:
# 1) Hashes update in the file component-samples/demo/rv/config.properties should reflect in RV database
# 2) Removing the existing credentials from the source code
# 3) Updating the code to read from the newly generated keys by overwriting the default keys
#
# Validated Scenarios with these new set of credentials:
# 1) Basic sanity testcase (e2e)
# 2) Resale testcase
# 3) Reuse testcase
# 4) SVI testcase
# 5) RV-ByPass testcase
# 6) Multiple RV-Info testcase
# 7) MTU testcase
# 8) Basic e2e testcase with HTTPS

shopt -s extglob
set -e

# Environmental variables
components=("manufacturer" "owner" "reseller")
COMP_PATH=$1
CREDS_PATH=$COMP_PATH/creds
CERTS_PATH=$CREDS_PATH/tmp

# Usage message to be displayed whenever we provide wrong inputs
usage()
{
  echo -e "Usage:
  $0 <absolute_path_of_pri_components> <OPTIONS>\n

  absolute_path_of_pri_components => /path/to/root/of/pri/binaries

  OPTIONS:
    -m => To create manufacturer keystore
    -o => To create owner keystore
    -r => To create reseller keystore
    -t => To create TLS keystore
    -d => To create device pem files
    -h => Prints help message\n
  NOTE: Order of arguments passing shouldn't change"
}

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

# Generation of keystore with rsa, ec256 and ec384 keys as shown below
# ---------------------------------------------------------------------------
# Private key    -> Public Key    -> Certificate -> P12 Key   -> P12 Keystore
# ---------------------------------------------------------------------------
# rsa_priv_key   -> rsa_pub_key   -> rsa_cert    -> rsa.p12   -> keystore.p12
# ec256_priv_key -> ec256_pub_key -> ec256_cert  -> ec256.p12 -> keystore.p12
# ec384_priv_key -> ec384_pub_key -> ec384_cert  -> ec384.p12 -> keystore.p12
# ---------------------------------------------------------------------------
# Merging the above generated public keys into one file with respective to single pri component
# Redirecting the randomly generated password into /path/to/root/of/pri/binaries/creds/pri_keys.pass
keystore_gen()
{
  PASS_KEY=`openssl rand --base64 12 | tr -dc 0-9A-Za-z`
  # RSA2048 Keys Generations
  openssl genrsa -F4 -out $CERTS_PATH/${1}_rsa2048_private.key 2048 > /dev/null 2>&1
  openssl rsa -in $CERTS_PATH/${1}_rsa2048_private.key -outform PEM -pubout -out $CERTS_PATH/${1}_rsa2048_public.key > /dev/null 2>&1
  openssl req -x509 -key $CERTS_PATH/${1}_rsa2048_private.key -out $CERTS_PATH/${1}_rsa2048_cert.pem -days 3650 -batch

  # EC256 Keys Generations
  openssl ecparam -name prime256v1 -genkey -noout -out $CERTS_PATH/${1}_ec256_private.key > /dev/null 2>&1
  openssl ec -in $CERTS_PATH/${1}_ec256_private.key -pubout -out $CERTS_PATH/${1}_ec256_public.key > /dev/null 2>&1
  openssl req -x509 -key $CERTS_PATH/${1}_ec256_private.key -out $CERTS_PATH/${1}_ec256_cert.pem -days 3650 -batch

  # EC384 Keys Generations
  openssl ecparam -name secp384r1 -genkey -noout -out $CERTS_PATH/${1}_ec384_private.key > /dev/null 2>&1
  openssl ec -in $CERTS_PATH/${1}_ec384_private.key -pubout -out $CERTS_PATH/${1}_ec384_public.key > /dev/null 2>&1
  openssl req -x509 -key $CERTS_PATH/${1}_ec384_private.key -out $CERTS_PATH/${1}_ec384_cert.pem -days 3650 -batch

  # Export RSA2048, EC256, and EC384 keys to their individual p12 files
  openssl pkcs12 -export -in $CERTS_PATH/${1}_rsa2048_cert.pem -inkey $CERTS_PATH/${1}_rsa2048_private.key \
    -name ${1}_rsa2048 -out $CERTS_PATH/${1}_rsa2048.p12 -password pass:${PASS_KEY}
  openssl pkcs12 -export -in $CERTS_PATH/${1}_ec256_cert.pem -inkey $CERTS_PATH/${1}_ec256_private.key \
    -name ${1}_ecdsa_256 -out $CERTS_PATH/${1}_ec256.p12 -password pass:${PASS_KEY}
  openssl pkcs12 -export -in $CERTS_PATH/${1}_ec384_cert.pem -inkey $CERTS_PATH/${1}_ec384_private.key \
    -name ${1}_ecdsa_384 -out $CERTS_PATH/${1}_ec384.p12 -password pass:${PASS_KEY}

  # Import rsa2048, ec256, and ec384 p12 keystores to one p12 keystore
  keytool -importkeystore -srckeystore $CERTS_PATH/${1}_rsa2048.p12 -srcstorepass ${PASS_KEY} \
    -destkeystore $CERTS_PATH/${1}_keystore.p12 -deststorepass ${PASS_KEY} -deststoretype pkcs12 -noprompt > /dev/null 2>&1
  keytool -importkeystore -srckeystore $CERTS_PATH/${1}_ec256.p12 -srcstorepass ${PASS_KEY} \
    -destkeystore $CERTS_PATH/${1}_keystore.p12 -deststorepass ${PASS_KEY} -deststoretype pkcs12 -noprompt > /dev/null 2>&1
  keytool -importkeystore -srckeystore $CERTS_PATH/${1}_ec384.p12 -srcstorepass ${PASS_KEY} \
    -destkeystore $CERTS_PATH/${1}_keystore.p12 -deststorepass ${PASS_KEY} -deststoretype pkcs12 -noprompt > /dev/null 2>&1

  # Verify the keys from p12 keystore
  keytool -list -keystore $CERTS_PATH/${1}_keystore.p12 -storepass ${PASS_KEY}

  # Creating a keys.pem files
  cat $CERTS_PATH/${1}_ec256_public.key | tee $CERTS_PATH/${1}_pub_keys.pem > /dev/null
  cat $CERTS_PATH/${1}_ec384_public.key | tee -a $CERTS_PATH/${1}_pub_keys.pem > /dev/null
  cat $CERTS_PATH/${1}_rsa2048_public.key | tee -a $CERTS_PATH/${1}_pub_keys.pem > /dev/null

  # Redirecting password into one file
  mkdir -p $CREDS_PATH/${1}
  echo "${PASS_KEY}" > $CERTS_PATH/${1}_keystore.pass
}

# Generation of all hashes using the certificates of rsa, ec256, and ec384
# Certificate -> Encrypted .der file -> Hash
# Redirect hashes to the corresponding *keys.hash files
hash_gen()
{
  # Hash generations
  openssl x509 -in $CERTS_PATH/${1}_rsa2048_cert.pem -pubkey -noout | openssl enc -base64 -d \
    > $CERTS_PATH/${1}_rsa2048.der
  export RSA2048_HASH=`cat $CERTS_PATH/${1}_rsa2048.der | openssl dgst -sha256 | awk '/s/{print toupper($2)}'`

  openssl x509 -in $CERTS_PATH/${1}_ec256_cert.pem -pubkey -noout | openssl enc -base64 -d \
    > $CERTS_PATH/${1}_ec256.der
  export EC256_HASH=`cat $CERTS_PATH/${1}_ec256.der | openssl dgst -sha256 | awk '/s/{print toupper($2)}'`

  openssl x509 -in $CERTS_PATH/${1}_ec384_cert.pem -pubkey -noout | openssl enc -base64 -d \
    > $CERTS_PATH/${1}_ec384.der
  export EC384_HASH=`cat $CERTS_PATH/${1}_ec384.der | openssl dgst -sha256 | awk '/s/{print toupper($2)}'`

  # Redirect the Hashes to one file
  echo $EC256_HASH | tee $CERTS_PATH/${1}_pub_keys.hash > /dev/null
  echo $EC384_HASH | tee -a $CERTS_PATH/${1}_pub_keys.hash > /dev/null
  echo $RSA2048_HASH | tee -a $CERTS_PATH/${1}_pub_keys.hash > /dev/null
}

# Generation of TLS keystore and truststore for signing purpose
# Updating all the keystores and truststores in the required destination paths
# Updating the ssl-keystore-password that is generated randomly in all the component environment files
tls_keystore()
{
  # Cleaning all existing component keys to regenerate fresh keys
  rm -rf $CERTS_PATH/ssl* $CERTS_PATH/truststore

  # TLS keystore and truststore creation steps
  export INTERFACE_NAME=$(ip route | grep default | sed -e "s/^.*dev.//" -e "s/.proto.*//")
  export SYS_IP="$(ifconfig | grep -A 1 $INTERFACE_NAME | tail -1 | cut -d ' ' -f 10)"
  openssl req \
    -x509 \
    -newkey rsa:2048 \
    -sha256 \
    -days 3560 \
    -nodes \
    -keyout $CERTS_PATH/tls.key \
    -out $CERTS_PATH/tls.crt \
    -subj '/CN=fdo' \
    -extensions san \
    -config <( \
    echo '[req]'; \
    echo 'distinguished_name=req'; \
    echo '[san]'; \
    echo 'subjectAltName=DNS:localhost,IP.1:127.0.0.1,IP.2:'${SYS_IP}) > /dev/null 2>&1

  export SSL_PASS=`openssl rand --base64 8 | tr -dc 0-9A-Za-z`
  openssl pkcs12 -export -in $CERTS_PATH/tls.crt -inkey $CERTS_PATH/tls.key -out $CERTS_PATH/ssl.p12 \
    -password pass:${SSL_PASS}
  keytool -import -alias fdo -file $CERTS_PATH/tls.crt -storetype PKCS12 -keystore $CERTS_PATH/truststore \
    -storepass ${SSL_PASS} -noprompt > /dev/null 2>&1
  echo "${SSL_PASS}" > $CERTS_PATH/ssl.pass; rm -rf $CERTS_PATH/tls.???
  echo "Creation of SSL keystore & Truststore completes successfully"

  # Copying the ssl keystore and truststores to the destination paths
  for comp in "manufacturer" "rv" "owner" "reseller" "aio"; do
    mkdir -p $CREDS_PATH/$comp/certs/
    cp $CERTS_PATH/ssl.p12 $CREDS_PATH/$comp/certs/
    pass_update_env_files $CREDS_PATH/$comp "${comp}_ssl_keystore-password=${SSL_PASS}"
  done

  for comp in "owner" "aio"; do
    mkdir -p $CREDS_PATH/$comp/certs
    cp $CERTS_PATH/truststore $CREDS_PATH/$comp/certs/
    pass_update_env_files $CREDS_PATH/$comp "ssl_truststore_password=${SSL_PASS}"
  done
}

# Calling the keystore_gen and hash_gen functions to generate keystores and hashes
# Updating the existing component keystores with the newly generated keystores
# Updating the corresponding passwords in all the component environment files
# Logging the path of the generated keystores or hashes or passwords
component_keys()
{
  # Cleaning all component keys that only exists in order to regenerate fresh keys
  if [[ -f $CERTS_PATH/${1}_keystore.p12 ]]; then
    rm -rf $CERTS_PATH/${1}*
  fi

  # Generating keystores and hashes of the component
  keystore_gen $1
  hash_gen $1 $PASS_KEY

  # Copying the keystores to the destination paths
  #cp $CERTS_PATH/${1}_keystore.p12 $COMP_PATH/${1}/${1}_keystore.p12
  mkdir -p $CREDS_PATH/${1}
  cp -v $CERTS_PATH/${1}_keystore.p12 $CREDS_PATH/${1}/${1}_keystore.p12

  # Updating the password in the component environment file
  pass_update_env_files $CREDS_PATH/${1} "${1}_keystore_password=${PASS_KEY}"

  # Logging the path of component hashes and it password
  echo "Creation of ${1} keystore completes successfully"
  echo -e "   ${1} hashes are redirected to the below path\n     $CERTS_PATH/${1}_keys.hash"
  echo -e "   ${1} password redirected to the below path\n     $CERTS_PATH/pri_keys.pass"
}

# Updating all component key hashes to the RV config.properties
update_rv_hashes()
{
  # Capturing all component keystore hashes into one environmental variable HASHES
  for i in $CERTS_PATH/*.hash; do
    if [[ $i =~ ".hash" ]]; then
      sed -n '1,3'p $i | tee -a $CERTS_PATH/all_hashes.txt > /dev/null
    fi
  done
  sed -i "1,$((`cat $CERTS_PATH/all_hashes.txt | wc -l`-1))s/$/,\\\\/g" $CERTS_PATH/all_hashes.txt
  HASHES=`cat $CERTS_PATH/all_hashes.txt`; rm -rf $CERTS_PATH/all_hashes.txt;

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
}

# Generating new device keys and updating it into the device.pem file
device_pem_files()
{
  # Device EC256 and EC384 pem file creation
  openssl ecparam -name prime256v1 -genkey -noout -out $CERTS_PATH/device_ec256_private.key
  openssl req -x509 -key $CERTS_PATH/device_ec256_private.key -out $CERTS_PATH/device_ec256_cert.pem -days 3650 -batch
  openssl ecparam -name secp384r1 -genkey -noout -out $CERTS_PATH/device_ec384_private.key
  openssl req -x509 -key $CERTS_PATH/device_ec384_private.key -out $CERTS_PATH/device_ec384_cert.pem -days 3650 -batch
  mkdir -p $CREDS_PATH/device
  cat $CERTS_PATH/device_ec256_cert.pem > $CREDS_PATH/device/device_ec256.pem
  cat $CERTS_PATH/device_ec256_private.key >> $CREDS_PATH/device/device_ec256.pem

  cat $CERTS_PATH/device_ec384_cert.pem > $CREDS_PATH/device/device_ec384.pem
  cat $CERTS_PATH/device_ec384_private.key >> $CREDS_PATH/device/device_ec384.pem

  cp $CREDS_PATH/device/device_ec256.pem $CREDS_PATH/device/device.pem
  echo "Creation of Device PEM files completes successfully"
}

misc_copy()
{
  # Manufacturer needs {reseller,owner}_pub_keys.pem
  mkdir -p $CREDS_PATH/manufacturer
  cp $CERTS_PATH/reseller_pub_keys.pem $CREDS_PATH/manufacturer/
  cp $CERTS_PATH/owner_pub_keys.pem $CREDS_PATH/manufacturer/

  # Reseller needs owner_pub_keys.pem
  mkdir -p $CREDS_PATH/reseller
  cp $CERTS_PATH/owner_pub_keys.pem $CREDS_PATH/reseller/

  # Owner needs owner_pub_keys_pem and {reseller=>owner2}_pub_keys.pem
  mkdir -p $CREDS_PATH/owner
  cp $CERTS_PATH/owner_pub_keys.pem $CREDS_PATH/owner/
  cp $CERTS_PATH/reseller_pub_keys.pem $CREDS_PATH/owner/owner2_pub_keys.pem

  # AIO needs owner_pub_keys.pem => owner_customer1.pem
  mkdir -p $CREDS_PATH/aio/resources
  cp $CERTS_PATH/owner_pub_keys.pem $CREDS_PATH/aio/resources/owner_customer1.pem

  # AIO needs {manufacturer,owner}_keystore.p12
  cp $CERTS_PATH/manufacturer_keystore.p12 $CREDS_PATH/aio
  PASS_KEY=$(cat $CERTS_PATH/manufacturer_keystore.pass)
  pass_update_env_files $CREDS_PATH/aio "manufacturer_keystore_password=${PASS_KEY}"

  cp $CERTS_PATH/owner_keystore.p12 $CREDS_PATH/aio
  PASS_KEY=$(cat $CERTS_PATH/owner_keystore.pass)
  pass_update_env_files $CREDS_PATH/aio "owner_keystore_password=${PASS_KEY}"
}

# Main functionality is defined in this function
start_generation()
{
  arr=($*)
  if [[ $# == 0 ]]; then
    echo "For help, provide -h to the script"; exit 1;
  elif [[ $* =~ "-h" ]]; then
    usage; exit 1;
  elif [[ $# == 1 ]]; then
    arr+=('default')
  fi
  mkdir -p $CERTS_PATH
  for option in ${arr[@]:1}; do
    case $option in
      "-m") component_keys "manufacturer";;
      "-o") component_keys "owner";;
      "-r") component_keys "reseller";;
      "-t") tls_keystore;;
      "-d") device_pem_files;;
      *)
        for i in ${components[@]}; do
          if [[ "${components[@]}" =~ "$i" ]]; then
            component_keys $i
          fi
        done
        tls_keystore
        device_pem_files
        misc_copy
        break;;
    esac
  done
  for i in $CERTS_PATH/*.hash; do
    if [[ $i =~ ".hash" ]]; then
      update_rv_hashes;
      break;
    fi
  done
}

start_generation $*

# Final status of the whole execution process
if [[ `ls $CERTS_PATH | wc -l` -gt 0 ]]; then
  echo "KEYSTORE GENERATION IS SUCCESSFUL"
  rm -rf $CERTS_PATH
  tree $CREDS_PATH
else
  echo "KEYSTORE GENERATION IS UN-SUCCESSFUL"
  usage
fi
