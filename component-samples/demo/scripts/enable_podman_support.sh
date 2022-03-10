# Copyright 2021 Intel Corporation
#
# Summary:
# This script is used to add the required changes to enable FDO support on RHEL. 
# It will replace the dockerfile build to Podmanfile in docker-compose.yml

#manufacturer
sed -i 's/Dockerfile/Podmanfile/' manufacturer/docker-compose.yml
grep -q '/fdo/app-data:Z' manufacturer/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' manufacturer/docker-compose.yml

#rv
sed -i 's/Dockerfile/Podmanfile/' rv/docker-compose.yml
grep -q '/fdo/app-data:Z' rv/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' rv/docker-compose.yml

#owner
sed -i 's/Dockerfile/Podmanfile/' owner/docker-compose.yml
grep -q '/fdo/app-data:Z' owner/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' owner/docker-compose.yml

#reseller
sed -i 's/Dockerfile/Podmanfile/' reseller/docker-compose.yml
grep -q '/fdo/app-data:Z' reseller/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' reseller/docker-compose.yml

#aio
sed -i 's/Dockerfile/Podmanfile/' aio/docker-compose.yml
grep -q '/fdo/app-data:Z' aio/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' aio/docker-compose.yml

