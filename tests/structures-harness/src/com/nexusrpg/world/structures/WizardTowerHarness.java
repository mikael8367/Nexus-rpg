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
        net.minecraft.block.Block[] walls = {Blocks.STONEBRICK, Blocks.QUARTZ_BLOCK, Blocks.SANDSTONE,
            Blocks.MOSSY_COBBLESTONE, Blocks.NETHER_BRICK, Blocks.QUARTZ_BLOCK, Blocks.END_BRICKS, Blocks.LOG};
        net.minecraft.block.Block[] accents = {Blocks.COBBLESTONE, Blocks.PRISMARINE, Blocks.BRICK_BLOCK,
            Blocks.COBBLESTONE, Blocks.RED_NETHER_BRICK, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN, Blocks.MOSSY_COBBLESTONE};
        Set<BlockPos> commonGeometry = null;
        for (int style = 0; style < 8; style++) {
            ok(WizardTower.supports(style), "unsupported biome " + style);
            Map<BlockPos,IBlockState> plan = new LinkedHashMap<>();
            WizardTower.build(plan,8,64,8,style);
            ok(plan.get(p(5,65,7)).getBlock()==walls[style], "wrong wall " + style);
            ok(plan.get(p(5,65,5)).getBlock()==accents[style], "wrong accent " + style);
            ok(plan.get(p(5,67,8)).getBlock()==Blocks.GLASS_PANE, "wrong window " + style);
            if (commonGeometry == null) commonGeometry = new HashSet<>(plan.keySet());
            else ok(commonGeometry.equals(plan.keySet()), "geometry changed " + style);
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
            for (Map.Entry<BlockPos,IBlockState> entry : plan.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockPos actual = p(pos.getX()-32,pos.getY(),pos.getZ()+48);
                ok(c.getBlockState(actual).equals(entry.getValue()), "wrong generated block " + style + " at " + actual);
            }
            for (BlockPos pos:w.writes) ok((pos.getX()>>4)==-2 && (pos.getZ()>>4)==3,"out of chunk");
            int writes=w.writes.size(); NovaHarness.generate(c,w,NovaHarness.count[style]+1);
            ok(w.writes.size()==writes && spawns[0]==before+1,"duplicate tower or spawn");
        }
        for (int invalid : new int[]{Integer.MIN_VALUE, -1, 8, Integer.MAX_VALUE}) {
            ok(!WizardTower.supports(invalid), "invalid biome accepted " + invalid);
            Map<BlockPos,IBlockState> untouched = new HashMap<>();
            try { WizardTower.build(untouched,8,64,8,invalid); throw new AssertionError("bad biome accepted"); }
            catch (IllegalArgumentException expected) { ok(untouched.isEmpty(), "invalid style changed plan"); }
        }
        for (int style = 0; style < 8; style++) {
            for (String failure : new String[]{"falseBefore","falseNoTile","falseWithTile","mutateFloor", "throwBefore", "throwAfterNoTile", "throwAfterTile"}) {
                Chunk c=new Chunk(0,0,NovaHarness.bs[style],64); World w=new World(c);
                // The gallery is always PLANKS; a LOG mutation must differ in every biome.
                w.chestFailure=failure; w.mutation=p(9,68,9); int before=spawns[0];
                try {
                    NovaHarness.generate(c,w,NovaHarness.count[style]+1);
                    ok(!failure.startsWith("throw"), "expected exception " + failure);
                } catch (RuntimeException e) { ok(failure.startsWith("throw"), "unexpected exception " + failure); }
                ok(spawns[0]==before,"spawned after chest or floor failure " + style + " " + failure);
                if (failure.equals("falseWithTile") || failure.equals("throwAfterTile") || failure.equals("mutateFloor"))
                    ok(c.tiles.get(p(8,65,8)) instanceof TileEntityChest, "removed chest tile " + style + " " + failure);
                else {
                    ok(c.tiles.isEmpty(), "unexpected chest tile " + style + " " + failure);
                    for (int x=5; x<=11; x++) for (int z=5; z<=11; z++) for (int y=64; y<=74; y++)
                        ok(c.getBlockState(p(x,y,z)).getBlock()==Blocks.AIR, "incomplete rollback " + style + " " + failure);
                }
                if (failure.equals("mutateFloor")) {
                    ok(c.getBlockState(w.mutation).getBlock()==Blocks.LOG, "overwrote foreign block");
                    ok(((TileEntityChest)c.tiles.get(p(8,65,8))).loot==null, "assigned loot after mutation");
                }
            }
            Chunk unsafe=new Chunk(0,0,NovaHarness.bs[style],64); World uw=new World(unsafe);
            unsafe.overlay.put(p(5,62,5),Blocks.AIR.getDefaultState()); int before=spawns[0];
            NovaHarness.generate(unsafe,uw,NovaHarness.count[style]+1);
            ok(uw.writes.isEmpty() && spawns[0]==before,"unsafe foundation accepted " + style);
        }
        Chunk occupied=new Chunk(0,0,NovaHarness.bs[0],64); World ow=new World(occupied);
        occupied.overlay.put(p(5,20,5),Blocks.CHEST.getDefaultState());
        occupied.tiles.put(p(5,20,5),new TileEntityChest()); int before=spawns[0];
        NovaHarness.generate(occupied,ow,8);
        ok(ow.writes.isEmpty() && occupied.tiles.containsKey(p(5,20,5)) && spawns[0]==before,"existing tile modified");
        Chunk high=new Chunk(0,0,NovaHarness.bs[0],245); World hw=new World(high);
        NovaHarness.generate(high,hw,8); ok(hw.writes.isEmpty() && spawns[0]==before,"world ceiling");
        WizardTower.setSpawnHandler(null);
        System.out.println("PASS wizard tower: 8 biomes and palettes, bounds, chest, loot, walkable stairs, gallery, callback, preservation and failed writes");
    }
}
