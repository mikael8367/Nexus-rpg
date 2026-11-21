#!/usr/bin/env bash
# JAVA_HOME = JDK 8, FORGE_MAPPED_JAR = Forge 1.12.2 mapped jar, DEPS_CP = Forge dependencies.
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?JDK 8 required}" "${FORGE_MAPPED_JAR:?mapped Forge jar required}" "${DEPS_CP:?dependencies required}"
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
find src/main/java -name '*.java' > "$out/sources"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -cp "$FORGE_MAPPED_JAR:$DEPS_CP" -d "$out" @"$out/sources" tests/character-network/CharacterNetworkReplayTest.java
"$JAVA_HOME/bin/java" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" com.nexusrpg.character.CharacterNetworkReplayTest
