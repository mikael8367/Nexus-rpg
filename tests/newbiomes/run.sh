#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
JDK=${JDK:-/home/daytona/workspace/nexus-test/jdk}
CACHE=${CACHE:-/home/daytona/workspace/nexus-test/gradle}
OUT=${OUT:-/home/daytona/workspace/nexus-test/biomes-1436-classes}
mkdir -p "$OUT"
"$JDK/bin/javac" -source 8 -target 8 -d "$OUT" src/main/java/com/nexusrpg/world/newbiomes/FeatureBlueprint.java tests/newbiomes/BlueprintTest.java
"$JDK/bin/java" -cp "$OUT" BlueprintTest
FORGE=$(find "$CACHE/caches/forge_gradle/minecraft_user_repo" -name 'forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar' | head -1)
test -n "$FORGE"
DEPS=$(find "$CACHE/caches/modules-2/files-2.1" -name '*.jar' -printf '%p:' | tr -d '\n')
"$JDK/bin/javac" -source 8 -target 8 -proc:none -cp "$FORGE:$DEPS" -d "$OUT" src/main/java/com/nexusrpg/world/newbiomes/ExperimentalBiomes.java
python3 tests/newbiomes/test_registration.py
