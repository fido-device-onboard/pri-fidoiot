#!/bin/bash

# USAGE:
#    ./extend_upload.sh -s <serial_no>
#
# Example of providing serial number:
#
#   ./extend_upload.sh -s abcdef
#
# Example of providing all arguments:
#
#   ./extend_upload.sh -a SECP256R1 -m 127.0.0.1 -o 127.0.0.1 -s abcdef -u apiUser -p password
#   ./extend_upload.sh -a SECP384R1 -m 127.0.0.1 -o 127.0.0.1 -s abcdef -u apiUser -p password
#   ./extend_upload.sh -a RSA2048RESTR -m 127.0.0.1 -o 127.0.0.1 -s abcdef -u apiUser -p password

############################################################
# Help                                                     #
############################################################
Help()
{
    # Display Help
    echo "This script is used to extend the voucher with provided serial number, upload to owner and trigger TO0"
    echo
    echo "Syntax: ./extend_upload.sh [-a|h|m|o|p|s|u]"
    echo "options:"
    echo "a     Certificate Attestation type to be retrieved, if not provided defaults to SECP256R1."
    echo "m     Manufacturer IP, if not provided defaults to localhost."
    echo "o     Owner IP, if not provided defaults to localhost."
    echo "p     API password, if not provided defaults to blank."
    echo "u     API username, if not provided defaults to apiUser"
    echo "s     Serial number to which extension has to performed."
    echo "h     Help."
    echo
}

while getopts a:hm:o:p:s:u: flag;
do
    case "${flag}" in
        a) attestation_type=${OPTARG};;
        h) Help 
           exit 0;;
        m) mfg_ip=${OPTARG};;
        o) onr_ip=${OPTARG};;
        p) api_passwd=${OPTARG};;
        s) serial_no=${OPTARG};;
        u) api_user=${OPTARG};;
        \?) echo "Error: Invalid Option, use -h for help"
            exit 1;;
    esac
done

if [ -z "$serial_no" ]; then
    echo "Serial number of device is mandatory, check usage with -h" >&2
    exit 1
fi

wrk_dir=$(pwd)

default_attestation_type="SECP256R1"
default_mfg_ip="localhost"
default_onr_ip="localhost"
default_api_user="apiUser"
default_api_passwd=""

attestation_type=${attestation_type:-$default_attestation_type}
mfg_ip=${mfg_ip:-$default_mfg_ip}
onr_ip=${onr_ip:-$default_onr_ip}
api_user=${api_user:-$default_api_user}
api_passwd=${api_passwd:-$default_api_passwd}


get_cert=$(curl --silent -w "%{http_code}\n" -D - --digest -u ${api_user}:${api_passwd} --location --request GET "http://${onr_ip}:8042/api/v1/certificate?alias=${attestation_type}" -H 'Content-Type: text/plain' -o owner_cert_${attestation_type}.txt)
get_cert_code=$(tail -n1 <<< "$get_cert")
if [ "$get_cert_code" = "200" ]; then
    echo "Success in downloading ${attestation_type} owner certificate to owner_cert_${attestation_type}.txt"
    owner_certificate=`cat owner_cert_${attestation_type}.txt`
    get_voucher=$(curl --silent -w "%{http_code}\n" -D - --digest -u ${api_user}:${api_passwd} --location --request POST "http://${mfg_ip}:8039/api/v1/mfg/vouchers/${serial_no}" --header 'Content-Type: text/plain' --data-raw  "$owner_certificate" -o ${serial_no}_voucher.txt)
    get_voucher_code=$(tail -n1 <<< "$get_voucher")
    if [ "$get_voucher_code" = "200" ]; then
        echo "Success in downloading extended voucher for device with serial number ${serial_no}"
        extended_voucher=`cat ${serial_no}_voucher.txt`
        upload_voucher=$(curl --silent -w "%{http_code}\n" -D - --digest -u ${api_user}:${api_passwd} --location --request POST "http://${onr_ip}:8042/api/v1/owner/vouchers/" --header 'Content-Type: text/plain' --data-raw "$extended_voucher" -o ${serial_no}_guid.txt)
        upload_voucher_code=$(tail -n1 <<< "$upload_voucher")
        if [ "$upload_voucher_code" = "200" ]; then
            device_guid=`cat ${serial_no}_guid.txt`
            echo "Success in uploading voucher to owner for device with serial number ${serial_no}"
            echo "GUID of the device is ${device_guid}"
            trigger_to0=$(curl --silent -w "%{http_code}\n" -D - --digest -u ${api_user}:${api_passwd} --location --request GET "http://${onr_ip}:8042/api/v1/to0/${device_guid}" --header 'Content-Type: text/plain')
            trigger_to0_code=$(tail -n1 <<< "$trigger_to0")
            echo "Success in triggering TO0 for ${serial_no} with GUID ${device_guid}"
        else
            echo "Failure in uploading voucher to owner for device with serial number ${serial_no} with response code: ${upload_voucher_code}"
        fi
    else
        echo "Failure in getting extended voucher for device with serial number ${serial_no} with response code: ${get_voucher_code}"
    fi
else
    echo "Failure in getting owner certificate for type ${attestation_type} with response code: ${get_cert_code}"
fi