#!/bin/bash

LAST='ls -l boostrap.sh'
while true; do
  sleep 5
  NEW='ls -l boostrap.sh'
  if [ "$NEW" != "$LAST" ]; then
    sh ./bootstrap.sh
    LAST="$NEW"
  fi
done


#Save it as watch.sh and do chmod u+x watch.sh. 

#./watch.sh boostrap.sh 