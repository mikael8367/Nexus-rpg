#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
: "${JAVA_HOME:?Set Java 8 JDK path}"
: "${FORGE_MAPPED_JAR:?Set mapped Forge 1.12.2 jar path}"
: "${DEPS_CP:?Set Minecraft/Forge dependency classpath}"
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
python3 "$root/tests/expedition/check_assets.py"
find "$root/src/main/java" -name '*.java' > "$out/sources"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -cp "$FORGE_MAPPED_JAR:$DEPS_CP" -d "$out" @"$out/sources" "$root/tests/expedition/ExpeditionAidTest.java" "$root/tests/expedition/RecipeTest.java"
cd "$out"
for test in ExpeditionAidTest RecipeTest; do
    "$JAVA_HOME/bin/java" -Dexpedition.root="$root" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" "com.nexusrpg.expedition.$test"
done
