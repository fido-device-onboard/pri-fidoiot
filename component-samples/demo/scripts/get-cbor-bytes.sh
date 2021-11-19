#!/bin/bash

# USAGE:
#    ./get-cbor-bytes.sh rvinfo_body
#
# Example of single RV information:
#
#   ./get-cbor-bytes.sh "[[[5, \"localhost\"], [3, 8040], [12, 1], [2, h'7F000001'], [4, 8041]]]"
#
#   NOTE : Escape double quotes characters.
#   NOTE: here 7F000001 is the hex representation of the IP address 127(7F).0(00).0(00).1(01)
#
# Example of multiple RV instructions:
#     ./get-cbor-bytes.sh "[[[5, \"localhost\"], [3, 8040], [12, 1], [2, h'7F000001'], [4, 8041]],
#                   [[5, \"localhost\"], [3, 8040], [12, 1], [2, h'6A140101'], [4, 8041]]]"
#

default_diagnostic="[[[5, \"localhost\"], [3, 8040], [12, 1], [2, h'7F000001'], [4, 8041]]]"
diagnostic=${1:-$default_diagnostic}
parameterized_diagnostic=`echo $diagnostic | sed 's/ /%20/g' | sed 's/"/%22/g' | sed "s/'/%27/g"`
result=`curl https://cbor.me/?diag=${parameterized_diagnostic} --globoff `
echo -n "Byte Value : "
echo $result | grep -o "bytes=.*>Bytes" | sed 's/bytes=//g' | sed 's/>Bytes//g' | sed 's/[^A-F0-9]//g'
