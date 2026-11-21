package com.nexusrpg.world.structures;

import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** Plans only: the caller owns the foundation, validation and central chest. */
final class NovaStructuresB {
    private NovaStructuresB() { }

    // IDs for the coordinator:
    // 17 collapsed colonnade; 18 broken watchtower; 19 ruined amphitheatre;
    // 20 fractured archway; 21 open circular shrine; 22 fallen library;
    // 23 abandoned gabled cottage; 24 abandoned lean-to workshop;
    // 25 surface crypt dungeon; 26 surface labyrinth dungeon;
    // 27 stepped pyramid ruin; 28 broken aqueduct; 29 ruined courtyard;
    // 30 ruined windmill base; 31 star-shaped observatory;
    // 32 cliffside hermit's shelter; 33 twin ruined spires.
    static void build(Map<BlockPos, IBlockState> p, int x, int y, int z, int index, int style) {
        if (style < 2 || style > 5) throw new IllegalArgumentException("style must be 2..5");
        Builder b = new Builder(p, x, y, z, style);
        switch (index) {
        case 17: // Collapsed colonnade: parallel rows, asymmetric broken lintels.
            for (int zz : new int[] {-3, 3}) {
                b.pillar(-3, zz, 4, b.main); b.pillar(-1, zz, 3, b.main);
                b.pillar(1, zz, 2, b.main); b.pillar(3, zz, 4, b.main);
            }
            b.lineX(-3, -1, 4, -3, b.trim); b.lineX(2, 3, 5, 3, b.trim);
            b.put(-2, 1, -2, b.accent); b.put(2, 1, 2, b.accent);
            break;
        case 18: // Broken watchtower: tall rear corners and open, stepped front.
            b.pillar(-3, -3, 7, b.main); b.pillar(3, -3, 6, b.main);
            b.pillar(-3, 3, 4, b.main); b.pillar(3, 3, 3, b.main);
            b.wallX(-2, 2, -3, 1, 3, b.main);
            b.lineZ(-3, -1, 3, -3, b.main); b.lineZ(1, 2, 2, 3, b.main);
            b.lineX(-3, 0, 7, -3, b.trim); b.lineZ(-3, -1, 5, 3, b.trim);
            b.put(-2, 4, -2, b.light);
            break;
        case 19: // Amphitheatre: three tiers of seating in a U, open at the front.
            b.lineX(-3, 3, 1, -1, b.main);
            b.lineX(-3, 3, 2, -2, b.main);
            b.lineX(-3, 3, 3, -3, b.main);
            for (int zz = 0; zz <= 2; zz++) {
                b.put(-2, 1, zz, b.main); b.put(2, 1, zz, b.main);
                b.put(-3, 2, zz, b.trim); b.put(3, 2, zz, b.trim);
            }
            b.pillar(-3, 3, 2, b.accent); b.pillar(3, 3, 2, b.accent);
            break;
        case 20: // Fractured gate: two arches with offset surviving wings.
            b.pillar(-2, -1, 5, b.main); b.pillar(2, -1, 5, b.main);
            b.lineX(-2, 2, 5, -1, b.trim); b.put(0, 6, -1, b.accent);
            b.pillar(-3, -3, 3, b.main); b.pillar(3, 2, 3, b.main);
            b.lineZ(-3, -1, 3, -3, b.main); b.lineZ(-1, 2, 3, 3, b.main);
            b.put(-3, 4, -2, b.trim); b.put(3, 4, 1, b.trim);
            break;
        case 21: // Circular shrine: octagonal broken ring with four roof fragments.
            for (int d = -1; d <= 1; d++) {
                if (d != 0) b.put(d, 1, 3, b.main);
                b.put(d, 1, -3, b.main); b.put(-3, 1, d, b.main); b.put(3, 1, d, b.main);
            }
            for (int xx : new int[] {-2, 2}) for (int zz : new int[] {-2, 2}) {
                b.pillar(xx, zz, 4, b.main); b.put(xx, 5, zz, b.accent);
            }
            b.lineX(-1, 1, 5, -2, b.trim); b.lineX(-1, 1, 5, 2, b.trim);
            b.put(-2, 5, 0, b.light); b.put(2, 5, 0, b.light);
            break;
        case 22: // Fallen library: surviving back wall, niches and scattered masonry.
            b.wallX(-3, 3, -3, 1, 3, b.main);
            b.pillar(-3, -2, 4, b.trim); b.pillar(3, -2, 3, b.trim);
            b.lineX(-2, 1, 4, -3, b.trim);
            b.lineZ(-1, 1, 1, -3, b.accent); b.lineZ(-1, 2, 1, 3, b.accent);
            b.put(-2, 2, -3, b.light); b.put(2, 2, -3, b.light);
            b.put(-2, 1, 2, b.main); b.put(2, 1, 3, b.main);
            break;
        case 23: // Abandoned cottage: front doorway, windows, damaged pitched roof.
            b.houseWalls(3, b.wood);
            // Solid blocks, not falling blocks; two sloping tiers and a broken ridge.
            for (int zz = -3; zz <= 3; zz++) {
                if (zz != 1) { b.put(-3, 4, zz, b.wood); b.put(3, 4, zz, b.wood); }
                if (zz != -1) { b.put(-2, 5, zz, b.wood); b.put(2, 5, zz, b.wood); }
                if (zz != 0 && zz != 1) { b.put(-1, 6, zz, b.trim); b.put(1, 6, zz, b.trim); }
                if (zz < 0 || zz == 3) b.put(0, 7, zz, b.trim);
            }
            b.pillar(-3, -3, 4, b.log); b.pillar(3, -3, 4, b.log);
            b.put(-2, 1, -2, b.accent); b.put(2, 1, -2, b.accent);
            break;
        case 24: // Abandoned workshop: asymmetric lean-to roof and partial porch.
            b.houseWalls(3, b.main);
            for (int zz = -3; zz <= 2; zz++) for (int xx = -3; xx <= 3; xx++) {
                if ((xx == -1 && zz == -1) || (xx == 2 && zz == 1)) continue;
                int h = xx <= -1 ? 6 : (xx <= 1 ? 5 : 4);
                b.put(xx, h, zz, b.wood);
            }
            b.pillar(-3, 3, 3, b.log); b.pillar(3, 3, 3, b.log);
            b.lineX(-3, -1, 4, 3, b.wood); b.lineX(1, 3, 4, 3, b.wood);
            b.put(-2, 1, -2, b.trim); b.put(-2, 2, -2, b.accent);
            b.put(2, 1, -2, b.trim); b.put(2, 2, -2, b.light);
            break;
        case 25: // Surface crypt: roofed chamber, unobstructed south doorway.
            b.closedRoom(4, b.main);
            b.roof(5, b.trim);
            b.lineX(-2, 2, 6, -3, b.main); b.lineX(-2, 2, 6, 3, b.main);
            b.pillar(-2, -2, 2, b.accent); b.pillar(2, -2, 2, b.accent);
            b.put(-2, 3, 0, b.light); b.put(2, 3, 0, b.light);
            b.put(0, 6, 0, b.accent); b.put(0, 7, 0, b.trim);
            break;
        case 26: // Surface labyrinth: open-to-sky dungeon; turning aisle leads to chest.
            b.closedRoom(3, b.main);
            // Entry at (0,3), turn east around the first partition then west to centre.
            b.lineX(-2, 1, 1, 1, b.trim); b.lineX(-2, 1, 2, 1, b.trim);
            b.lineZ(-2, -1, 1, 2, b.trim); b.lineZ(-2, -1, 2, 2, b.trim);
            b.put(-2, 1, -2, b.accent); b.put(-2, 2, -2, b.light);
            b.put(2, 3, 2, b.light); b.put(-3, 4, -3, b.accent); b.put(3, 4, -3, b.accent);
            break;
        case 27: // Stepped pyramid: terraced perimeter with an open south approach.
            for (int level = 1; level <= 3; level++) {
                int r = 4 - level;
                for (int d = -r; d <= r; d++) {
                    b.put(d, level, -r, b.main);
                    if (d != 0) b.put(d, level, r, b.main);
                    b.put(-r, level, d, b.main); b.put(r, level, d, b.main);
                }
            }
            b.pillar(-1, -1, 4, b.trim); b.pillar(1, -1, 4, b.trim);
            b.put(0, 5, -1, b.accent); b.put(-2, 2, 2, b.light); b.put(2, 2, 2, b.light);
            break;
        case 28: // Aqueduct: long elevated channel with two arches, no water.
            for (int xx : new int[] {-3, 0, 3}) {
                b.pillar(xx, -2, 4, b.main); b.pillar(xx, 2, 4, b.main);
            }
            for (int xx = -3; xx <= 3; xx++) {
                b.put(xx, 5, -2, b.trim); b.put(xx, 5, 2, b.trim);
                b.put(xx, 6, -2, b.main); b.put(xx, 6, 2, b.main);
            }
            b.lineZ(-2, 2, 5, -3, b.trim); b.lineZ(-2, 2, 5, 3, b.trim);
            b.put(-1, 6, -2, b.accent); b.put(1, 6, 2, b.accent);
            break;
        case 29: // Courtyard: low irregular walls, central open cross, corner planters.
            b.lineX(-3, -1, 1, -3, b.main); b.lineX(1, 3, 1, -3, b.main);
            b.lineX(-3, -2, 1, 3, b.main); b.lineX(2, 3, 1, 3, b.main);
            b.lineZ(-2, 2, 1, -3, b.main); b.lineZ(-2, 2, 1, 3, b.main);
            b.pillar(-3, -3, 3, b.trim); b.pillar(3, -3, 2, b.trim);
            b.pillar(-3, 3, 2, b.trim); b.pillar(3, 3, 3, b.trim);
            b.put(-2, 1, -2, b.accent); b.put(2, 1, -2, b.accent);
            b.put(-2, 1, 2, b.accent); b.put(2, 1, 2, b.accent);
            b.put(-3, 4, -3, b.light); b.put(3, 4, 3, b.light);
            break;
        case 30: // Windmill base: octagonal shell, broken mast and one diagonal sail.
            b.closedRoom(2, b.main);
            b.pillar(-3, -3, 4, b.log); b.pillar(3, -3, 4, b.log);
            b.lineX(-2, 2, 4, -3, b.wood);
            b.pillar(0, -3, 7, b.log); b.put(0, 8, -3, b.accent);
            b.put(-1, 7, -3, b.wood); b.put(-2, 8, -3, b.wood);
            b.put(1, 5, -3, b.wood); b.put(2, 4, -3, b.wood);
            b.put(-2, 1, -2, b.trim); b.put(2, 1, -2, b.trim);
            break;
        case 31: // Observatory: four radial pylons around an open viewing circle.
            b.pillar(-3, 0, 4, b.main); b.pillar(3, 0, 4, b.main);
            b.pillar(0, -3, 6, b.main); b.pillar(0, 3, 3, b.main);
            for (int xx : new int[] {-2, 2}) for (int zz : new int[] {-2, 2}) b.put(xx, 1, zz, b.trim);
            b.lineX(-2, 2, 5, -3, b.trim); b.put(0, 7, -3, b.light);
            b.put(-3, 5, 0, b.accent); b.put(3, 5, 0, b.accent);
            break;
        case 32: // Hermit's shelter: L-shaped rock wall and partial canopy.
            b.wallX(-3, 3, -3, 1, 3, b.main);
            for (int zz = -2; zz <= 2; zz++) for (int h = 1; h <= (zz < 1 ? 3 : 2); h++) b.put(-3, h, zz, b.main);
            b.pillar(3, -3, 4, b.log); b.pillar(3, 2, 3, b.log);
            for (int xx = -3; xx <= 3; xx++) for (int zz = -3; zz <= 0; zz++)
                if (!(xx == 1 && zz == -1)) b.put(xx, 4, zz, b.wood);
            b.lineZ(1, 2, 4, -3, b.wood); b.lineZ(1, 2, 4, 3, b.wood);
            b.put(-2, 1, -2, b.trim); b.put(2, 1, -2, b.accent); b.put(-2, 3, -3, b.light);
            break;
        case 33: // Twin spires: mismatched towers joined by a broken high bridge.
            b.pillar(-3, -2, 8, b.main); b.pillar(-2, -2, 6, b.trim);
            b.pillar(3, -2, 6, b.main); b.pillar(2, -2, 5, b.trim);
            b.put(-3, 9, -2, b.accent); b.put(3, 7, -2, b.accent);
            b.lineX(-3, -1, 5, -2, b.wood); b.lineX(1, 3, 5, -2, b.wood);
            b.pillar(-3, 2, 2, b.main); b.pillar(3, 2, 3, b.main);
            b.lineZ(-2, 2, 1, -3, b.main); b.lineZ(-2, 2, 1, 3, b.main);
            b.put(-3, 3, 2, b.light); b.put(3, 4, 2, b.light);
            break;
        default: throw new IllegalArgumentException("structure index must be 17..33");
        }
    }

    private static final class Builder {
        final Map<BlockPos, IBlockState> p;
        final int x, y, z;
        final Block main, trim, accent, wood, log, light;
        Builder(Map<BlockPos, IBlockState> p, int x, int y, int z, int style) {
            this.p = p; this.x = x; this.y = y; this.z = z;
            switch (style) {
            case 2: main = Blocks.SANDSTONE; trim = Blocks.STONEBRICK; accent = Blocks.PRISMARINE; wood = Blocks.PLANKS; log = Blocks.LOG; break;
            case 3: main = Blocks.MOSSY_COBBLESTONE; trim = Blocks.STONEBRICK; accent = Blocks.COBBLESTONE; wood = Blocks.PLANKS; log = Blocks.LOG; break;
            case 4: main = Blocks.NETHER_BRICK; trim = Blocks.STONEBRICK; accent = Blocks.OBSIDIAN; wood = Blocks.PLANKS; log = Blocks.LOG; break;
            case 5: main = Blocks.QUARTZ_BLOCK; trim = Blocks.STONEBRICK; accent = Blocks.PURPUR_BLOCK; wood = Blocks.PLANKS; log = Blocks.LOG; break;
            default: throw new IllegalArgumentException("style must be 2..5");
            }
            light = Blocks.GLOWSTONE;
        }
        void put(int dx, int dy, int dz, Block block) {
            if (dx < -3 || dx > 3 || dz < -3 || dz > 3 || dy < 1 || dy > 10)
                throw new IllegalArgumentException("structure outside 7x7x10 envelope");
            // Never plan over the chest or its lid clearance, even if a helper is reused.
            if (dx == 0 && dz == 0 && dy <= 2) return;
            p.put(new BlockPos(x + dx, y + dy, z + dz), block.getDefaultState());
        }
        void pillar(int dx, int dz, int height, Block block) {
            for (int h = 1; h <= height; h++) put(dx, h, dz, block);
        }
        void lineX(int lo, int hi, int h, int dz, Block block) {
            for (int dx = lo; dx <= hi; dx++) put(dx, h, dz, block);
        }
        void lineZ(int lo, int hi, int h, int dx, Block block) {
            for (int dz = lo; dz <= hi; dz++) put(dx, h, dz, block);
        }
        void wallX(int lo, int hi, int dz, int low, int high, Block block) {
            for (int h = low; h <= high; h++) lineX(lo, hi, h, dz, block);
        }
        // Continuous wall with one two-block-high south doorway.
        void closedRoom(int height, Block block) {
            for (int h = 1; h <= height; h++) for (int d = -3; d <= 3; d++) {
                put(d, h, -3, block);
                if (d != 0 || h > 2) put(d, h, 3, block);
                put(-3, h, d, block); put(3, h, d, block);
            }
        }
        // Deliberately leave openings rather than place air into the caller's plan.
        void houseWalls(int height, Block block) {
            for (int h = 1; h <= height; h++) for (int d = -3; d <= 3; d++) {
                if (!(h == 2 && (d == -1 || d == 1))) put(d, h, -3, block);
                if (d != 0 || h > 2) put(d, h, 3, block);
                if (!(h == 2 && d == 0)) { put(-3, h, d, block); put(3, h, d, block); }
            }
        }
        void roof(int h, Block block) {
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) put(dx, h, dz, block);
        }
    }
}
