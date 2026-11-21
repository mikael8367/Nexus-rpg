#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?Set JAVA_HOME to JDK 8}"
: "${GRADLE_USER_HOME:?Set GRADLE_USER_HOME to the ForgeGradle cache}"
forge="$GRADLE_USER_HOME/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar"
[ -f "$forge" ] || { echo "Forge mapped jar not found: $forge" >&2; exit 1; }
deps=$(find "$GRADLE_USER_HOME/caches/modules-2/files-2.1" -name '*.jar' -print | paste -sd: -)
cp="build/classes/java/main:$forge:$deps"
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT HUP INT TERM
"$JAVA_HOME/bin/javac" -source 8 -target 8 -cp "$cp" -d "$dir" src/main/java/com/nexusrpg/energy/TileCoalGenerator.java tests/energy-coal-audit/CoalNbtAuditTest.java tests/energy-coal-audit/CoalUnknownRootTest.java tests/energy-regression/CoalInventoryRegressionTest.java
"$JAVA_HOME/bin/java" -cp "$dir:$cp" com.nexusrpg.energy.CoalNbtAuditTest
"$JAVA_HOME/bin/java" -cp "$dir:$cp" com.nexusrpg.energy.CoalInventoryRegressionTest
"$JAVA_HOME/bin/java" -cp "$dir:$cp" com.nexusrpg.energy.CoalUnknownRootTest
