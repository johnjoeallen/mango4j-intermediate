#!/usr/bin/env bash
# Builds every mango4j group in dependency order, installing each into the local
# Maven repository so the groups that depend on it can resolve it.
#
#   ./build.sh                      build and install everything
#   ./build.sh -Phammer-time        extra arguments are passed to every mvn call
#   GROUPS_TO_BUILD="mango4j mango4j-flowsink" ./build.sh
#
# Each directory is one future repo: mango4j (core + spring, one reactor), flowsink, swarm.
# They depend on no other group, so their order does not matter.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

DEFAULT_GROUPS="mango4j mango4j-flowsink mango4j-swarm"
read -r -a groups <<< "${GROUPS_TO_BUILD:-$DEFAULT_GROUPS}"

for group in "${groups[@]}"; do
  echo
  echo "=== Building ${group} ==="
  mvn -f "${group}/pom.xml" clean install -Dgpg.skip "$@"
done

echo
echo "=== All groups built ==="
