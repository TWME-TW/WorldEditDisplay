#!/usr/bin/env bash
set -euo pipefail

readonly PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly PAPER_API="https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds/latest"
readonly PACKETEVENTS_URL="https://github.com/retrooper/packetevents/releases/download/v2.13.0/packetevents-spigot-2.13.0.jar"
readonly PACKETEVENTS_SHA256="6d9ece0d87ee727a79a20b7ffbd432021609c6f52bafcb654fc2d3e9b6f064c5"
readonly PORT="${WED_E2E_PORT:-$(node -e 'const net=require("net");const server=net.createServer();server.listen(0,"127.0.0.1",()=>{console.log(server.address().port);server.close()})')}"
readonly TEMP_DIR="$(mktemp -d)"
readonly SERVER_DIR="${TEMP_DIR}/server"
readonly CACHE_DIR="${PROJECT_DIR}/target/mineflayer-cache"
readonly PAPER_JAR="${CACHE_DIR}/paper-1.21.11.jar"
readonly PACKETEVENTS_JAR="${CACHE_DIR}/packetevents-spigot-2.13.0.jar"
readonly INTEGRATION_JAR="${PROJECT_DIR}/integration/paper-plugin/target/WorldEditDisplayIntegration.jar"
SERVER_PID=""

cleanup() {
  if [[ -n "${SERVER_PID}" ]] && kill -0 "${SERVER_PID}" 2>/dev/null; then
    kill "${SERVER_PID}" 2>/dev/null || true
    for _ in $(seq 1 10); do
      if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
        break
      fi
      sleep 1
    done
    if kill -0 "${SERVER_PID}" 2>/dev/null; then
      kill -KILL "${SERVER_PID}" 2>/dev/null || true
    fi
    wait "${SERVER_PID}" 2>/dev/null || true
  fi
  rm -rf "${TEMP_DIR}"
}
trap cleanup EXIT

mvn -B clean package
mvn -B -f "${PROJECT_DIR}/integration/paper-plugin/pom.xml" clean package

mkdir -p "${CACHE_DIR}" "${SERVER_DIR}/plugins"

mapfile -t plugin_jars < <(find "${PROJECT_DIR}/target" -maxdepth 1 -type f -name 'WorldEditDisplay-*-paper.jar' -print)
if [[ "${#plugin_jars[@]}" -ne 1 ]]; then
  echo "Expected exactly one WorldEditDisplay paper JAR, found ${#plugin_jars[@]}" >&2
  exit 1
fi

paper_metadata="$(curl --fail --silent --show-error --location "${PAPER_API}")"
paper_url="$(jq -er '.downloads["server:default"].url' <<<"${paper_metadata}")"
paper_sha256="$(jq -er '.downloads["server:default"].checksums.sha256' <<<"${paper_metadata}")"
if [[ ! -f "${PAPER_JAR}" ]] || ! echo "${paper_sha256}  ${PAPER_JAR}" | sha256sum --check --status; then
  curl --fail --silent --show-error --location "${paper_url}" --output "${PAPER_JAR}"
fi
echo "${paper_sha256}  ${PAPER_JAR}" | sha256sum --check --status

if [[ ! -f "${PACKETEVENTS_JAR}" ]] || ! echo "${PACKETEVENTS_SHA256}  ${PACKETEVENTS_JAR}" | sha256sum --check --status; then
  curl --fail --silent --show-error --location "${PACKETEVENTS_URL}" --output "${PACKETEVENTS_JAR}"
fi
echo "${PACKETEVENTS_SHA256}  ${PACKETEVENTS_JAR}" | sha256sum --check --status

cp "${PAPER_JAR}" "${SERVER_DIR}/paper.jar"
cp "${PACKETEVENTS_JAR}" "${SERVER_DIR}/plugins/packetevents.jar"
cp "${plugin_jars[0]}" "${SERVER_DIR}/plugins/WorldEditDisplay.jar"
cp "${INTEGRATION_JAR}" "${SERVER_DIR}/plugins/WorldEditDisplayIntegration.jar"

printf 'eula=true\n' >"${SERVER_DIR}/eula.txt"
printf '%s\n' \
  "server-port=${PORT}" \
  'online-mode=false' \
  'spawn-protection=0' \
  'view-distance=4' \
  'simulation-distance=4' \
  'level-type=minecraft:flat' \
  'motd=WorldEditDisplay integration test' \
  >"${SERVER_DIR}/server.properties"

pushd "${SERVER_DIR}" >/dev/null
java -Xms512M -Xmx1G -jar paper.jar --nogui >server.log 2>&1 &
SERVER_PID="$!"
popd >/dev/null

for _ in $(seq 1 180); do
  if grep -q 'Done (' "${SERVER_DIR}/server.log" 2>/dev/null; then
    break
  fi
  if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
    tail -160 "${SERVER_DIR}/server.log" >&2
    exit 1
  fi
  sleep 1
done
if ! grep -q 'Done (' "${SERVER_DIR}/server.log"; then
  tail -160 "${SERVER_DIR}/server.log" >&2
  echo "Paper did not start within 180 seconds" >&2
  exit 1
fi
if ! grep -q 'Enabling WorldEditDisplay v2.4.0' "${SERVER_DIR}/server.log"; then
  tail -160 "${SERVER_DIR}/server.log" >&2
  echo "WorldEditDisplay 2.4.0 did not enable" >&2
  exit 1
fi

npm ci --prefix "${PROJECT_DIR}/integration/mineflayer" --silent
if ! WED_E2E_PORT="${PORT}" node "${PROJECT_DIR}/integration/mineflayer/test.mjs"; then
  tail -200 "${SERVER_DIR}/server.log" >&2
  exit 1
fi
