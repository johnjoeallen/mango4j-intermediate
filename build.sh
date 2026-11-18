#!/usr/bin/env bash
# Builds every mango4j group in dependency order, installing each into the local
# Maven repository so the groups that depend on it can resolve it.
#
#   ./build.sh                      build and install everything
#   ./build.sh -Phammer-time        extra arguments are passed to every mvn call
#   GROUPS_TO_BUILD="mango4j" ./build.sh
#
# Five groups, in dependency order: mango4j (plain Java), mango4j-spring (Spring), then mango4j-logging,
# mango4j-flowsink and mango4j-swarm, each with plain and/or Spring modules. Every group depends on mango4j
# (parent pom) and, for its Spring modules, on mango4j-spring (parent pom, aop), so those go first.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

DEFAULT_GROUPS="mango4j mango4j-spring mango4j-logging mango4j-flowsink mango4j-swarm"
read -r -a groups <<< "${GROUPS_TO_BUILD:-$DEFAULT_GROUPS}"

for group in "${groups[@]}"; do
  echo
  echo "=== Building ${group} ==="
  mvn -f "${group}/pom.xml" clean install -Dgpg.skip "$@"
done

echo
echo "=== All groups built ==="
