#!/bin/bash

# USAGE:
#     ./cbor2bytes rvinfo_body
#
# Example of single RV information:
#
#   ./cbor2bytes.sh "[[[5, \"localhost\"], [3, 8040], [12, 1], [2, h'7F000001'], [4, 8443]]]"
#   NOTE: here 7F000001 is the hex representation of the IP address 127(7F).0(00).0(00).1(01)
#
# Example of multiple RV instructions:
#     ./cbor2bytes.sh  "[[[5, \"localhost\"], [3, 8040], [12, 1], [2, h'7F000001'], [4, 8443]],
#                   [[5, \"localhost\"], [3, 8040], [12, 1], [2, h'6A140101'], [4, 8443]]]"

cbor=$1
param=`echo $cbor | sed 's/ /%20/g' | sed 's/"/%22/g' | sed "s/'/%27/g"`
result=`curl http://cbor.me/?diag=${param} --globoff `
echo -n "Byte Value : "
echo $result | grep -o "bytes=.*>Bytes" | sed 's/bytes=//g' | sed 's/>Bytes//g' | sed 's/[^A-F0-9]//g'