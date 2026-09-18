#!/usr/bin/env bash
set -euo pipefail

# Run on the Oracle database VM. The drill restores the latest pgBackRest backup
# into an ephemeral Docker volume and starts an isolated PostgreSQL process with
# no published port. Outbound network remains available so recovery can read an
# Object Storage repository. The live PostgreSQL data directory is never mounted.

stanza="${PGBACKREST_STANZA:-deskimperial_pg17}"
postgres_image="${POSTGRES_IMAGE:-desk-imperial-db-postgres}"
repo_volume="${PGBACKREST_REPO_VOLUME:-desk-imperial-db_pgbackrest-repo}"
config_file="${PGBACKREST_CONFIG:-/opt/desk-imperial/infra/oracle/db/.runtime/pgbackrest.conf}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
volume="interpretaai-restore-drill-${stamp}"
container="interpretaai-restore-drill-${stamp}"

case "$volume" in
  interpretaai-restore-drill-*) ;;
  *) echo "Nome de volume inseguro; ensaio recusado." >&2; exit 2 ;;
esac

cleanup() {
  sudo docker rm --force "$container" >/dev/null 2>&1 || true
  sudo docker volume rm "$volume" >/dev/null 2>&1 || true
}
trap cleanup EXIT

sudo test -r "$config_file"
sudo docker image inspect "$postgres_image" >/dev/null
sudo docker volume inspect "$repo_volume" >/dev/null
sudo docker volume create "$volume" >/dev/null

sudo docker run --rm \
  --entrypoint pgbackrest \
  --volume "$repo_volume:/var/lib/pgbackrest:ro" \
  --volume "$volume:/var/lib/postgresql/data" \
  --volume "$config_file:/etc/pgbackrest/pgbackrest.conf:ro" \
  "$postgres_image" \
  --stanza="$stanza" \
  --pg1-path=/var/lib/postgresql/data/pgdata \
  --archive-mode=off \
  --type=immediate \
  restore

sudo docker run --detach --name "$container" --network bridge \
  --volume "$volume:/var/lib/postgresql/data" \
  --volume "$repo_volume:/var/lib/pgbackrest:ro" \
  --volume "$config_file:/etc/pgbackrest/pgbackrest.conf:ro" \
  --env PGDATA=/var/lib/postgresql/data/pgdata \
  "$postgres_image" \
  postgres -D /var/lib/postgresql/data/pgdata \
    -c listen_addresses= \
    -c unix_socket_directories=/tmp \
    -c max_connections=120 \
    -c archive_mode=off \
    -c shared_preload_libraries= >/dev/null

ready=false
for _ in $(seq 1 30); do
  if sudo docker exec "$container" pg_isready -h /tmp -d postgres >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 1
done
[[ "$ready" == true ]] || {
  sudo docker logs --tail 100 "$container" >&2
  echo "PostgreSQL restaurado não iniciou dentro de 30 segundos." >&2
  exit 1
}

sudo docker exec "$container" psql -h /tmp -U postgres -d postgres -Atqc \
  "select current_setting('server_version'), count(*) from pg_database where datallowconn group by 1"
echo "restore_drill=PASS isolation=no-published-port cleanup=automatic"
