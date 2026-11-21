package com.nexusrpg.world.structures;

import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** Geometry only, stubs instead of Minecraft. */
public class LargeTowerHarness {
    private static void ok(boolean condition, String msg) { if (!condition) throw new AssertionError(msg); }
    private static BlockPos pos(int x, int y, int z) { return new BlockPos(x,y,z); }
    public static void main(String[] args) {
        Set<BlockPos> previous = null;
        for (int style=0; style<8; style++) {
            Map<BlockPos,IBlockState> p = new LinkedHashMap<>();
            LargeWizardTower.build(p,8,64,8,style);
            ok(previous == null || previous.equals(p.keySet()), "geometry differed"); previous = new HashSet<>(p.keySet());
            for (BlockPos b:p.keySet())
                ok(Math.abs(b.getX()-8)<=5 && Math.abs(b.getZ()-8)<=5 && b.getY()>=65 && b.getY()<=101, "out of bounds");
            BlockPos chest = LargeWizardTower.chest(8,64,8);
            ok(!p.containsKey(chest) && !p.containsKey(chest.up()), "chest blocked");
            BlockPos boss = LargeWizardTower.bossFeet(8,64,8);
            ok(p.get(pos(boss.getX(),boss.getY()-1,boss.getZ())).getBlock()==Blocks.PLANKS &&
               !p.containsKey(boss) && !p.containsKey(boss.up()), "boss blocked");
            for (int f=0;f<5;f++) {
                BlockPos g=LargeWizardTower.guardFeet(8,64,8,f);
                ok((f==0 || p.get(pos(g.getX(),g.getY()-1,g.getZ())).getBlock()==Blocks.PLANKS) &&
                    !p.containsKey(g) && !p.containsKey(g.up()), "guard blocked");
                int base = 64+f*6;
                // Room from lower landing, five steps and upper landing, including jump clearance.
                for (int i=0;i<=6;i++) {
                    int z=8-4+i, y=base+i, x=4;
                    if (i>0) ok(p.get(pos(x,y,z)).getBlock()==Blocks.PLANKS,"missing step");
                    for(int h=1;h<=3;h++) ok(!p.containsKey(pos(x,y+h,z)), "stair headroom f="+f+" step="+i+" h="+h);
                }
                // Walk around to the next staircase on the next level.
                for (int z=-4;z<=3;z++) {
                    ok(p.get(pos(5,base+6,8+z)).getBlock()==Blocks.PLANKS,"landing floor");
                    ok(!p.containsKey(pos(5,base+7,8+z)) && !p.containsKey(pos(5,base+8,8+z)),"landing obstructed");
                }
            }
            for(int x=-1;x<=1;x++) for(int y=1;y<=2;y++) ok(!p.containsKey(pos(8+x,64+y,3)),"door");
        }
        for(int style:new int[]{-1,8,Integer.MAX_VALUE,Integer.MIN_VALUE}) {
            Map<BlockPos,IBlockState> p=new HashMap<>();
            try { LargeWizardTower.build(p,8,64,8,style); throw new AssertionError("invalid style"); }
            catch(IllegalArgumentException expected) { ok(p.isEmpty(),"invalid mutated plan"); }
        }
        System.out.println("PASS large tower plan: 8 palettes, bounds, 6 floors, 5 stairs and headroom, chest/guard/boss positions, entrance");
    }
}
