# Copyright 2021 Intel Corporation
#
# Summary:
# This script is used to add the required changes to enable podman build support on RHEL. 
# It will replace the dockerfile build to Podmanfile in docker-compose.yml

#build/
sed -i 's/Dockerfile/Podmanfile/' docker-compose.yml
sed -i '/- fdo-m2:\/home\/fdouser\/.m2:rw/d' docker-compose.yml
sed -i 's/:rw/:Z/' docker-compose.yml
grep -q 'fdo-m2: {}' docker-compose.yml || sed -i 's/fdo-m2:/fdo-m2: {}/' docker-compose.yml
sed -i 's/java-11-openjdk-amd64/java-11-openjdk/' build.sh
sed -i '/export JAVA_HOME/d' build.sh

