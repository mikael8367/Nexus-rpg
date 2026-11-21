package com.nexusrpg.world.structures;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/** Fake-world regressions; these do not replace tests in Minecraft/Forge. */
public final class OriginalApproachRegression {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void generate(Chunk c, World w, int choice) {
        new NexusStructureGenerator().generate(new NovaHarness.Pick(choice), c.x, c.z, w, null,
            (x, z) -> {
                check(x == c.x && z == c.z, "queried a neighboring chunk");
                return c;
            });
    }

    private static void lowerRing(Chunk c) {
        for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++)
            if (x == 4 || x == 12 || z == 4 || z == 12) c.h[x][z] = 60;
    }

    public static void main(String[] args) {
        for (int style = 0; style < 8; style++) {
            for (int choice : new int[] {0, 1, NovaHarness.count[style] + 1}) {
                Chunk cliff = new Chunk(-2, 3, NovaHarness.bs[style], 64);
                World blocked = new World(cliff);
                lowerRing(cliff);
                generate(cliff, blocked, choice);
                check(blocked.writes.isEmpty(), "unreachable structure was written: style=" + style + " choice=" + choice);
                check(cliff.tiles.isEmpty(), "unreachable chest created");
            }
            Chunk flat = new Chunk(-2, 3, NovaHarness.bs[style], 64);
            World accessible = new World(flat);
            generate(flat, accessible, 0);
            check(flat.tiles.containsKey(new BlockPos(-24, 65, 56)), "flat original did not generate: style=" + style);
        }
        System.out.println("PASS: 24 inaccessible ring cases reject writes; 8 flat originals still generate; no neighboring chunk queries");
    }
}
