#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
dir=$(mktemp -d)
trap 'rm -rf "$dir"' EXIT HUP INT TERM
find tests/exploration-harness/src -name '*.java' -print > "$dir/sources"
echo src/main/java/com/nexusrpg/world/exploration/ExplorationGenerator.java >> "$dir/sources"
echo src/main/java/com/nexusrpg/world/exploration/ExplorationRegistration.java >> "$dir/sources"
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -source 8 -target 8 -d "$dir" @"$dir/sources"
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$dir" com.nexusrpg.world.exploration.ExplorationHarness
