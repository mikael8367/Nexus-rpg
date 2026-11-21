#!/bin/sh
# Headless Forge regression. Does not delete outputs or touch other worktrees.
set -eu
cd "$(dirname "$0")/../.."
: "${JAVA_HOME:?Set JAVA_HOME to JDK 8}"
: "${GRADLE_USER_HOME:?Set GRADLE_USER_HOME to the ForgeGradle cache}"
BASE_CLASSES=${BASE_CLASSES:-/home/daytona/workspace/nexus-flight-v2/build/classes/java/main}
forge="$GRADLE_USER_HOME/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar"
[ -d "$BASE_CLASSES" ] && [ -f "$forge" ] || { echo 'Missing base classes or Forge jar' >&2; exit 1; }
deps=$(find "$GRADLE_USER_HOME/caches/modules-2/files-2.1" -name '*.jar' -print | paste -sd: -)
cp="$BASE_CLASSES:$forge:$deps"
out=tests/energy-coal-audit/.coal-test-classes
mkdir -p "$out"
"$JAVA_HOME/bin/javac" -source 8 -target 8 -cp "$cp" -d "$out" src/main/java/com/nexusrpg/energy/TileProducer.java src/main/java/com/nexusrpg/energy/TileCoalGenerator.java src/main/java/com/nexusrpg/energy/BlockPowerMachine.java src/main/java/com/nexusrpg/energy/EnergyRegistry.java src/main/java/com/nexusrpg/energy/EnergyModels.java tests/energy-coal-audit/CoalRecoveryCarrierTest.java tests/energy-coal-audit/CoalNbtAuditTest.java tests/energy-coal-audit/CoalUnknownRootTest.java tests/energy-regression/CoalInventoryRegressionTest.java tests/energy-coal-audit/CoalBreakPreservationTest.java tests/energy-coal-audit/CoalGuardTest.java tests/energy-coal-audit/CoalCriticalRegressionTest.java tests/energy-coal-audit/CoalEdgeRegressionTest.java
for test in CoalNbtAuditTest CoalUnknownRootTest CoalInventoryRegressionTest CoalGuardTest CoalRecoveryCarrierTest CoalCriticalRegressionTest CoalEdgeRegressionTest; do
  echo "--- $test ---"
  "$JAVA_HOME/bin/java" -cp "$out:$cp" "com.nexusrpg.energy.$test"
done
echo '--- CoalBreakPreservationTest ---'
"$JAVA_HOME/bin/java" -cp "$out:$cp" com.nexusrpg.energy.CoalBreakPreservationTest
