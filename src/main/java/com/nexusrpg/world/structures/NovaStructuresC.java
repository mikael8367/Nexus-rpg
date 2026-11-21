package com.nexusrpg.world.structures;

import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** Independent surface-structure designs. Coordinates are relative to the platform.
 * The caller creates the platform and chest, validates placement, and owns the world.
 * IDs: 34 broken arch; 35 ruined watchtower; 36 ruined cloister;
 * 37 fallen aqueduct; 38 ruined shrine; 39 ruined fortress corner;
 * 40 abandoned gabled cottage; 41 abandoned lean-to house;
 * 42 open crypt dungeon; 43 breached vault dungeon; 44 stone circle;
 * 45 obelisk court; 46 ruined amphitheatre; 47 roofless forge;
 * 48 ruined gateway; 49 stepped pagoda.
 */
final class NovaStructuresC {
    private NovaStructuresC() { }

    /** index is the absolute design ID (34..49); style selects palette 5..7. */
    static void build(Map<BlockPos, IBlockState> p, int x, int y, int z, int index, int style) {
        if (style < 5 || style > 7) throw new IllegalArgumentException("style must be 5..7");
        IBlockState stone = (style == 6 ? Blocks.NETHER_BRICK : style == 7 ? Blocks.END_BRICKS : Blocks.STONEBRICK).getDefaultState();
        IBlockState rough = (style == 6 ? Blocks.NETHERRACK : style == 7 ? Blocks.END_STONE : Blocks.COBBLESTONE).getDefaultState();
        IBlockState trim = (style == 6 ? Blocks.RED_NETHER_BRICK : style == 7 ? Blocks.PURPUR_BLOCK : Blocks.MOSSY_COBBLESTONE).getDefaultState();
        IBlockState wood = Blocks.PLANKS.getDefaultState();
        IBlockState log = Blocks.LOG.getDefaultState();
        IBlockState bars = Blocks.IRON_BARS.getDefaultState();
        Shape s = new Shape(p, x, y, z);
        switch (index) {
        case 34: // Broken arch: freestanding asymmetric arch, missing right end.
            s.column(-2, -1, 1, 5, stone); s.column(2, -1, 1, 3, rough);
            s.rowX(-2, 1, 5, -1, stone); s.set(-1, 6, -1, trim);
            s.set(-3, 1, -1, rough); s.set(3, 1, -1, trim);
            s.column(-2, 2, 1, 2, rough); s.set(2, 1, 2, rough);
            break;
        case 35: // Ruined watchtower: high L-shaped wall, parapet and broken staircase.
            s.wallX(-3, 2, -3, 1, 5, stone);
            s.wallZ(-3, -2, 2, 1, 4, stone);
            s.column(-3, -3, 1, 7, trim); s.column(2, -3, 1, 6, rough);
            s.rowX(-2, 1, 6, -3, trim); s.set(0, 7, -3, stone);
            s.set(-2, 1, 1, rough); s.set(-2, 2, 0, rough); s.set(-2, 3, -1, rough);
            break;
        case 36: // Ruined cloister: low garden perimeter with four slender pillars.
            s.rowX(-3, -1, 1, -3, rough); s.rowX(1, 3, 1, -3, rough);
            s.rowX(-3, 3, 1, 3, rough); s.rowZ(-3, -2, 2, 1, rough); s.rowZ(3, -2, 2, 1, rough);
            s.column(-2, -2, 1, 4, stone); s.column(2, -2, 1, 3, stone);
            s.column(-2, 2, 1, 3, trim); s.column(2, 2, 1, 4, stone);
            s.rowX(-2, 2, 5, 2, stone); s.set(-1, 5, -2, trim);
            break;
        case 37: // Fallen aqueduct: raised segmented channel on uneven legs.
            s.column(-3, 1, 1, 4, stone); s.column(0, 1, 1, 3, rough);
            s.column(3, 1, 1, 4, stone); s.rowX(-3, 3, 5, 1, trim);
            s.rowX(-3, -1, 6, 1, stone); s.rowX(2, 3, 6, 1, stone);
            s.set(-1, 1, 3, rough); s.set(2, 1, -2, rough); s.set(3, 2, -2, stone);
            break;
        case 38: // Ruined shrine: three-tier back altar and off-center pillars.
            s.rowX(-2, 2, 1, 3, stone); s.rowX(-1, 1, 2, 3, trim); s.set(0, 3, 3, stone);
            s.column(-3, -2, 1, 4, stone); s.column(3, -2, 1, 3, stone);
            s.column(-3, 2, 1, 2, rough); s.column(3, 2, 1, 4, rough);
            s.rowX(-3, -1, 5, -2, trim); s.set(-3, 5, 2, trim);
            break;
        case 39: // Fortress corner: two crenellated, differently sized walls.
            s.wallX(-3, 3, 3, 1, 3, stone); s.wallZ(-3, -3, 2, 1, 3, stone);
            s.column(-3, 3, 1, 5, trim); s.set(-1, 4, 3, stone); s.set(1, 4, 3, stone);
            s.set(-3, 4, -3, stone); s.set(-3, 4, -1, stone); s.set(-3, 4, 1, stone);
            s.set(3, 1, 1, rough); s.set(2, 1, -2, rough);
            break;
        case 40: // Abandoned cottage: gable roof and front doorway.
            s.houseWalls(stone, log, 3);
            s.rowX(-3, 3, 4, -3, wood); s.rowX(-3, 3, 4, -2, wood);
            s.rowX(-3, 3, 4, -1, wood); s.rowX(-3, 3, 4, 0, wood);
            s.rowX(-3, 3, 4, 1, wood); s.rowX(-3, 3, 4, 2, wood); s.rowX(-3, 3, 4, 3, wood);
            s.roofRidgeZ(-2, 2, 5, wood); s.roofRidgeZ(-1, 1, 6, wood);
            s.column(2, 2, 4, 6, rough); // chimney
            break;
        case 41: // Abandoned lean-to: open side, sloping roof, porch posts.
            s.wallX(-3, 2, 3, 1, 3, wood); s.wallZ(-3, -2, 2, 1, 3, stone);
            s.column(2, 3, 1, 4, log); s.column(2, -3, 1, 3, log);
            s.column(-3, -3, 1, 4, log);
            s.wallX(-3, -2, -3, 1, 2, wood); s.set(1, 1, -3, wood);
            s.roofStrip(-3, 5, wood); s.roofStrip(-2, 5, wood); s.roofStrip(-1, 4, wood);
            s.roofStrip(0, 4, wood); s.roofStrip(1, 4, wood); s.roofStrip(2, 3, wood);
            s.set(3, 1, 2, rough); s.set(3, 2, 2, rough);
            break;
        case 42: // Open crypt dungeon: front breach, low walls, tomb alcoves, overhead lintel.
            s.room(stone, 3); s.rowX(-2, 2, 4, 3, trim);
            s.column(-2, 2, 1, 2, rough); s.column(2, 2, 1, 2, rough);
            s.column(-2, -2, 1, 3, trim); s.column(2, -2, 1, 3, trim);
            s.rowX(-2, 2, 4, -2, stone);
            s.set(-3, 4, -3, rough); s.set(3, 4, 3, rough);
            break;
        case 43: // Breached vault dungeon: vaulted chamber, barred windows, open doorway.
            s.room(stone, 3); s.set(-3, 2, 0, bars); s.set(3, 2, 0, bars);
            s.roofStrip(-3, 4, trim); s.roofStrip(3, 4, trim);
            s.roofStrip(-2, 5, stone); s.roofStrip(2, 5, stone);
            s.roofStrip(-1, 6, stone); s.roofStrip(0, 6, stone); s.roofStrip(1, 6, stone);
            s.column(-2, 2, 1, 2, rough); s.column(2, 2, 1, 2, rough);
            s.set(0, 4, -3, trim); // high lintel; entrance remains two blocks tall
            break;
        case 44: // Stone circle: staggered megaliths and small standing stones.
            s.column(-3, 0, 1, 5, stone); s.column(3, 0, 1, 4, stone);
            s.column(0, -3, 1, 3, rough); s.column(0, 3, 1, 5, stone);
            s.column(-2, -2, 1, 2, trim); s.column(2, -2, 1, 3, rough);
            s.column(-2, 2, 1, 3, rough); s.column(2, 2, 1, 2, trim);
            s.set(-3, 6, 0, trim); s.set(0, 6, 3, trim);
            break;
        case 45: // Obelisk court: off-center tall monument with ring of short posts.
            s.column(2, 2, 1, 7, stone); s.set(2, 8, 2, trim);
            s.set(1, 1, 2, trim); s.set(2, 1, 1, trim); s.set(3, 1, 2, trim); s.set(2, 1, 3, trim);
            s.column(-3, -3, 1, 2, rough); s.column(3, -3, 1, 2, rough);
            s.column(-3, 3, 1, 2, rough); s.rowX(-2, 2, 1, -3, stone);
            break;
        case 46: // Ruined amphitheatre: stepped semicircular seating, open front.
            s.rowX(-3, 3, 1, 3, rough); s.rowX(-2, 2, 2, 3, stone);
            s.rowZ(-3, -1, 2, 1, rough); s.rowZ(3, -1, 2, 1, rough);
            s.rowZ(-3, 0, 2, 2, stone); s.rowZ(3, 0, 2, 2, stone);
            s.column(-3, 3, 1, 4, trim); s.column(3, 3, 1, 3, trim);
            s.set(-2, 3, 3, trim); s.set(2, 3, 3, trim);
            break;
        case 47: // Roofless forge: stone workstation silhouette, chimney, timber supports.
            s.wallX(-3, 3, 3, 1, 2, stone); s.wallZ(-3, -1, 2, 1, 2, rough);
            s.column(2, 3, 1, 6, stone); s.column(3, 3, 1, 6, stone);
            s.rowX(1, 3, 4, 3, trim); s.set(3, 7, 3, trim);
            s.column(-3, -3, 1, 4, log); s.column(3, -3, 1, 3, log);
            s.rowX(-3, -1, 4, -3, wood); s.set(-2, 1, 1, stone);
            break;
        case 48: // Ruined gateway: double piers and high broken crenellated bridge.
            s.wallZ(-2, -1, 1, 1, 4, stone); s.wallZ(2, -1, 1, 1, 4, stone);
            s.rowX(-2, 2, 5, 0, trim); s.rowX(-2, 2, 6, 0, stone);
            s.set(-2, 7, 0, stone); s.set(0, 7, 0, stone); s.set(2, 7, 0, stone);
            s.set(-3, 1, 1, rough); s.set(3, 1, -1, rough); s.set(3, 2, -1, rough);
            break;
        case 49: // Stepped pagoda: four posts under two visibly tiered roofs.
            s.column(-2, -2, 1, 4, log); s.column(2, -2, 1, 4, log);
            s.column(-2, 2, 1, 4, log); s.column(2, 2, 1, 4, log);
            for (int dz = -3; dz <= 3; dz++) s.rowX(-3, 3, 5, dz, wood);
            for (int dz = -2; dz <= 2; dz++) s.rowX(-2, 2, 6, dz, trim);
            for (int dz = -1; dz <= 1; dz++) s.rowX(-1, 1, 7, dz, wood);
            s.set(0, 8, 0, trim); s.set(0, 9, 0, stone);
            s.set(-3, 1, 0, rough); s.set(3, 1, 0, rough);
            break;
        default:
            throw new IllegalArgumentException("structure index must be 34..49");
        }
    }

    /** Centralized bounds and chest clearance apply even if a helper is changed. */
    private static final class Shape {
        private final Map<BlockPos, IBlockState> p;
        private final int x, y, z;
        Shape(Map<BlockPos, IBlockState> p, int x, int y, int z) {
            this.p = p; this.x = x; this.y = y; this.z = z;
        }
        void set(int dx, int dy, int dz, IBlockState state) {
            if (dx < -3 || dx > 3 || dz < -3 || dz > 3 || dy < 1 || dy > 10)
                throw new IllegalArgumentException("structure outside 7x7x10 envelope");
            if (dx == 0 && dz == 0 && dy <= 2) return;
            p.put(new BlockPos(x + dx, y + dy, z + dz), state);
        }
        void column(int dx, int dz, int lo, int hi, IBlockState state) {
            for (int dy = lo; dy <= hi; dy++) set(dx, dy, dz, state);
        }
        void rowX(int lo, int hi, int dy, int dz, IBlockState state) {
            for (int dx = lo; dx <= hi; dx++) set(dx, dy, dz, state);
        }
        void rowZ(int dx, int lo, int hi, int dy, IBlockState state) {
            for (int dz = lo; dz <= hi; dz++) set(dx, dy, dz, state);
        }
        void wallX(int lo, int hi, int dz, int bottom, int top, IBlockState state) {
            for (int dy = bottom; dy <= top; dy++) rowX(lo, hi, dy, dz, state);
        }
        void wallZ(int dx, int lo, int hi, int bottom, int top, IBlockState state) {
            for (int dy = bottom; dy <= top; dy++) rowZ(dx, lo, hi, dy, state);
        }
        // A two-block-wide approach is not needed: one unobstructed 1x2 doorway is enough.
        void room(IBlockState wall, int height) {
            wallX(-3, 3, 3, 1, height, wall);
            wallZ(-3, -2, 2, 1, height, wall);
            wallZ(3, -2, 2, 1, height, wall);
            for (int dy = 1; dy <= height; dy++) {
                rowX(-3, -1, dy, -3, wall); rowX(1, 3, dy, -3, wall);
                if (dy > 2) set(0, dy, -3, wall);
            }
        }
        void houseWalls(IBlockState wall, IBlockState posts, int height) {
            room(wall, height);
            column(-3, -3, 1, height, posts); column(3, -3, 1, height, posts);
            column(-3, 3, 1, height, posts); column(3, 3, 1, height, posts);
            set(-3, 2, 0, Blocks.GLASS_PANE.getDefaultState());
            set(3, 2, 0, Blocks.GLASS_PANE.getDefaultState());
        }
        void roofStrip(int dx, int dy, IBlockState state) { rowZ(dx, -3, 3, dy, state); }
        void roofRidgeZ(int lo, int hi, int dy, IBlockState state) {
            for (int dz = -3; dz <= 3; dz++) rowX(lo, hi, dy, dz, state);
        }
    }
}
