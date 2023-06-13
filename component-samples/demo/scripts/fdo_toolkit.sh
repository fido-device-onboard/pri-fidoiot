#!/bin/bash

# This script takes care of the E2E execution of the FDO Service

# All methods will go here



#################    Documentation   #############################
#
#
#  Usage:
#
#  wget script_url.sh
#  export ipaddress=<ip-address-of-machine>
#  chmod 777 fdo_toolkit.sh & source fdo_toolkit.sh
#
#
#  aio_e2e_setup
#
#  
#  Pending Features:
#  
#
#  1. Ability to update rvinfo
#  2. Ability to update Owner redirect
#  3. Starting individual services
#  4. Handling proxy 
#  5. Add your ideas here
#
#
#
#
#
#
#################    Documentation   #############################


FDO_RELEASE="1.1.5.1"


chk() {
    local exitCode=$1
    local task=$2
    if [[ $exitCode == 0 ]]; then return; fi
    echo "Error: exit code $exitCode from: $task"
    exit $exitCode
}

clean_build() {
  sudo rm -rf  ~/pri_code_base/
}

download_and_unpack_binaries() {


    mkdir -p ~/pri_code_base
    cd ~/pri_code_base
    echo "Downloading Protocol Reference Implementation $FDO_RELEASE"
    curl --progress-bar -LO https://github.com/secure-device-onboard/release-fidoiot/releases/download/v$FDO_RELEASE/pri-fidoiot-v$FDO_RELEASE.tar.gz
    chk $? 'downloading pri'
    echo "Unpacking Protocol Reference Implementation $FDO_RELEASE"
    tar -zxf pri-fidoiot-v$FDO_RELEASE.tar.gz
    chk $? 'unpacking pri'

}

docker_cleaner() {
    echo "Stopping all containers"  
    docker stop $(docker ps -a -q)
    echo "Stopped all containers"
}

fdo_docker_cleaner() {
    echo "Stopping all FDO containers"
    docker stop pri-fdo-aio pri-fdo-mfg pri-fdo-rv pri-fdo-owner db_fdo-db_1
    echo "Stopped all FDO containers"
}

docker_system_pruner() {
    docker_cleaner
    echo "Removing all containers, images and volumes"
    echo "y" | docker system prune -a --volumes
    echo "Removed all containers, images and volumes"
}

isDockerComposeAtLeast() {
    : ${1:?}
    local minVersion=$1
    if ! command -v docker-compose >/dev/null 2>&1; then
        return 1   # it is not even installed
    fi
    # docker-compose is installed, check its version
    lowerVersion=$(echo -e "$(docker-compose version --short)\n$minVersion" | sort -V | head -n1)
    if [[ $lowerVersion == $minVersion ]]; then
        return 0   # the installed version was >= minVersion
    else
        return 1
    fi
}

install_dependencies() {

echo "Installing all FDO dependencies"

if java -version 2>&1 | grep version | grep -q 11.; then
        echo "Found java 11"
else
        echo "Java 11 not found, installing it..."
        apt-get update && apt-get install -y openjdk-11-jre-headless
        chk $? 'installing java 11'
fi


if ! dpkg-query -s haveged > /dev/null 2>&1; then
    echo "Haveged is required, installing it"
    sudo apt-get install -y haveged
    chk $? 'installing haveged'
fi

# If docker isn't installed, do that
if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is required, installing it..."
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    chk $? 'adding docker repository key'
    add-apt-repository "deb [arch=$(dpkg --print-architecture)] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    chk $? 'adding docker repository'
    apt-get install -y docker-ce docker-ce-cli containerd.io
    chk $? 'installing docker'
fi


# If docker-compose isn't installed, or isn't at least 1.21.0 (when docker-compose.yml version 2.4 was introduced), then install/upgrade it
# For the dependency on 1.21.0 or greater, see: https://docs.docker.com/compose/release-notes/
minVersion=1.29.2
if ! isDockerComposeAtLeast $minVersion; then
    if [[ -f '/usr/bin/docker-compose' ]]; then
        echo "Error: Need at least docker-compose $minVersion. A down-level version is currently installed, preventing us from installing the latest version. Uninstall docker-compose and rerun this script."
        exit 2
    fi
    echo "docker-compose is not installed or not at least version $minVersion, installing/upgrading it..."
    # Install docker-compose from its github repo, because that is the only way to get a recent enough version
    curl --progress-bar -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chk $? 'downloading docker-compose'
    chmod +x /usr/local/bin/docker-compose
    chk $? 'making docker-compose executable'
    ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
    chk $? 'linking docker-compose to /usr/bin'
fi


echo "Upgraded and Installed all FDO dependencies"
}

update_scripts() {
cd $1
sed -i 's/#subjectAltName = @alt_names/subjectAltName = @alt_names/g' web-server.conf
sed -i 's/#subjectAltName/subjectAltName/g' web-server.conf
head -n -8 web-server.conf > temp.txt ; mv temp.txt web-server.conf

echo "[ alt_names ]">> web-server.conf
echo "DNS.1 = $ipaddress" >> web-server.conf
echo "DNS.2 = localhost" >> web-server.conf
echo "DNS.3 = host.docker.internal" >> web-server.conf
echo "DNS.4 = host.docker.internal" >> web-server.conf
echo "IP.1 = $ipaddress" >> web-server.conf
echo "IP.2 = 127.0.0.1" >> web-server.conf
echo "IP.3 = 172.17.0.1" >> web-server.conf

}

generate_keys() {
cd $1
bash demo_ca.sh
bash web_csr_req.sh
bash user_csr_req.sh
bash keys_gen.sh
chmod -R 777 secrets/ service.env
cp -r secrets service.env ../manufacturer/
cp -r secrets service.env ../rv/
cp -r secrets service.env ../owner/
cp -r secrets service.env ../aio/
cp -r secrets ../db/
}

start_db() {
cd $1/db
if [ ! "$( docker container inspect -f '{{.State.Status}}' $container_name )" == "running" ]; then
  sed -i 's+innodb_change_buffer_max_size = 25+#innodb_change_buffer_max_size = 25+g' custom/config-file.cnf
  docker-compose up --build -d
fi
}

start_aio() {
cd $1/aio
sed -i 's/  #- org.fidoalliance.fdo.protocol.UntrustedRendezvousAcceptFunction/  - org.fidoalliance.fdo.protocol.UntrustedRendezvousAcceptFunction/g' service.yml
sed -i 's/  - org.fidoalliance.fdo.protocol.db.TrustedRendezvousAcceptFunction/  #- org.fidoalliance.fdo.protocol.db.TrustedRendezvousAcceptFunction/g' service.yml
docker-compose up --build -d
docker logs -f pri-fdo-aio
}

start_mfg() {
cd $1/manufacturer
docker-compose up --build -d
}

start_rv() {
cd $1/rv
sed -i 's/  #- org.fidoalliance.fdo.protocol.UntrustedRendezvousAcceptFunction/  - org.fidoalliance.fdo.protocol.UntrustedRendezvousAcceptFunction/g' service.yml
sed -i 's/  - org.fidoalliance.fdo.protocol.db.TrustedRendezvousAcceptFunction/  #- org.fidoalliance.fdo.protocol.db.TrustedRendezvousAcceptFunction/g' service.yml
docker-compose up --build -d
}

start_owner() {
cd $1/owner
docker-compose up --build -d
}

add_docker_internal_mappings() {

 if ! grep -q "host.docker.internal" /etc/hosts; then
   echo '127.0.0.1 host.docker.internal' | sudo tee -a /etc/hosts
 fi

 echo "Added host.docker.internal mappings"
}

aio_e2e_setup() {
  add_docker_internal_mappings
  install_dependencies
  docker_cleaner
  download_and_unpack_binaries
  update_scripts ~/pri_code_base/pri-fidoiot-v$FDO_RELEASE/scripts
  generate_keys ~/pri_code_base/pri-fidoiot-v$FDO_RELEASE/scripts
  start_db ~/pri_code_base/pri-fidoiot-v$FDO_RELEASE
  sleep 10
  start_aio ~/pri_code_base/pri-fidoiot-v$FDO_RELEASE
}

#update_rvinfo_aio() {
  #ipaddress=`ip addr | grep 'state UP' -A2 | tail -n1 | awk '{print $2}' | cut -f1  -d'/'`
  #cert_path="~/pri_code_base/pri-fidoiot-v$FDO_RELEASE/aio"
  #auth_arg="--cacert ${cert_path}/ca-cert.pem --cert ${cert_path}/api-user.pem"
  #ip={$1:-${ipaddress}}
  #get_cert=$(curl ${auth_arg} --silent -w "%{http_code}\n" --location --request POST "https://${ip}:8080/api/v1/rvinfo?ip=$ip&rvprot=http -H 'Content-Type: text/plain')
  #get_cert_code=$(tail -n1 <<< "$get_cert")
  #if [ "$get_cert_code" = "200" ]; then 
  #  echo "Successfully updated rvinfo"
  #fi  
#}



# Main Execution script starts from here

if [[ -z "${ipaddress}" ]]; then
  ipaddress=`ip addr | grep 'state UP' -A2 | tail -n1 | awk '{print $2}' | cut -f1  -d'/'`
fi

while getopts ":b:c:d:i:z" opt; do
  case $opt in
    b)
      echo "-binary parameter was passed"
      download_and_unpack_binaries  
      ;;
    c)
      docker_cleaner
      ;;
    d)
      echo "Deep cleaning"
      ;;
    i)
      install_dependencies
      ;;
    z)
      aio_e2e_setup
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      ;;
  esac
done
