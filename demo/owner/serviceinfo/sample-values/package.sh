#!/bin/bash
filename=payload.bin
cksum_tx=1612472339
cksum_rx=$(cksum $filename | cut -d ' ' -f 1)
if [ $cksum_tx -eq $cksum_rx  ]; then
  echo "Device onboarded successfully."
  echo "Device onboarded successfully." > result.txt
else
  echo "ServiceInfo file transmission failed."
  echo "ServiceInfo file transmission failed." > result.txt
fi
