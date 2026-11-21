package com.nexusrpg.world.settlement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Offline-only blueprint. No Minecraft world access or registration. Coordinates are relative. */
public final class SettlementPrototype {
    public enum BiomeStyle {
        ENCHANTED_GROVE("enchanted_grove", "minecraft:planks", "minecraft:log", "minecraft:cobblestone"),
        FROST_CROWN("frost_crown", "minecraft:planks", "minecraft:log", "minecraft:stonebrick"),
        SUNKEN_EMPIRE("sunken_empire", "minecraft:sandstone", "minecraft:sandstone", "minecraft:sandstone"),
        WITCHING_MIRE("witching_mire", "minecraft:planks", "minecraft:log", "minecraft:planks"),
        CINDER_HEART("cinder_heart", "minecraft:nether_brick", "minecraft:nether_brick", "minecraft:stonebrick"),
        ROSA_SANCTUM("rosa_sanctum", "minecraft:quartz_block", "minecraft:planks", "minecraft:stonebrick"),
        UMBRAL_REACH("umbral_reach", "minecraft:stonebrick", "minecraft:planks", "minecraft:cobblestone"),
        TITAN_CANOPY("titan_canopy", "minecraft:planks", "minecraft:log", "minecraft:planks");

        public final String id, wall, roof, floor;
        BiomeStyle(String id, String wall, String roof, String floor) {
            this.id = id; this.wall = wall; this.roof = roof; this.floor = floor;
        }
        /** Exact registry path only; unknown IDs fail closed. */
        public static BiomeStyle fromRegistryName(String name) {
            for (BiomeStyle style : values()) if (("nexusrpg:" + style.id).equals(name)) return style;
            throw new IllegalArgumentException("Unknown Nexus biome: " + name);
        }
    }

    public static final class Pos {
        public final int x, y, z;
        public Pos(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Pos)) return false;
            Pos p = (Pos) other;
            return x == p.x && y == p.y && z == p.z;
        }
        @Override public int hashCode() { return (x * 31 + y) * 31 + z; }
    }

    private SettlementPrototype() {}

    /** A 7x7 cabin with an open two-block doorway and a stepped solid roof. */
    public static Map<Pos, String> cabin(BiomeStyle style) {
        requireStyle(style);
        Map<Pos, String> plan = new LinkedHashMap<Pos, String>();
        addCabin(plan, 0, style);
        return Collections.unmodifiableMap(plan);
    }

    /** Three independent cabins and a connecting path; no villagers, loot or world edits. */
    public static Map<Pos, String> village(BiomeStyle style) {
        requireStyle(style);
        Map<Pos, String> plan = new LinkedHashMap<Pos, String>();
        for (int center = -9; center <= 9; center += 9) addCabin(plan, center, style);
        for (int x = -12; x <= 12; x++) put(plan, x, 0, -4, style.floor);
        return Collections.unmodifiableMap(plan);
    }

    private static void requireStyle(BiomeStyle style) {
        if (style == null) throw new IllegalArgumentException("A biome style is required");
    }
    private static void addCabin(Map<Pos, String> plan, int center, BiomeStyle style) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            put(plan, center + x, 0, z, style.floor);
            if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                for (int y = 1; y <= 3; y++) {
                    boolean door = x == 0 && z == -3 && y <= 2;
                    boolean window = Math.abs(x) == 3 && z == 0 && y == 2;
                    if (!door && !window) put(plan, center + x, y, z, style.wall);
                }
            }
            put(plan, center + x, 4 + (3 - Math.abs(x)) / 2, z, style.roof);
        }
    }
    private static void put(Map<Pos, String> plan, int x, int y, int z, String block) {
        Pos p = new Pos(x, y, z);
        if (plan.put(p, block) != null) throw new IllegalStateException("Overlapping blueprint blocks");
    }
}
