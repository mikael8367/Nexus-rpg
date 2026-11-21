#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?JDK 8 required}"
: "${FORGE_MAPPED_JAR:?mapped Forge 1.12.2 jar required}"
: "${DEPS_CP:?Minecraft and Forge dependencies required}"
out=$(mktemp -d)
# Preserve the test artifacts for inspection, rather than deleting any files.
find src/main/java -name '*.java' > "$out/sources"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -cp "$FORGE_MAPPED_JAR:$DEPS_CP" -d "$out" @"$out/sources" tests/journeys/JourneyRegressionTest.java tests/trail-hunts/TrailHuntsTest.java tests/quests/HuntDataTest.java tests/achievements/AchievementDataTest.java
for test in com.nexusrpg.quests.JourneyRegressionTest com.nexusrpg.quests.TrailHuntsTest com.nexusrpg.quests.HuntDataTest com.nexusrpg.achievements.AchievementDataTest; do
  "$JAVA_HOME/bin/java" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" "$test"
done
printf 'Compiled artifacts: %s\n' "$out"
