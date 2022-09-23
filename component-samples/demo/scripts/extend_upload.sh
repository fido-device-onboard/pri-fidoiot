#!/bin/bash
#
# Copyright 2022 Intel Corporation
# SPDX-License-Identifier:
#
# USAGE when PRI services are configured with Digest Authentication:
#    ./extend_upload.sh -e digest -s <serial_no>
#
# USAGE when PRI services are configured with mTLS or Client Certificate Authentication:
#    ./extend_upload.sh -e mtls -s <serial_no>
#
# Example of providing serial number:
#
#   ./extend_upload.sh -e digest -s abcdef
#   ./extend_upload.sh -e mtls -s abcdef
#
# Example of providing arguments for Digest Authentication:
#
#   ./extend_upload.sh -a SECP256R1 -e digest -m 172.17.0.1 -o 172.17.0.1 -s abcdef -u apiUser -k password1 -p password2
#   ./extend_upload.sh -a SECP384R1 -e digest -m 172.17.0.1 -o 172.17.0.1 -s abcdef -u apiUser -k password1 -p password2
#   ./extend_upload.sh -a RSA2048RESTR -e digest -m 172.17.0.1 -o 172.17.0.1 -s abcdef -u apiUser -k password1 -p password2
#
# Example of providing arguments for mTLS or Client Certificate Authentication:
#
#   ./extend_upload.sh -a SECP256R1 -c /home/certs -e mtls -m 172.17.0.1 -o 172.17.0.1 -s abcdef
#   ./extend_upload.sh -a SECP384R1 -c /home/certs -e mtls -m 172.17.0.1 -o 172.17.0.1 -s abcdef
#   ./extend_upload.sh -a RSA2048RESTR -c /home/certs -e mtls -m 172.17.0.1 -o 172.17.0.1 -s abcdef

# NOTE
#
# By default, when digest authentication is used in this script, verification of server's TLS certificate is skipped.
# This scripts supports either Digest Auth or mTLS at a time but not both.
# The CA certs are to be stored in the file ca-cert.pem and client cert are stored in the file api-user.pem


############################################################
# Help                                                     #
############################################################
Help()
{
    # Display Help
    echo "This script is used to extend the voucher with provided serial number, upload to owner and trigger TO0"
    echo
    echo "Syntax: ./extend_upload.sh [-a|c|e|k|h|m|o|p|s|u]"
    echo "options:"
    echo "a     Certificate Attestation type to be retrieved, if not provided defaults to SECP256R1."
    echo "c     Path to directory where both client and CA certs are present. Used for mTLS/Client certificate Authentication."
    echo "e     Auth method to be used, Supported - digest and mtls"
    echo "k     Manufacturer API password to be used only for Digest Authentication, if not provided defaults to blank."
    echo "m     Manufacturer IP, if not provided defaults to host.docker.internal"
    echo "o     Owner IP, if not provided defaults to host.docker.internal"
    echo "p     Owner API password to be used only for Digest Authentication, if not provided defaults to blank."
    echo "u     API username to be used only for Digest Authentication, if not provided defaults to apiUser"
    echo "s     Serial number to which extension has to performed."
    echo "h     Help."
    echo
}

while getopts a:c:e:hk:m:o:p:s:u: flag;
do
    case "${flag}" in
        a) attestation_type=${OPTARG};;
        c) cert_path=${OPTARG};;
        e) auth_type=${OPTARG};;
        h) Help
           exit 0;;
        k) mfg_api_passwd=${OPTARG};;
        m) mfg_ip=${OPTARG};;
        o) onr_ip=${OPTARG};;
        p) onr_api_passwd=${OPTARG};;
        s) serial_no=${OPTARG};;
        u) api_user=${OPTARG};;
        \?) echo "Error: Invalid Option, use -h for help"
            exit 1;;
    esac
done

if [ -z "$auth_type" ]; then
    echo "Auth method is mandatory, check usage with -h" >&2
    exit 1
fi

if [ -z "$serial_no" ]; then
    echo "Serial number of device is mandatory, check usage with -h" >&2
    exit 1
fi

default_attestation_type="SECP256R1"
default_mfg_ip="host.docker.internal"
default_onr_ip="host.docker.internal"
default_api_user="apiUser"
default_mfg_api_passwd=""
default_onr_api_passwd=""
mfg_port="8038"
onr_port="8043"

attestation_type=${attestation_type:-$default_attestation_type}
mfg_ip=${mfg_ip:-$default_mfg_ip}
onr_ip=${onr_ip:-$default_onr_ip}
api_user=${api_user:-$default_api_user}
mfg_api_passwd=${mfg_api_passwd:-$default_mfg_api_passwd}
onr_api_passwd=${onr_api_passwd:-$default_onr_api_passwd}

if [[ "${auth_type,,}" == "digest" ]]; then
    echo "Digest authentication mode is being used"
    onr_auth_arg="-D - --digest -u ${api_user}:${onr_api_passwd} --insecure"
    mfg_auth_arg="-D - --digest -u ${api_user}:${mfg_api_passwd} --insecure"
elif [[ "${auth_type,,}" == "mtls" ]]; then
    echo "Client Certificate authentication mode is being used"
    if [ -z "$cert_path" ]; then
        echo "Path to client and CA certificates is mandatory for mTLS, check usage with -h" >&2
        exit 1
    fi
    if [[ "${cert_path: -1}" == "/" ]]; then
        cert_path=${cert_path%/}
    fi
    onr_auth_arg="--cacert ${cert_path}/ca-cert.pem --cert ${cert_path}/api-user.pem"
    mfg_auth_arg="--cacert ${cert_path}/ca-cert.pem --cert ${cert_path}/api-user.pem"
else
    echo "Provided Auth type is not valid, check usage with -h" >&2
    exit 1
fi
get_cert=$(curl ${onr_auth_arg} --silent -w "%{http_code}\n" --location --request GET "https://${onr_ip}:${onr_port}/api/v1/certificate?alias=${attestation_type}" -H 'Content-Type: text/plain' -o owner_cert_${attestation_type}.txt)
get_cert_code=$(tail -n1 <<< "$get_cert")
if [ "$get_cert_code" = "200" ]; then
    echo "Success in downloading ${attestation_type} owner certificate to owner_cert_${attestation_type}.txt"
    owner_certificate=`cat owner_cert_${attestation_type}.txt`
    get_voucher=$(curl ${mfg_auth_arg} --silent -w "%{http_code}\n" --location --request POST "https://${mfg_ip}:${mfg_port}/api/v1/mfg/vouchers/${serial_no}" --header 'Content-Type: text/plain' --data-raw  "$owner_certificate" -o ${serial_no}_voucher.txt)
    get_voucher_code=$(tail -n1 <<< "$get_voucher")
    if [ "$get_voucher_code" = "200" ]; then
        echo "Success in downloading extended voucher for device with serial number ${serial_no}"
        extended_voucher=`cat ${serial_no}_voucher.txt`
        upload_voucher=$(curl ${onr_auth_arg} --silent -w "%{http_code}\n" --location --request POST "https://${onr_ip}:${onr_port}/api/v1/owner/vouchers/" --header 'Content-Type: text/plain' --data-raw "$extended_voucher" -o ${serial_no}_guid.txt)
        upload_voucher_code=$(tail -n1 <<< "$upload_voucher")
        if [ "$upload_voucher_code" = "200" ]; then
            device_guid=`cat ${serial_no}_guid.txt`
            echo "Success in uploading voucher to owner for device with serial number ${serial_no}"
            echo "GUID of the device is ${device_guid}"
            trigger_to0=$(curl ${onr_auth_arg} --silent -w "%{http_code}\n" --location --request GET "https://${onr_ip}:${onr_port}/api/v1/to0/${device_guid}" --header 'Content-Type: text/plain')
            trigger_to0_code=$(tail -n1 <<< "$trigger_to0")
            if [ "$trigger_to0_code" = "200" ]; then
                echo "Success in triggering TO0 for ${serial_no} with GUID ${device_guid}"
            else
                echo "Failure in triggering TO0 for ${serial_no} with GUID ${device_guid}"
            fi
        else
            echo "Failure in uploading voucher to owner for device with serial number ${serial_no} with response code: ${upload_voucher_code}"
        fi
    else
        echo "Failure in getting extended voucher for device with serial number ${serial_no} with response code: ${get_voucher_code}"
    fi
else
    echo "Failure in getting owner certificate for type ${attestation_type} with response code: ${get_cert_code}"
fi
