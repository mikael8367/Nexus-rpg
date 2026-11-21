package com.nexusrpg.world.structures;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** Isolated plan checks, using fakes. No worldgen, encounter or real Minecraft test. */
public final class LargeWizardTowerPlanHarness {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static BlockPos p(int x, int y, int z) { return new BlockPos(x, y, z); }
    private static void clear(Map<BlockPos, IBlockState> plan, BlockPos p) {
        check(!plan.containsKey(p), "obstructed " + p);
    }
    public static void main(String[] args) throws Exception {
        Method oldValidator = NexusStructureGenerator.class.getDeclaredMethod("validNewDecoration", Map.class, int.class, int.class, int.class);
        oldValidator.setAccessible(true);
        for (int style = 0; style < 8; style++) {
            int x = 8, y = 64, z = 8;
            Map<BlockPos, IBlockState> plan = new LinkedHashMap<BlockPos, IBlockState>();
            LargeWizardTower.build(plan, x, y, z, style);
            check(!plan.isEmpty(), "empty style " + style);
            check(!(Boolean) oldValidator.invoke(null, plan, x, y, z), "old validator unexpectedly accepts large plan");
            int minX = 100, maxX = -100, minZ = 100, maxZ = -100, top = 0;
            for (Map.Entry<BlockPos,IBlockState> e : plan.entrySet()) {
                BlockPos q = e.getKey();
                int dx = q.getX() - x, dz = q.getZ() - z, dy = q.getY() - y;
                check(Math.abs(dx) <= 5 && Math.abs(dz) <= 5 && dy >= 1 && dy <= 37, "outside plan envelope");
                check(!e.getValue().getBlock().hasTileEntity(e.getValue()), "plan contains tile");
                minX = Math.min(minX, dx); maxX = Math.max(maxX, dx);
                minZ = Math.min(minZ, dz); maxZ = Math.max(maxZ, dz); top = Math.max(top, dy);
            }
            check(minX == -5 && maxX == 5 && minZ == -5 && maxZ == 5 && top == 37, "wrong dimensions");
            BlockPos chest = LargeWizardTower.chest(x,y,z);
            clear(plan, chest); clear(plan, chest.up());
            // Three-wide, two-high north entrance and path to chest.
            for (int dx = -1; dx <= 1; dx++) for (int h = 1; h <= 2; h++) clear(plan, p(x+dx,y+h,z-5));
            for (int dz = -4; dz <= -1; dz++) for (int h = 1; h <= 2; h++) clear(plan, p(x,y+h,z+dz));
            // Stairs are full blocks, with headroom and a solid landing at the next floor.
            for (int floor = 0; floor < 30; floor += 6) {
                for (int step = 1; step <= 5; step++) {
                    BlockPos q = p(x-4,y+floor+step,z-4+step);
                    check(plan.get(q).getBlock() == Blocks.PLANKS, "missing step");
                    clear(plan, q.up()); clear(plan, q.up().up());
                }
                BlockPos landing = p(x-4,y+floor+6,z+2);
                check(plan.get(landing).getBlock() == Blocks.PLANKS, "missing landing");
                clear(plan, landing.up()); clear(plan, landing.up().up());
            }
            BlockPos boss = LargeWizardTower.bossFeet(x,y,z);
            check(plan.get(p(boss.getX(), boss.getY()-1, boss.getZ())).getBlock() == Blocks.PLANKS, "boss support");
            clear(plan, boss); clear(plan, boss.up()); clear(plan, boss.up().up());
            for (int floor = 0; floor < 5; floor++) {
                BlockPos guard = LargeWizardTower.guardFeet(x,y,z,floor);
                // Ground-floor support belongs to the generator, not this plan.
                if (floor > 0) check(plan.get(p(guard.getX(), guard.getY()-1, guard.getZ())).getBlock() == Blocks.PLANKS, "guard support");
                clear(plan, guard); clear(plan, guard.up());
            }
        }
        for (int invalid : new int[] {-1, 8}) {
            Map<BlockPos,IBlockState> plan = new LinkedHashMap<BlockPos,IBlockState>();
            try { LargeWizardTower.build(plan, 8, 64, 8, invalid); throw new AssertionError("invalid style accepted"); }
            catch (IllegalArgumentException expected) { check(plan.isEmpty(), "invalid plan modified"); }
        }
        System.out.println("PASS large tower plan: eight styles, envelope, stairs, chest, boss/guard spaces; old validator rejects it");
    }
}
