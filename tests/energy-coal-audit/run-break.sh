#!/bin/sh
# Headless direct breakBlock preservation regression.
set -eu
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?Set JAVA_HOME to JDK 8}"
: "${GRADLE_USER_HOME:?Set GRADLE_USER_HOME to the ForgeGradle cache}"
forge="$GRADLE_USER_HOME/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar"
[ -f "$forge" ] || { echo "Forge mapped jar not found: $forge" >&2; exit 1; }
deps=$(find "$GRADLE_USER_HOME/caches/modules-2/files-2.1" -name '*.jar' -print | paste -sd: -)
BASE_CLASSES=${BASE_CLASSES:-/home/daytona/workspace/nexus-flight-v2/build/classes/java/main}
[ -d "$BASE_CLASSES" ] || { echo "Missing base classes: $BASE_CLASSES" >&2; exit 1; }
cp="$BASE_CLASSES:$forge:$deps"
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT HUP INT TERM
"$JAVA_HOME/bin/javac" -source 8 -target 8 -cp "$cp" -d "$dir" src/main/java/com/nexusrpg/energy/TileCoalGenerator.java src/main/java/com/nexusrpg/energy/BlockPowerMachine.java tests/energy-coal-audit/CoalBreakPreservationTest.java
"$JAVA_HOME/bin/java" -cp "$dir:$cp" com.nexusrpg.energy.CoalBreakPreservationTest
