#!/bin/sh
# Standalone Java 8 test. No Forge, Minecraft, Gradle, or real world required.
set -eu
cd "$(dirname "$0")/../.."
mkdir -p tests/settlement-prototype/out
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -source 8 -target 8 -d tests/settlement-prototype/out \
  src/main/java/com/nexusrpg/world/settlement/SettlementPrototype.java \
  tests/settlement-prototype/SettlementPrototypeTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp tests/settlement-prototype/out SettlementPrototypeTest
