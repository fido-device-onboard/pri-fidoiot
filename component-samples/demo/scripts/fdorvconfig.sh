#!/bin/bash
#
# Copyright 2024 Intel Corporation
# SPDX-License-Identifier:
#
# This script configures FDO Components AIO/Owner to complete TO0 with Hosted RV.
# Downloads the target hosted RV certificate and adds to ca-cert.pem of the mentioned components.
# Disables AutoVoucherInjection worker and Enables StandardVoucherStorage worker in AIO, that enables TO0 trigger.
# 
# USAGE
# bash fdorvconfig.sh -a <path_to_aio_directory> -o <path_to_owner_directory>
# 
# Example of providing custom proxy to use while connecting to target server
# bash fdorvconfig.sh -a aio/ -o owner/ -p http://testproxy.com:4444
# 
# Example to configure either owner or AIO
# bash fdorvconfig.sh -a aio/
# bash fdorvconfig.sh -o owner/
# 
# NOTE
# If any proxy is not provided by using argument '-p'
# This script considers environment variables "https_proxy" and "HTTPS_PROXY" for connection to Hosted RV.
# Default hosted instance is fdorv.com and port 443, but can be configured.



############################################################
# Help                                                     #
############################################################
Help()
{
    # Display Help
    echo "This script is used to configure FDO AIO, Owner components to communicate with hosted RV instance fdorv.com (default)"
    echo
    echo "Syntax: bash fdorvconfig.sh [-a|h|o|p]"
    echo "options:"
    echo "a     Absolute or relative path to AIO directory"
    echo "o     Absolute or relative path to Owner directory"
    echo "p     Proxy to be used to connect to hosted RV, if not provided, uses environment variables https_proxy and HTTPS_PROXY"
    echo "h     Help."
    echo
}

while getopts a:ho:p: flag;
do
    case "${flag}" in
        a) aio_path=${OPTARG};;
        h) Help
           exit 0;;
        o) onr_path=${OPTARG};;
        p) custom_proxy=${OPTARG};;
        \?) echo "Error: Invalid Option, use -h for help"
            exit 1;;
    esac
done

# Setting log level with color codes
colgrn='\033[0;32m'
colred='\033[0;31m'
colylw='\033[0;33m'
nocol='\033[0m'

info_level="${colgrn}INFO:${nocol}"
err_level="${colred}ERROR:${nocol}"
warn_level="${colylw}WARN:${nocol}"

if [ -z "$aio_path" ] && [ -z "$onr_path" ]; then
    echo -e "$err_level Path to either AIO or Owner directory is mandatory, check usage with -h" >&2
    exit 1
fi

conn_timeout="30"
extracted_crt="/tmp/extracted_crt"
target_rv_addr="fdorv.com"
target_rv_port="443"
hosted_rvcert="/tmp/hostedrv_crt"

# Downloads hosted RV certificate and adds to ca-cert.pem of AIO and Owner Components.
# Updating respective worker in AIO to trigger TO0 with hosted RV instance.
function config_fdo_components() {
    target_component_dir=$1
    if [ ! -d "$target_component_dir" ]; then
        echo -e "$err_level No such directory $target_component_dir exists, please check and try again." >&2
        exit 1
    fi
    if [[ "${target_component_dir: -1}" == "/" ]]; then
        target_component_dir=${target_component_dir%/}
    fi
    secrets_dir="$target_component_dir/secrets"
    cacert_file="$secrets_dir/ca-cert.pem"
    if [ ! -d "$secrets_dir" ]; then
        echo -e "$err_level $secrets_dir directory doesn't exist, please check and try again." >&2
        exit 1
    fi
    if [ ! -f "$cacert_file" ]; then
        echo -e "$err_level Required file $cacert_file doesn't exist, please check and try again." >&2
        exit 1
    fi
    # Checking if already Hosted RV certificate is present in target ca-cert.pem
    if [ -f "$hosted_rvcert" ]; then
        if [ -s "$hosted_rvcert" ]; then
            cert_lines=`wc -l $hosted_rvcert | cut -d ' ' -f 1`
            grep -A $cert_lines "END CERTIFICATE" $cacert_file | sed -n '2,$p' > $extracted_crt
            diff -q $extracted_crt $hosted_rvcert &>/dev/null
            if [[ $? -eq 0 ]]; then
                echo -e "$info_level Hosted RV $target_rv_addr certificate is already present in $cacert_file, hence skipping downloading and adding."
                return
            fi
        fi
    fi
    # Testing connection to target site and downloading certificate
    echo -e "$info_level Downloading hosted RV instance, $target_rv_addr certificate to $cacert_file"
    if [ $required_proxy ]; then
        proxy_host=$(echo $required_proxy | cut -d '/' -f 3 | cut -d ':' -f 1)
        proxy_port=$(echo $required_proxy | cut -d '/' -f 3 | cut -d ':' -f 2)
        proxy_arg="-proxy ${proxy_host}:${proxy_port}"
    else
        proxy_arg=""
    fi
    timeout --preserve-status $conn_timeout openssl s_client $proxy_arg -connect $target_rv_addr:$target_rv_port 1>/dev/null 2>/dev/null </dev/null
    if [[ $? -eq 0 ]]; then
        echo -e "$info_level Success in testing connection to $target_rv_addr"
    else
        echo -e "$err_level Failure in testing connection to $target_rv_addr after timeout of $conn_timeout seconds"
        return
    fi
    echo | openssl s_client $proxy_arg -showcerts -connect $target_rv_addr:$target_rv_port 2>/dev/null </dev/null | sed -n '/-----BEGIN CERTIFICATE-----/, /-----END CERTIFICATE-----/p' > $hosted_rvcert
    if [[ $? -eq 0 ]]; then
        cat $hosted_rvcert >> $cacert_file
        echo -e "$info_level Success in downloading certificate to $cacert_file"
    else
        echo -e "$err_level Failure downloading certificate to $cacert_file"
        return
    fi
    if [ $is_aio ] && [[ "$secrets_dir" == *"aio"* ]]; then
        echo -e "$info_level Disabling AutoInjectVoucherStorage Function and Enabling StandardVoucherStorageFunction for AIO"
        aio_config_file="$aio_path/service.yml"
        sed -i "s|  - org.fidoalliance.fdo.protocol.db.AutoInjectVoucherStorageFunction|  #- org.fidoalliance.fdo.protocol.db.AutoInjectVoucherStorageFunction|" $aio_config_file
        sed -i "s|  #- org.fidoalliance.fdo.protocol.db.StandardVoucherStorageFunction|  - org.fidoalliance.fdo.protocol.db.StandardVoucherStorageFunction|" $aio_config_file
    fi
}

# Updating HTTPS proxy information from argument or environment variables.
# Proxy information from argument takes first preference and if not provided, from https_proxy and HTTPS_PROXY variables accordingly.
if [ "$custom_proxy" ]; then
    required_proxy=$custom_proxy
    echo -e "$info_level Proxy found $required_proxy ,connection to $target_rv_addr will use the same"
else
    if [ "$https_proxy" ]; then
        required_proxy=$https_proxy
        echo -e "$info_level Proxy found $required_proxy from env variable https_proxy, connection to $target_rv_addr will use the same"
    elif [ "$HTTPS_PROXY" ]; then
        required_proxy=$HTTPS_PROXY
        echo -e "$info_level Proxy found $required_proxy from env variable HTTPS_PROXY, connection to $target_rv_addr will use the same"
    else
        echo -e "$warn_level No proxy information found, connection to $target_rv_addr will be direct"
    fi
fi

if [ "$required_proxy" ]; then
    proxy_host=$(echo $required_proxy | cut -d '/' -f 3 | cut -d ':' -f 1)
    proxy_port=$(echo $required_proxy | cut -d '/' -f 3 | cut -d ':' -f 2)
fi

if [ "$aio_path" ]; then
    is_aio=true
    config_fdo_components $aio_path
fi

if [ "$onr_path" ]; then
    config_fdo_components $onr_path
fi