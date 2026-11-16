#!/usr/bin/env bash
# Builds every mango4j group in dependency order, installing each into the local
# Maven repository so the groups that depend on it can resolve it.
#
#   ./build.sh                      build and install everything
#   ./build.sh -Phammer-time        extra arguments are passed to every mvn call
#   GROUPS_TO_BUILD="mango4j" ./build.sh
#
# Two repos: mango4j (plain Java) and mango4j-spring (Spring). mango4j-spring depends on
# mango4j (masking, flowsink-core/annotations and the parent pom), so mango4j must go first.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

DEFAULT_GROUPS="mango4j mango4j-spring"
read -r -a groups <<< "${GROUPS_TO_BUILD:-$DEFAULT_GROUPS}"

for group in "${groups[@]}"; do
  echo
  echo "=== Building ${group} ==="
  mvn -f "${group}/pom.xml" clean install -Dgpg.skip "$@"
done

echo
echo "=== All groups built ==="
