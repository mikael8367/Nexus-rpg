#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?Set JAVA_HOME to JDK 8}"
: "${GRADLE_USER_HOME:?Set GRADLE_USER_HOME to the ForgeGradle cache}"
forge="$GRADLE_USER_HOME/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar"
if [ ! -f "$forge" ]; then echo "Forge mapped jar not found: $forge" >&2; exit 1; fi
deps=$(find "$GRADLE_USER_HOME/caches/modules-2/files-2.1" -name '*.jar' -print | paste -sd: -)
cp="build/classes/java/main:$forge:$deps"
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT HUP INT TERM
# Compile the production class under test from source: do not trust a stale build.
"$JAVA_HOME/bin/javac" -source 8 -target 8 -cp "$cp" -d "$dir" src/main/java/com/nexusrpg/energy/TileCoalGenerator.java tests/energy-regression/EnergyPersistenceTest.java tests/energy-regression/CoalInventoryRegressionTest.java
"$JAVA_HOME/bin/java" -cp "$dir:$cp" com.nexusrpg.energy.EnergyPersistenceTest "$@"
"$JAVA_HOME/bin/java" -cp "$dir:$cp" com.nexusrpg.energy.CoalInventoryRegressionTest
