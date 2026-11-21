package com.nexusrpg.world.structures;

import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** Independent, additive structure plans. y is the TOP of the existing platform.
 * Only solid, non-tile-entity vanilla blocks are used. No air is written: the
 * caller is responsible for validating/replacing terrain and placing the chest.
 * The north approach (x=0,z=-3..0) is deliberately clear at heights 1 and 2.
 * IDs: 0 broken arch, 1 watchtower, 2 ruined shrine, 3 fallen colonnade,
 * 4 wayside obelisk, 5 ruined courtyard, 6 broken bridge, 7 burial cairn,
 * 8 abandoned cottage, 9 abandoned lodge, 10 gatehouse ruins,
 * 11 standing stones, 12 ruined apse, 13 pavilion, 14 broken ziggurat,
 * 15 surface dungeon: crypt, 16 surface dungeon: bastion.
 */
final class NovaStructuresA {
    private NovaStructuresA() { }

    static void build(Map<BlockPos, IBlockState> p, int x, int y, int z, int index, int style) {
        Palette b = new Palette(style);
        Plan a = new Plan(p, x, y, z);
        switch (index) {
            case 0: arch(a, b); break;
            case 1: tower(a, b); break;
            case 2: shrine(a, b); break;
            case 3: colonnade(a, b); break;
            case 4: obelisk(a, b); break;
            case 5: courtyard(a, b); break;
            case 6: bridge(a, b); break;
            case 7: cairn(a, b); break;
            case 8: cottage(a, b); break;
            case 9: lodge(a, b); break;
            case 10: gatehouse(a, b); break;
            case 11: stones(a, b); break;
            case 12: apse(a, b); break;
            case 13: pavilion(a, b); break;
            case 14: ziggurat(a, b); break;
            case 15: crypt(a, b); break;
            case 16: bastion(a, b); break;
            default: throw new IllegalArgumentException("Structure index must be 0..16: " + index);
        }
    }

    private static final class Palette {
        final Block wall, accent, wood;
        Palette(int style) {
            switch (style) {
                case 0: wall = Blocks.STONEBRICK; accent = Blocks.MOSSY_COBBLESTONE; wood = Blocks.PLANKS; break;
                case 1: wall = Blocks.COBBLESTONE; accent = Blocks.STONEBRICK; wood = Blocks.PLANKS; break;
                case 2: wall = Blocks.SANDSTONE; accent = Blocks.BRICK_BLOCK; wood = Blocks.PLANKS; break;
                default: throw new IllegalArgumentException("Style must be 0..2: " + style);
            }
        }
    }

    private static final class Plan {
        final Map<BlockPos, IBlockState> p;
        final int x, y, z;
        Plan(Map<BlockPos, IBlockState> p, int x, int y, int z) {
            this.p = p; this.x = x; this.y = y; this.z = z;
        }
        void put(int dx, int dy, int dz, Block b) {
            if (dx < -3 || dx > 3 || dz < -3 || dz > 3 || dy < 1 || dy > 10)
                throw new IllegalArgumentException("Out of structure bounds");
            // Keep both the chest and its lid clear, with a two-block-high entrance.
            if (dx == 0 && dz >= -3 && dz <= 0 && dy <= 2) return;
            p.put(new BlockPos(x + dx, y + dy, z + dz), b.getDefaultState());
        }
        void column(int dx, int dz, int from, int to, Block b) {
            for (int h = from; h <= to; h++) put(dx, h, dz, b);
        }
        void lineX(int min, int max, int h, int dz, Block b) {
            for (int dx = min; dx <= max; dx++) put(dx, h, dz, b);
        }
        void lineZ(int dx, int h, int min, int max, Block b) {
            for (int dz = min; dz <= max; dz++) put(dx, h, dz, b);
        }
        void roof(int minX, int maxX, int minZ, int maxZ, int h, Block b) {
            for (int dx = minX; dx <= maxX; dx++) lineZ(dx, h, minZ, maxZ, b);
        }
        void damagedRoof(int minX, int maxX, int minZ, int maxZ, int h, Block b) {
            for (int dx = minX; dx <= maxX; dx++) for (int dz = minZ; dz <= maxZ; dz++) {
                if ((dx == -1 && dz == 0) || (dx == 2 && dz == 1)) continue;
                put(dx, h, dz, b);
            }
        }
    }

    // 0: freestanding, asymmetrically damaged transverse arch.
    private static void arch(Plan a, Palette b) {
        a.column(-2, 1, 1, 5, b.wall); a.column(2, 1, 1, 4, b.wall);
        a.lineX(-2, 2, 5, 1, b.wall); a.put(-3, 1, 1, b.accent);
        a.put(3, 1, 1, b.accent); a.put(-2, 6, 1, b.accent);
        a.put(-3, 1, 2, b.wall); a.put(2, 1, 2, b.accent);
    }
    // 1: partial tower with offset remnants and an incomplete parapet.
    private static void tower(Plan a, Palette b) {
        a.column(-2, -2, 1, 7, b.wall); a.column(2, -2, 1, 5, b.wall);
        a.column(-2, 2, 1, 7, b.wall); a.column(2, 2, 1, 4, b.wall);
        for (int h = 1; h <= 4; h++) { a.lineZ(-2, h, -1, 1, b.wall); a.lineX(-1, 1, h, 2, b.wall); }
        a.lineZ(-2, 8, -2, 2, b.accent); a.lineX(-2, 0, 8, 2, b.accent);
        a.put(-2, 9, -2, b.wall); a.put(-2, 9, 0, b.wall); a.put(-2, 9, 2, b.wall);
    }
    // 2: open shrine, four short piers and a broken rear canopy.
    private static void shrine(Plan a, Palette b) {
        for (int dx : new int[] {-2, 2}) for (int dz : new int[] {-2, 2}) a.column(dx, dz, 1, 3, b.wall);
        a.lineX(-2, 2, 4, 2, b.accent); a.lineZ(-2, 4, -2, 2, b.accent);
        a.lineX(-2, 0, 4, -2, b.accent); a.column(0, 3, 1, 2, b.accent);
        a.put(-3, 1, 2, b.wall); a.put(3, 1, 2, b.wall);
    }
    // 3: paired columns, one surviving lintel and scattered fragments.
    private static void colonnade(Plan a, Palette b) {
        for (int dz : new int[] {-2, 0, 2}) {
            a.column(-3, dz, 1, dz == 0 ? 3 : 4, b.wall);
            a.column(3, dz, 1, dz == 2 ? 2 : 4, b.wall);
        }
        a.lineZ(-3, 5, -2, 0, b.accent); a.lineZ(3, 5, -2, 0, b.accent);
        a.put(2, 1, 3, b.accent); a.put(-2, 1, 2, b.accent);
    }
    // 4: a tall rear obelisk with a stepped base and flanking markers.
    private static void obelisk(Plan a, Palette b) {
        a.lineX(-1, 1, 1, 2, b.accent); a.column(0, 2, 2, 7, b.wall);
        a.put(0, 8, 2, b.accent); a.column(-3, 1, 1, 2, b.wall);
        a.column(3, 1, 1, 2, b.wall); a.put(-2, 1, 3, b.accent); a.put(2, 1, 3, b.accent);
    }
    // 5: low irregular courtyard walls, an intact corner and rubble.
    private static void courtyard(Plan a, Palette b) {
        a.lineZ(-3, 1, -3, 3, b.wall); a.lineZ(3, 1, -2, 2, b.wall);
        a.lineX(-3, 3, 1, 3, b.wall); a.lineX(-3, -1, 1, -3, b.wall);
        a.column(-3, 3, 2, 4, b.accent); a.lineX(-3, -1, 2, 3, b.wall);
        a.put(3, 2, 2, b.accent); a.put(2, 1, 2, b.accent);
    }
    // 6: collapsed bridge-like crossbeam with ragged parapets.
    private static void bridge(Plan a, Palette b) {
        for (int dx : new int[] {-3, 3}) {
            a.column(dx, 1, 1, 4, b.wall); a.column(dx, 3, 1, 3, b.wall);
            a.lineZ(dx, 4, 1, 3, b.accent);
        }
        a.lineX(-3, 3, 4, 1, b.wall); a.lineX(-3, 1, 4, 3, b.wall);
        a.put(-2, 5, 1, b.accent); a.put(2, 5, 1, b.accent);
        a.put(-3, 1, 0, b.accent); a.put(3, 1, 0, b.accent);
    }
    // 7: rear cairn and four small upright grave markers.
    private static void cairn(Plan a, Palette b) {
        a.lineX(-2, 2, 1, 3, b.accent); a.lineX(-1, 1, 2, 3, b.wall);
        a.put(0, 3, 3, b.wall); a.column(-2, -2, 1, 2, b.wall);
        a.column(2, -2, 1, 2, b.wall); a.column(-2, 1, 1, 2, b.accent);
        a.column(2, 1, 1, 2, b.accent);
    }
    // 8: compact cottage, four walls with windows and a broken pitched roof.
    private static void cottage(Plan a, Palette b) {
        for (int h = 1; h <= 3; h++) {
            a.lineX(-2, 2, h, -2, b.wood);
            if (h == 2) {
                a.lineX(-2, -1, h, 2, b.wood); a.lineX(1, 2, h, 2, b.wood);
                for (int dx : new int[] {-2, 2}) {
                    a.put(dx, h, -1, b.wood); a.put(dx, h, 1, b.wood);
                }
            } else {
                a.lineX(-2, 2, h, 2, b.wood);
                a.lineZ(-2, h, -1, 1, b.wood); a.lineZ(2, h, -1, 1, b.wood);
            }
        }
        // Door extends to the lintel; no removal or AIR entries required.
        a.damagedRoof(-3, 3, -3, 3, 4, b.wood);
        a.damagedRoof(-2, 2, -2, 2, 5, b.wood); a.lineZ(0, 6, -2, 2, b.wood);
        a.column(2, 2, 4, 7, b.accent); a.put(2, 8, 2, b.accent);
    }
    // 9: long lodge with open side porch, raised ridge and exposed end beams.
    private static void lodge(Plan a, Palette b) {
        for (int h = 1; h <= 3; h++) {
            a.lineX(-2, 2, h, 3, b.wood);
            a.lineZ(-2, h, -2, 2, b.wood);
            a.lineZ(2, h, -2, 2, b.wood);
            a.lineX(-2, 2, h, -2, b.wood);
        }
        a.damagedRoof(-2, 2, -3, 3, 4, b.wood);
        a.damagedRoof(-1, 1, -3, 3, 5, b.wood); a.lineZ(0, 6, -3, 3, b.accent);
        a.column(-3, -3, 1, 3, b.wall); a.lineX(-3, -2, 4, -3, b.wall);
        a.put(2, 1, 2, b.accent); a.put(2, 2, 2, b.accent);
    }
    // 10: broken gatehouse: two asymmetric piers and a high crosspiece.
    private static void gatehouse(Plan a, Palette b) {
        for (int dx : new int[] {-3, -2, 2, 3}) {
            a.column(dx, -1, 1, dx < 0 ? 5 : 4, b.wall);
            a.column(dx, 1, 1, dx < 0 ? 4 : 3, b.wall);
        }
        a.lineX(-3, 3, 5, -1, b.accent); a.lineZ(-3, 6, -1, 1, b.wall);
        a.put(-3, 7, -1, b.accent); a.put(3, 6, -1, b.wall);
    }
    // 11: open circle of unequal standing stones, no roof.
    private static void stones(Plan a, Palette b) {
        a.column(-3, -2, 1, 3, b.wall); a.column(3, -2, 1, 4, b.accent);
        a.column(-3, 1, 1, 5, b.accent); a.column(3, 1, 1, 3, b.wall);
        a.column(-2, 3, 1, 2, b.wall); a.column(0, 3, 1, 4, b.accent);
        a.column(2, 3, 1, 2, b.wall); a.put(-2, 1, 2, b.accent);
    }
    // 12: U-shaped ruined apse with surviving high rear wall.
    private static void apse(Plan a, Palette b) {
        for (int h = 1; h <= 4; h++) a.lineX(-2, 2, h, 3, b.wall);
        for (int h = 1; h <= 3; h++) { a.lineZ(-3, h, 0, 2, b.wall); a.lineZ(3, h, 0, 2, b.wall); }
        a.column(-2, 3, 5, 6, b.accent); a.column(2, 3, 5, 6, b.accent);
        a.lineX(-2, 2, 5, 3, b.wall); a.put(0, 6, 3, b.accent);
        a.put(-3, 4, 1, b.accent); a.put(3, 4, 2, b.accent);
    }
    // 13: roofed open pavilion with four corner supports and a raised crown.
    private static void pavilion(Plan a, Palette b) {
        for (int dx : new int[] {-2, 2}) for (int dz : new int[] {-2, 2}) a.column(dx, dz, 1, 4, b.wall);
        a.roof(-3, 3, -3, 3, 5, b.wood); a.roof(-2, 2, -2, 2, 6, b.accent);
        a.roof(-1, 1, -1, 1, 7, b.wall); a.put(0, 8, 0, b.accent);
    }
    // 14: truncated stepped monument, open at the front and hollow at chest level.
    private static void ziggurat(Plan a, Palette b) {
        a.lineZ(-3, 1, -2, 3, b.wall); a.lineZ(3, 1, -2, 3, b.wall);
        a.lineX(-3, 3, 1, 3, b.wall); a.lineZ(-2, 2, 0, 2, b.accent);
        a.lineZ(2, 2, 0, 2, b.accent); a.lineX(-2, 2, 2, 2, b.accent);
        a.lineX(-1, 1, 3, 2, b.wall); a.put(0, 4, 2, b.accent);
    }
    // 15: surface crypt, open doorway, vaulted ceiling and small side buttresses.
    private static void crypt(Plan a, Palette b) {
        for (int h = 1; h <= 3; h++) {
            a.lineX(-2, 2, h, -2, b.wall); a.lineX(-2, 2, h, 2, b.wall);
            a.lineZ(-2, h, -1, 1, b.wall); a.lineZ(2, h, -1, 1, b.wall);
        }
        a.roof(-2, 2, -2, 2, 4, b.wall);
        a.roof(-1, 1, -2, 2, 5, b.accent); a.lineZ(0, 6, -2, 2, b.wall);
        a.column(-3, 1, 1, 2, b.accent); a.column(3, 1, 1, 2, b.accent);
        a.put(-1, 1, 1, b.accent); a.put(1, 1, 1, b.accent);
    }
    // 16: larger surface bastion with battlements and a narrow entrance.
    private static void bastion(Plan a, Palette b) {
        for (int h = 1; h <= 4; h++) {
            a.lineX(-3, 3, h, -3, b.wall); a.lineX(-3, 3, h, 3, b.wall);
            a.lineZ(-3, h, -2, 2, b.wall); a.lineZ(3, h, -2, 2, b.wall);
        }
        a.roof(-3, 3, -3, 3, 5, b.wall);
        for (int d : new int[] {-3, -1, 1, 3}) {
            a.put(d, 6, -3, b.accent); a.put(d, 6, 3, b.accent);
            a.put(-3, 6, d, b.accent); a.put(3, 6, d, b.accent);
        }
        a.column(-2, 2, 1, 2, b.accent); a.column(2, 2, 1, 2, b.accent);
    }
}
