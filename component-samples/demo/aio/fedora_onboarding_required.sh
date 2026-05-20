#!/usr/bin/env bash

# Run onboarding commands excluding optional digest-auth blocks.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

echo "2.5 Configure AIO to use embedded H2 DB (no external DB)"

cp service.yml service.yml.bak
cp service.env service.env.bak
cp hibernate.cfg.xml hibernate.cfg.xml.bak

# 1) Switch worker Remote DB -> Embedded DB
sed -i 's/org\.fidoalliance\.fdo\.protocol\.RemoteDatabaseServer/org.fidoalliance.fdo.protocol.EmbeddedDatabaseServer/' service.yml

# 2) Switch JDBC URL to H2
sed -i 's#hibernate.connection.url: jdbc:mariadb://host.docker.internal:3306/emdb?useSSL=$(useSSL)#hibernate.connection.url: jdbc:h2:tcp://localhost:9092//home/fdo/app-data/emdb#' service.yml

# 3) Switch Hibernate dialect to H2
sed -i 's/hibernate.dialect: org.hibernate.dialect.MariaDBDialect/hibernate.dialect: org.hibernate.dialect.H2Dialect/' service.yml

# 4) Ensure driver_class is H2
grep -q 'hibernate.connection.driver_class: org.h2.Driver' service.yml || sed -i '/^hibernate-properties:/a\  hibernate.connection.driver_class: org.h2.Driver' service.yml

# 5) Keep secrets list non-null and remove db_password secret resolution in H2 mode
perl -0777 -i -pe 's/secrets:\n\s*-\s*db_password/secrets: []/g' service.yml

# 6) Add h2-database section safely at top-level
if ! grep -q '^h2-database:' service.yml; then
  awk 'BEGIN{s="h2-database:\n  tcp-server:\n    - \"-ifNotExists\"\n  web-server:\n    - \"-webPort\"\n    - \"8082\"\n"} /^workers:/&&!a{print s; a=1} {print} END{if(!a){print ""; print s}}' service.yml > service.yml.new && mv service.yml.new service.yml
fi

# 7) Set explicit db password in env
grep -q '^db_password=' service.env && sed -i 's/^db_password=.*/db_password=fdoh2pass123/' service.env || echo 'db_password=fdoh2pass123' >> service.env

# 8) Update Hibernate XML driver class
sed -i 's/org\.mariadb\.jdbc\.Driver/org.h2.Driver/' hibernate.cfg.xml

echo "Validate edits"
grep -nE 'EmbeddedDatabaseServer|jdbc:h2:tcp://localhost:9092//home/fdo/app-data/emdb|hibernate.dialect: org.hibernate.dialect.H2Dialect|hibernate.connection.driver_class: org.h2.Driver|secrets: \[\]|h2-database:' service.yml
grep -n '^db_password=' service.env
grep -n 'org.h2.Driver' hibernate.cfg.xml

echo "2.6 Start AIO and verify health"

export AIO_HTTP_PORT="${AIO_HTTP_PORT:-8080}"
set +e
set +o pipefail

echo "2.6.2 Apply required stability settings (always)"
# Keep DB path absolute inside container
sed -i 's#hibernate.connection.url: .*#hibernate.connection.url: jdbc:h2:tcp://localhost:9092//home/fdo/app-data/emdb#' service.yml

# Use SELinux + rootless uid/gid mapping for bind mount
grep -q './app-data:/home/fdo/app-data:Z,U' docker-compose.yml || sed -i 's#\./app-data:/home/fdo/app-data[^ ]*#./app-data:/home/fdo/app-data:Z,U#' docker-compose.yml

# Reset app-data and map ownership in Podman user namespace
mkdir -p app-data
sudo chattr -Ri app-data 2>/dev/null || true
rm -rf app-data/*
chmod 775 app-data
podman unshare chown -R 1000:1000 app-data

# Fedora SELinux: allow container access to bind mount
sudo chcon -Rt container_file_t app-data 2>/dev/null || true

echo "2.6.4 Restart AIO"
podman-compose down --remove-orphans || true
podman rm -f pri-fdo-aio 2>/dev/null || true
podman-compose up --build -d
podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo "2.6.5 Verify app-data is writable in container"
podman exec pri-fdo-aio sh -lc 'id; ls -ld /home/fdo/app-data; touch /home/fdo/app-data/.writetest && ls -l /home/fdo/app-data/.writetest'

echo "2.6.6 Verify health endpoint"
for i in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${AIO_HTTP_PORT}/health" >/dev/null; then
    echo "AIO health is reachable"
    break
  fi
  sleep 2
done

curl -v "http://127.0.0.1:${AIO_HTTP_PORT}/health"

echo "Optional sections 2.6.3 and 2.6.7 were intentionally skipped in this script."
