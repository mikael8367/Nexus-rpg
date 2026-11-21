package com.nexusrpg.dragons.world;
import java.util.*;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.ChunkDataEvent;

/** Simulated tests; does not establish Minecraft or other-mod compatibility. */
public class DragonNestHarness {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static long seed(int style){for(long n=0;n<100000;n++){Random r=new Random(n^0x445241474e455354L);if(r.nextInt(120)==0&&r.nextInt(4)==style)return n;}throw new AssertionError();}
 static Chunk c(){return new Chunk(0,0,null,64);}
 static World w(Chunk c,int style){World w=new World(c);w.seed=seed(style);return w;}
 static void run(DragonNestGenerator g,Chunk c,World w){IChunkProvider p=(x,z)->{if(x!=0||z!=0)throw new AssertionError("neighbor loaded");return c;};g.generate(new Random(),0,0,w,null,p);}
 static void clean(Chunk c){for(int x=1;x<=15;x++)for(int z=1;z<=15;z++)for(int y=64;y<=74;y++)ok(c.getBlockState(new BlockPos(x,y,z)).getBlock()==Blocks.AIR,"rollback "+x+","+y+","+z);}
 public static void main(String[] args){
  String[] types={"EntityFireDragon","EntityWaterDragon","EntityWindDragon","EntityEarthDragon"};
  for(int i=0;i<4;i++){
   Chunk c=c();World w=w(c,i);DragonNestGenerator g=new DragonNestGenerator();run(g,c,w);
   ok(w.spawns==1 && w.loadedEntityList.size()==1,"spawn style "+i);
   ok(w.last.getClass().getSimpleName().equals(types[i]),"element "+i);ok(w.last.persistent,"persistent");
   int minX=100,minZ=100,maxX=0,maxZ=0,maxY=0;
   for(BlockPos p:w.writes){ok((p.getX()>>4)==0&&(p.getZ()>>4)==0,"write escaped chunk");minX=Math.min(minX,p.getX());maxX=Math.max(maxX,p.getX());minZ=Math.min(minZ,p.getZ());maxZ=Math.max(maxZ,p.getZ());maxY=Math.max(maxY,p.getY());}
   ok(maxX-minX+1==15&&maxZ-minZ+1==15,"dimensions");ok(maxY-64+1==new int[]{7,5,9,6}[i],"height");int count=w.writes.size();run(g,c,w);ok(w.spawns==1&&w.writes.size()==count,"duplicate");
  }
  for(String mode:new String[]{"remote","dimension","features","border","tree","building","tile","entity","collision","liquid","saved","foundation","flower","spawnCancel","writeFail","foreignPlan","slope","throwSpawn","addedFalse","failAfterWrite","throwAfterWrite","tileAfterWrite","structure","invalidStructure","obstacleDuringWrite","obstacleDuringCollision"}){
   Chunk c=c();World w=w(c,0);DragonNestGenerator g=new DragonNestGenerator();
   if(mode.equals("remote"))w.isRemote=true;if(mode.equals("dimension"))w.provider.dim=1;if(mode.equals("features"))w.mapFeatures=false;if(mode.equals("border"))w.border=false;
   if(mode.equals("tree"))c.overlay.put(new BlockPos(3,64,3),Blocks.LOG.getDefaultState());
   if(mode.equals("building"))c.overlay.put(new BlockPos(3,40,3),Blocks.PLANKS.getDefaultState());
   if(mode.equals("tile"))c.tiles.put(new BlockPos(3,40,3),new net.minecraft.tileentity.TileEntityChest());
   if(mode.equals("entity"))w.loadedEntityList.add(new net.minecraft.entity.Entity(){ {box=new net.minecraft.util.math.AxisAlignedBB(7,64,7,8,66,8);} });
   if(mode.equals("collision"))w.collision=true;if(mode.equals("liquid"))w.liquid=true;
   if(mode.equals("saved"))g.onChunkDataLoad(new ChunkDataEvent.Load(w,c));
   if(mode.equals("foundation"))c.overlay.put(new BlockPos(3,61,3),Blocks.AIR.getDefaultState());
   if(mode.equals("flower"))c.overlay.put(new BlockPos(3,64,3),Blocks.YELLOW_FLOWER.getDefaultState());
   if(mode.equals("spawnCancel"))w.spawnResult=false;if(mode.equals("writeFail"))w.failFloor=true;
   if(mode.equals("foreignPlan")){w.chestFailure="foreignPlan";w.mutation=new BlockPos(3,64,4);}
   if(mode.equals("obstacleDuringWrite")||mode.equals("obstacleDuringCollision")){w.chestFailure=mode;w.mutation=new BlockPos(8,70,8);}
   if(mode.equals("slope"))c.h[3][3]=65;
   if(mode.equals("throwSpawn"))w.throwSpawn=true;
   if(mode.equals("addedFalse")){w.spawnResult=false;w.addThenFalse=true;}
   if(mode.equals("failAfterWrite")||mode.equals("throwAfterWrite")||mode.equals("tileAfterWrite"))w.chestFailure=mode;
   if(mode.equals("structure")||mode.equals("invalidStructure")){
    net.minecraft.world.gen.structure.MapGenStructureData data=new net.minecraft.world.gen.structure.MapGenStructureData();
    net.minecraft.nbt.NBTTagCompound feature=new net.minecraft.nbt.NBTTagCompound();
    if(mode.equals("structure"))feature.entries.put("BB",new int[]{-5,0,-5,20,100,20});
    data.tags.entries.put("feature",feature);w.storage.data.put("Village",data);
   }
   run(g,c,w);
   boolean spawn=mode.equals("spawnCancel")||mode.equals("throwSpawn")||mode.equals("addedFalse");ok(w.spawns==(spawn?1:0),mode+" spawn");
   if(mode.equals("foreignPlan"))ok(c.getBlockState(w.mutation).getBlock()==Blocks.LOG,"foreign preserved");
   if(mode.equals("obstacleDuringWrite")||mode.equals("obstacleDuringCollision")){
    ok(c.getBlockState(w.mutation).getBlock()==Blocks.LOG,mode+" preserves foreign obstacle");
    for(int x=1;x<=15;x++)for(int z=1;z<=15;z++)for(int y=64;y<=74;y++){
     BlockPos p=new BlockPos(x,y,z); if(!p.equals(w.mutation))ok(c.getBlockState(p).getBlock()==Blocks.AIR,mode+" rollback "+x+","+y+","+z);
    }
   }
   if(mode.equals("spawnCancel")||mode.equals("collision")||mode.equals("liquid")||mode.equals("writeFail")||mode.equals("failAfterWrite")||mode.equals("throwAfterWrite"))clean(c);
   if(mode.equals("tileAfterWrite"))ok(!c.tiles.isEmpty() && c.getBlockState(c.tiles.keySet().iterator().next()).getBlock()!=Blocks.AIR,"foreign tile preserved");
   if(mode.equals("throwSpawn")||mode.equals("addedFalse"))ok(c.getBlockState(new BlockPos(3,64,4)).getBlock()!=Blocks.AIR,mode+" preserve");
  }
  System.out.println("PASS: four variants, geometry, spawn/repeat, 26 safety/failure cases (simulated)");
 }
}
