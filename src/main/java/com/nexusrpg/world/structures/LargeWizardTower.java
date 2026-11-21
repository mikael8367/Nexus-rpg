package com.nexusrpg.world.structures;

import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** Pure plan: no world access or writes. The caller owns terrain checks, loot and spawning.
 * Centre must be the centre of a chunk; base is the Y of the floor platform.
 * This larger plan is NOT compatible with the old 7x7/+10 validator.
 */
public final class LargeWizardTower {
    public static final int RADIUS = 5;
    public static final int INSPECTION_RADIUS = 6;
    public static final int TOP = 37;
    public static final int FLOOR_SPACING = 6;
    public static final int BOSS_FLOOR = 30;
    private LargeWizardTower() { }

    public static boolean supports(int style) { return style >= 0 && style < 8; }

    /** Excludes ground platform and chest so they can be committed transactionally by the generator. */
    public static void build(Map<BlockPos, IBlockState> plan, int x, int y, int z, int style) {
        if (!supports(style)) throw new IllegalArgumentException("Unsupported tower biome");
        final Block wall, accent;
        switch (style) {
        case 0: wall = Blocks.STONEBRICK; accent = Blocks.COBBLESTONE; break;
        case 1: wall = Blocks.QUARTZ_BLOCK; accent = Blocks.PRISMARINE; break;
        case 2: wall = Blocks.SANDSTONE; accent = Blocks.BRICK_BLOCK; break;
        case 3: wall = Blocks.MOSSY_COBBLESTONE; accent = Blocks.COBBLESTONE; break;
        case 4: wall = Blocks.NETHER_BRICK; accent = Blocks.RED_NETHER_BRICK; break;
        case 5: wall = Blocks.QUARTZ_BLOCK; accent = Blocks.PURPUR_BLOCK; break;
        case 6: wall = Blocks.END_BRICKS; accent = Blocks.OBSIDIAN; break;
        case 7: wall = Blocks.LOG; accent = Blocks.MOSSY_COBBLESTONE; break;
        default: throw new AssertionError();
        }
        // Eleven-wide, six fully enclosed rooms. Leave a two-high north door.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            if (Math.abs(dx) != RADIUS && Math.abs(dz) != RADIUS) continue;
            for (int h = 1; h <= 36; h++) {
                if (dz == -RADIUS && Math.abs(dx) <= 1 && h <= 2) continue;
                int inFloor = (h - 1) % FLOOR_SPACING;
                boolean window = (inFloor == 2 || inFloor == 3) &&
                    ((Math.abs(dx) == RADIUS && Math.abs(dz) <= 1) ||
                     (Math.abs(dz) == RADIUS && Math.abs(dx) <= 1));
                boolean band = h % FLOOR_SPACING == 0 || (Math.abs(dx) == RADIUS && Math.abs(dz) == RADIUS);
                put(plan,x,y,z,dx,h,dz,window ? Blocks.GLASS_PANE : band ? accent : wall);
            }
            if (((dx + dz) & 1) == 0) put(plan,x,y,z,dx,37,dz,accent);
        }
        // Plank floors. Three holes above the stair provide headroom during jumping.
        for (int floor = 6; floor <= 30; floor += 6) {
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                if (dx == -4 && dz >= -1 && dz <= 1) continue;
                put(plan,x,y,z,dx,floor,dz,Blocks.PLANKS);
            }
        }
        // Each staircase goes north-to-south, with a clear walk around to the next.
        for (int floor = 0; floor < 30; floor += 6) {
            for (int step = 1; step <= 5; step++)
                put(plan,x,y,z,-4,floor + step,-4 + step,Blocks.PLANKS);
        }
        // Roof and illumination (lights in ceiling, not blocking room floors or chest).
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++)
            put(plan,x,y,z,dx,36,dz,((dx == -2 || dx == 2) && (dz == -2 || dz == 2)) ? Blocks.GLOWSTONE : wall);
    }

    public static BlockPos chest(int x, int y, int z) { return new BlockPos(x, y + 1, z); }
    public static BlockPos bossFeet(int x, int y, int z) { return new BlockPos(x, y + BOSS_FLOOR + 1, z); }
    public static BlockPos guardFeet(int x, int y, int z, int floorIndex) {
        if (floorIndex < 0 || floorIndex >= 5) throw new IllegalArgumentException("guard floor");
        return new BlockPos(x + (floorIndex % 2 == 0 ? -2 : 2), y + floorIndex * 6 + 1,
                            z + (floorIndex < 2 ? -2 : 2));
    }
    private static void put(Map<BlockPos,IBlockState> p, int x,int y,int z,int dx,int dy,int dz,Block b) {
        p.put(new BlockPos(x + dx, y + dy, z + dz), b.getDefaultState());
    }
}
