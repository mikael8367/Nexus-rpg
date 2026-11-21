package com.nexusrpg.world.structures;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.MapGenStructureData;

/** Simulated safety regression tests, not a Minecraft or Forge runtime. */
public class SurfaceSafetyHarness {
    static void ok(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static Chunk chunk() { return new Chunk(-2, 3, NovaHarness.bs[0], 64); }
    static BlockPos pos(int dx, int y, int dz) { return new BlockPos(-24 + dx, y, 56 + dz); }
    static void generate(Chunk c, World w, int choice) {
        new NexusStructureGenerator().generate(new NovaHarness.Pick(choice), c.x, c.z, w, null,
            (x,z) -> { ok(x == c.x && z == c.z, "queried neighboring chunk"); return c; });
    }
    static class MutatingWorld extends World {
        boolean changed;
        MutatingWorld(Chunk c) { super(c); }
        @Override public boolean setBlockState(BlockPos p, IBlockState state, int flags) {
            boolean result = super.setBlockState(p, state, flags);
            if (!changed) { changed = true; chunk.overlay.put(pos(1, 62, 1), Blocks.AIR.getDefaultState()); }
            return result;
        }
    }
    static class ChestMutatingWorld extends World {
        ChestMutatingWorld(Chunk c) { super(c); }
        @Override public boolean setBlockState(BlockPos p, IBlockState state, int flags) {
            boolean result = super.setBlockState(p, state, flags);
            if (state.getBlock() == Blocks.CHEST) chunk.overlay.put(pos(1, 62, 1), Blocks.AIR.getDefaultState());
            return result;
        }
    }
    public static void main(String[] args) {
        // Every shallow depth, edge and center, for an original, nova and tower.
        for (int choice : new int[] {0, 1, 8}) for (int depth = 2; depth <= 5; depth++)
            for (int[] offset : new int[][] {{-3,-3}, {0,0}, {3,3}})
                for (Block hole : new Block[] {Blocks.AIR, Blocks.WATER, Blocks.LAVA}) {
                    Chunk c=chunk(); World w=new World(c); BlockPos p=pos(offset[0],64-depth,offset[1]);
                    c.overlay.put(p,hole.getDefaultState()); generate(c,w,choice);
                    ok(w.writes.isEmpty() && c.getBlockState(p).getBlock()==hole,
                       "unsafe shallow foundation choice="+choice+" depth="+depth);
                }
        for (int choice : new int[] {0, 1, 8}) {
            Chunk c=chunk(); World w=new World(c);
            c.overlay.put(pos(0,58,0),Blocks.AIR.getDefaultState()); // Cave below the five-block check.
            generate(c,w,choice);
            ok(!w.writes.isEmpty(),"deep cave unnecessarily rejected " + choice);
            ok(c.getBlockState(pos(0,58,0)).getBlock()==Blocks.AIR,"deep cave overwritten");
        }
        Chunk changed=chunk(); MutatingWorld mw=new MutatingWorld(changed); generate(changed,mw,8);
        ok(changed.getBlockState(pos(1,62,1)).getBlock()==Blocks.AIR,"foreign foundation overwritten");
        ok(changed.tiles.isEmpty(),"chest placed after foundation mutation");
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) for(int y=64;y<=74;y++)
            ok(changed.getBlockState(pos(x,y,z)).getBlock()==Blocks.AIR,"rollback after foundation mutation");
        final int[] spawns={0}; WizardTower.setSpawnHandler((world,feet) -> spawns[0]++);
        Chunk chestChanged=chunk(); World chestWorld=new ChestMutatingWorld(chestChanged); generate(chestChanged,chestWorld,8);
        ok(chestChanged.getBlockState(pos(1,62,1)).getBlock()==Blocks.AIR,"overwrote chest-callback mutation");
        ok(chestChanged.tiles.containsKey(pos(0,65,0)),"deleted chest containing a tile");
        ok(spawns[0]==0,"spawned wizard after chest-callback foundation mutation");
        WizardTower.setSpawnHandler(null);
        for (int choice : new int[] {0,1,8}) {
            Chunk roof=chunk(); World rw=new World(roof);
            roof.overlay.put(pos(0,90,0),Blocks.STONE.getDefaultState());
            generate(roof,rw,choice); ok(rw.writes.isEmpty(),"buried below overhang " + choice);
        }
        Chunk invalid=chunk(); World iw=new World(invalid); MapGenStructureData data=new MapGenStructureData();
        NBTTagCompound start=new NBTTagCompound(); start.put("BB",new int[] {100,100,100,110,20,110});
        data.features.put("bad",start); iw.storage.values.put("Village",data); generate(invalid,iw,8);
        ok(iw.writes.isEmpty(),"inverted bounding-box height must fail closed");
        Chunk steep=chunk(); World sw=new World(steep); steep.h[5][5]=60; generate(steep,sw,8);
        ok(sw.writes.isEmpty(),"steep foundation accepted");
        Chunk ceiling=new Chunk(0,0,NovaHarness.bs[0],245); World cw=new World(ceiling); generate(ceiling,cw,8);
        ok(cw.writes.isEmpty(),"tower near world ceiling");
        System.out.println("PASS surface safety: 108 shallow cavity/fluid cases, 3 deep caves, callback rollback, malformed bounds, slope, ceiling and local-chunk queries");
    }
}
