#!/bin/sh
# Simulated unit/integration tests; requires JDK 8+. Does NOT launch Minecraft.
set -eu
cd "$(dirname "$0")/../.."
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT HUP INT TERM
find tests/structures-harness/src -name '*.java' -print > "$dir/sources"
find src/main/java/com/nexusrpg/world/structures -maxdepth 1 -name '*.java' -print >> "$dir/sources"
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -source 8 -target 8 -d "$dir" @"$dir/sources"
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$dir" com.nexusrpg.world.structures.NovaHarness
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$dir" com.nexusrpg.world.structures.WizardTowerHarness
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$dir" com.nexusrpg.world.structures.SurfaceSafetyHarness
