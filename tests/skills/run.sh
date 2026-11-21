#!/usr/bin/env bash
# Java 8; FORGE_MAPPED_JAR aponta ao Forge 1.12.2 com mappings 20171003-1.12.
# DEPS_CP deve conter as dependências de runtime do Forge (Guava, log4j etc.).
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?Defina JAVA_HOME para um JDK 8}"
: "${FORGE_MAPPED_JAR:?Defina FORGE_MAPPED_JAR}"
: "${DEPS_CP:?Defina DEPS_CP}"
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
find src/main/java -name '*.java' > "$out/sources"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -cp "$FORGE_MAPPED_JAR:$DEPS_CP" -d "$out" @"$out/sources" tests/skills/SkillRulesTest.java tests/skills/SkillTreeTest.java tests/skills/CloneNbtRegressionTest.java tests/character/CharacterCloneProtectionTest.java
"$JAVA_HOME/bin/java" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" com.nexusrpg.skills.SkillRulesTest
"$JAVA_HOME/bin/java" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" com.nexusrpg.skills.SkillTreeTest
"$JAVA_HOME/bin/java" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" com.nexusrpg.skills.CloneNbtRegressionTest
"$JAVA_HOME/bin/java" -cp "$out:$FORGE_MAPPED_JAR:$DEPS_CP" com.nexusrpg.character.CharacterCloneProtectionTest
