package com.nexusrpg.world.structures;

import java.util.*;
import java.lang.reflect.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/** Simulated tests, not a Forge or in-game test. */
public class WizardTowerHarness {
    static void ok(boolean b, String m) { if (!b) throw new AssertionError(m); }
    static BlockPos p(int x,int y,int z) { return new BlockPos(x,y,z); }
    static void air(Map<BlockPos,IBlockState> plan, int x,int y,int z) {
        ok(!plan.containsKey(p(x,y,z)), "path obstructed at " + x + "," + y + "," + z);
    }
    public static void main(String[] args) throws Exception {
        Method valid = NexusStructureGenerator.class.getDeclaredMethod("validNewDecoration", Map.class,int.class,int.class,int.class);
        valid.setAccessible(true);
        Method approach = NexusStructureGenerator.class.getDeclaredMethod("hasChestApproach",Chunk.class,Map.class,int.class,int.class,int.class);
        approach.setAccessible(true);
        final int[] spawns = {0};
        WizardTower.setSpawnHandler((world,feet) -> {
            spawns[0]++;
            ok(world.getTileEntity(p((world.chunk.x<<4)+8,65,(world.chunk.z<<4)+8)) instanceof TileEntityChest,"called before chest");
            ok(((TileEntityChest)world.getTileEntity(p((world.chunk.x<<4)+8,65,(world.chunk.z<<4)+8))).loot != null,"called before loot");
            ok(feet.equals(WizardTower.spawnFeet((world.chunk.x<<4)+8,64,(world.chunk.z<<4)+8)),"wrong spawn position");
            ok(world.chunk.getBlockState(feet).getBlock()==Blocks.AIR && world.chunk.getBlockState(feet.up()).getBlock()==Blocks.AIR,"spawn blocked");
        });
        for (int style : new int[]{0,3,6}) {
            Map<BlockPos,IBlockState> plan = new LinkedHashMap<>();
            WizardTower.build(plan,8,64,8,style);
            ok((Boolean)valid.invoke(null,plan,8,64,8),"tower validator " + style);
            Chunk c = new Chunk(-2,3,NovaHarness.bs[style],64);
            Chunk local = new Chunk(0,0,NovaHarness.bs[style],64);
            ok((Boolean)approach.invoke(null,local,plan,8,64,8),"chest approach " + style);
            // Entrance, walk to the four jumpable stairs, upper gallery and spawn.
            for (int[] s : new int[][]{{0,-3,0},{0,-2,0},{1,-2,0},{1,-1,1},{2,-1,2},{2,0,3},{2,1,4},{1,1,4},{0,1,4},{-1,1,4}}) {
                int x=8+s[0],z=8+s[1],floor=64+s[2];
                if (s[2] > 0) ok(plan.containsKey(p(x,floor,z)),"missing step/gallery support");
                air(plan,x,floor+1,z); air(plan,x,floor+2,z);
            }
            air(plan,8,65,8); air(plan,8,66,8);
            World w = new World(c); int before = spawns[0];
            NovaHarness.generate(c,w,NovaHarness.count[style]+1);
            ok(spawns[0]==before+1,"no spawn callback " + style);
            ok(c.tiles.get(p(-24,65,56)) instanceof TileEntityChest,"no chest");
            for (BlockPos pos:w.writes) ok((pos.getX()>>4)==-2 && (pos.getZ()>>4)==3,"out of chunk");
            int writes=w.writes.size(); NovaHarness.generate(c,w,NovaHarness.count[style]+1);
            ok(w.writes.size()==writes && spawns[0]==before+1,"duplicate tower or spawn");
        }
        for (int style : new int[]{1,2,4,5,7}) ok(!WizardTower.supports(style),"invalid biome");
        try { WizardTower.build(new HashMap<BlockPos,IBlockState>(),8,64,8,1); throw new AssertionError("bad biome accepted"); }
        catch (IllegalArgumentException expected) { }
        for (String failure : new String[]{"falseBefore","falseNoTile","falseWithTile","mutateFloor"}) {
            Chunk c=new Chunk(0,0,NovaHarness.bs[0],64); World w=new World(c);
            w.chestFailure=failure; w.mutation=p(5,64,5); int before=spawns[0];
            NovaHarness.generate(c,w,8);
            ok(spawns[0]==before,"spawned after chest or floor failure " + failure);
        }
        Chunk occupied=new Chunk(0,0,NovaHarness.bs[0],64); World ow=new World(occupied);
        occupied.overlay.put(p(5,20,5),Blocks.CHEST.getDefaultState());
        occupied.tiles.put(p(5,20,5),new TileEntityChest()); int before=spawns[0];
        NovaHarness.generate(occupied,ow,8);
        ok(ow.writes.isEmpty() && occupied.tiles.containsKey(p(5,20,5)) && spawns[0]==before,"existing tile modified");
        Chunk high=new Chunk(0,0,NovaHarness.bs[0],245); World hw=new World(high);
        NovaHarness.generate(high,hw,8); ok(hw.writes.isEmpty() && spawns[0]==before,"world ceiling");
        WizardTower.setSpawnHandler(null);
        System.out.println("PASS wizard tower: 3 biomes, bounds, chest, loot, walkable stairs, gallery, callback, preservation and failed writes");
    }
}
