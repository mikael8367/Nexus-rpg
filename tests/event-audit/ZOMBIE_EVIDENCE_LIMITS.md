# Experimental zombie-hunt mitigation

Base: `aba088117bc36fe9c1fe8e37e69e5e1c9a0a1e53` on `auditoria-seguranca-rpg`. No world data or NBT format is migrated. XP and necromancer leech remain vulnerable to a later LOWEST listener canceling their events; they are deliberately unchanged.

The zombie hunt stores a bounded, transient candidate at death without touching quest data. It accepts an initially canceled event to allow a later uncancel. It requires matching victim and identical damage source in `LivingDropsEvent`, final uncanceled death, nonpositive (not NaN) health, same world and the exact player session still online; it then uses the existing `HuntData.record` and reward code. It uses the preexisting `PendingDeaths` capacity of 512 and two server-END-tick delay. It never changes drops or persists queued candidates. Hunt must be active both at event time and when recorded. Exact vanilla `EntityZombie` only, as before.

On the locally available Forge 1.12.2 14.23.5.2860 mapped jar, `javap -c net.minecraft.entity.EntityLivingBase` shows `ForgeHooks.onLivingDeath` returning early on cancellation, and `ForgeHooks.onLivingDrops` after `dead=true`, outside the `doMobLoot` condition. This is code inspection, not an in-game demonstration. Other mods can change the implementation or emit synthetic drops. The technique is conservative evidence, NOT a universal guarantee against arbitrary mods and not a transactional post-death API.

Tradeoffs: a crash/restart, player logout/dimension change before drain, revival, a conflicting event, queue overflow (>512 candidates in the two-tick window), or a modified death path without matching drops can lose legitimate quest credit. No replay or persistent marker is written. Unsupported/malformed/future NBT remains governed by existing fail-closed `HuntData`. The feature has not been tested in a running Minecraft/Forge server or real modpack; do not describe it as a universally verified fix.

## Commands (no Gradle, no removal)

From the worktree, choose new nonexistent output paths on each run:

```sh
JAVA_HOME=/home/daytona/workspace/nexus-test/jdk \
ZOMBIE_TEST_DIR=/home/daytona/workspace/nexus-test/zombie-evidence-NEW \
python3 tests/event-audit/zombie_evidence_harness.py
```

This compiles production `ZombieDeathEvidence` and `PendingDeaths` with simulated Minecraft/Forge objects and a stub quest recorder. It exercises cancellation before/after candidate and after drops, uncancel, empty/canceled drops, duplicate, revival, NaN health, exact type, inactive quest, offline/world switch, source mismatch, client, candidate replacement and capacity. It does not run the real Forge event bus or actual quest writer.

Compilation and NBT unit tests with locally cached Forge and dependencies (the directory must not exist):

```sh
export JAVA_HOME=/home/daytona/workspace/nexus-test/jdk
FORGE=/home/daytona/workspace/nexus-test/gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar
DEPS=$(find /home/daytona/workspace/nexus-test/gradle/caches/modules-2/files-2.1 -type f -name '*.jar' | paste -sd: -)
OUT=/home/daytona/workspace/nexus-test/zombie-real-compile-NEW
mkdir "$OUT"
find src/main/java -name '*.java' > "$OUT/sources"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -cp "$FORGE:$DEPS" -d "$OUT" @"$OUT/sources" tests/event-audit/OrderingAuditTest.java tests/quests/HuntDataTest.java
"$JAVA_HOME/bin/java" -cp "$OUT:$FORGE:$DEPS" com.nexusrpg.achievements.OrderingAuditTest
"$JAVA_HOME/bin/java" -cp "$OUT:$FORGE:$DEPS" com.nexusrpg.quests.HuntDataTest
```

`OrderingAuditTest` is a model of callback ordering, not an EventBus test. `HuntDataTest` exercises the real NBT implementation and reward attribute update, including serialization, unknown/corrupt fields, duplicates, clone and unrelated data. It does not run the complete handler pipeline.
