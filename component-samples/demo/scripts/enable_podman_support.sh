# Copyright 2021 Intel Corporation
#
# Summary:
# This script is used to add the required changes to enable FDO support on RHEL. 
# It will replace the dockerfile build to Podmanfile in docker-compose.yml

#Podman Build
if [ -f "../../../build/builder.sh" ]; then
    sed -i '/export JAVA_HOME/d' ../../../build/build.sh
fi

#manufacturer
sed -i 's/Dockerfile/Podmanfile/' manufacturer/docker-compose.yml
grep -q '/fdo/app-data:Z' manufacturer/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' manufacturer/docker-compose.yml
grep -q '_JAVA_OPTIONS=${_JAVA_OPTIONS}' manufacturer/docker-compose.yml || sed -i 's/_JAVA_OPTIONS/_JAVA_OPTIONS=${_JAVA_OPTIONS}/' manufacturer/docker-compose.yml
grep -q 'networks:' manufacturer/docker-compose.yml || sed -i '5a networks:\n \ default:\n \  name: podman\n \  external: true\n' manufacturer/docker-compose.yml
grep -q 'security_opt:' manufacturer/docker-compose.yml || sed -i '/volumes:/i \    security_opt:\n \     - label=disable' manufacturer/docker-compose.yml

#rv
sed -i 's/Dockerfile/Podmanfile/' rv/docker-compose.yml
grep -q '/fdo/app-data:Z' rv/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' rv/docker-compose.yml
grep -q '_JAVA_OPTIONS=${_JAVA_OPTIONS}' rv/docker-compose.yml || sed -i 's/_JAVA_OPTIONS/_JAVA_OPTIONS=${_JAVA_OPTIONS}/' rv/docker-compose.yml
grep -q 'networks:' rv/docker-compose.yml || sed -i '5a networks:\n \ default:\n \  name: podman\n \  external: true\n' rv/docker-compose.yml
grep -q 'security_opt:' rv/docker-compose.yml || sed -i '/volumes:/i \    security_opt:\n \     - label=disable' rv/docker-compose.yml

#owner
sed -i 's/Dockerfile/Podmanfile/' owner/docker-compose.yml
grep -q '/fdo/app-data:Z' owner/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' owner/docker-compose.yml
grep -q '_JAVA_OPTIONS=${_JAVA_OPTIONS}' owner/docker-compose.yml || sed -i 's/_JAVA_OPTIONS/_JAVA_OPTIONS=${_JAVA_OPTIONS}/' owner/docker-compose.yml
grep -q 'networks:' owner/docker-compose.yml || sed -i '5a networks:\n \ default:\n \  name: podman\n \  external: true\n' owner/docker-compose.yml
grep -q 'security_opt:' owner/docker-compose.yml || sed -i '/volumes:/i \    security_opt:\n \     - label=disable' owner/docker-compose.yml

#reseller
sed -i 's/Dockerfile/Podmanfile/' reseller/docker-compose.yml
grep -q '/fdo/app-data:Z' reseller/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' reseller/docker-compose.yml
grep -q '_JAVA_OPTIONS=${_JAVA_OPTIONS}' reseller/docker-compose.yml || sed -i 's/_JAVA_OPTIONS/_JAVA_OPTIONS=${_JAVA_OPTIONS}/' reseller/docker-compose.yml
grep -q 'networks:' reseller/docker-compose.yml || sed -i '5a networks:\n \ default:\n \  name: podman\n \  external: true\n' reseller/docker-compose.yml
grep -q 'security_opt:' reseller/docker-compose.yml || sed -i '/volumes:/i \    security_opt:\n \     - label=disable' reseller/docker-compose.yml

#aio
sed -i 's/Dockerfile/Podmanfile/' aio/docker-compose.yml
grep -q '/fdo/app-data:Z' aio/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' aio/docker-compose.yml
grep -q '_JAVA_OPTIONS=${_JAVA_OPTIONS}' aio/docker-compose.yml || sed -i 's/_JAVA_OPTIONS/_JAVA_OPTIONS=${_JAVA_OPTIONS}/' aio/docker-compose.yml
grep -q 'networks:' aio/docker-compose.yml || sed -i '5a networks:\n \ default:\n \  name: podman\n \  external: true\n' aio/docker-compose.yml
grep -q 'security_opt:' aio/docker-compose.yml || sed -i '/volumes:/i \    security_opt:\n \     - label=disable' aio/docker-compose.yml

#device
sed -i 's/Dockerfile/Podmanfile/' device/docker-compose.yml
grep -q '/fdo/app-data:Z' device/docker-compose.yml || sed -i 's/fdo\/app-data/fdo\/app-data:Z/' device/docker-compose.yml
grep -q '_JAVA_OPTIONS=${_JAVA_OPTIONS}' device/docker-compose.yml || sed -i 's/_JAVA_OPTIONS/_JAVA_OPTIONS=${_JAVA_OPTIONS}/' device/docker-compose.yml
grep -q 'networks:' device/docker-compose.yml || sed -i '5a networks:\n \ default:\n \  name: podman\n \  external: true\n' device/docker-compose.yml
grep -q 'security_opt:' device/docker-compose.yml || sed -i '/volumes:/i \    security_opt:\n \     - label=disable' device/docker-compose.yml

#db
grep -q '/var/lib/mysql:Z' db/docker-compose.yml || sed -i 's/var\/lib\/mysql/var\/lib\/mysql:Z/' db/docker-compose.yml
grep -q '/etc/mysql/conf.d:Z' db/docker-compose.yml || sed -i 's/etc\/mysql\/conf.d/etc\/mysql\/conf.d:Z/' db/docker-compose.yml
grep -q 'networks:' db/docker-compose.yml || sed -i '5a networks:\n \ default:\n \  name: podman\n \  external: true\n' db/docker-compose.yml
grep -q 'security_opt:' db/docker-compose.yml || sed -i '/volumes:/i \    security_opt:\n \     - label=disable' db/docker-compose.yml

#update .bashrc
grep -qxF 'export PODMAN_USERNS=keep-id' ~/.bashrc || echo $'\nexport PODMAN_USERNS=keep-id' >> ~/.bashrc
source ~/.bashrc
