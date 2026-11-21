#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?Java 8 JDK required}"
: "${FORGE_MAPPED_JAR:?mapped Forge 1.12.2 jar required}"
: "${DEPS_CP:?Minecraft and Forge dependencies required}"
root="$PWD"
out=$(mktemp -d)
find src/main/java -name '*.java' > "$out/sources"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -cp "$FORGE_MAPPED_JAR:$DEPS_CP" -d "$out" @"$out/sources" tests/supplies/SupplyTest.java tests/journeys/JourneyRegressionTest.java tests/trail-hunts/TrailHuntsTest.java tests/quests/HuntDataTest.java tests/achievements/AchievementDataTest.java
cd "$out"
for test in com.nexusrpg.supplies.SupplyTest com.nexusrpg.quests.JourneyRegressionTest com.nexusrpg.quests.TrailHuntsTest com.nexusrpg.quests.HuntDataTest com.nexusrpg.achievements.AchievementDataTest; do
  "$JAVA_HOME/bin/java" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" "$test"
done
printf 'Artifacts: %s\n' "$out"
