package com.nexusrpg.world.structures;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/** Simulates already-loaded entities and block callbacks; not a real Minecraft runtime. */
public class EntitySafetyHarness {
 static void ok(boolean v,String s){if(!v)throw new AssertionError(s);}
 static Chunk chunk(){return new Chunk(-2,3,NovaHarness.bs[0],64);}
 static AxisAlignedBB box(double x1,double y1,double z1,double x2,double y2,double z2){return new AxisAlignedBB(x1,y1,z1,x2,y2,z2);}
 static void generate(Chunk c,World w,int choice){SurfaceSafetyHarness.generate(c,w,choice);}
 static final AxisAlignedBB INSIDE=box(-24.3,64.2,55.7,-23.7,66,56.3);
 static void reject(Entity e,int choice){
  Chunk c=chunk(); World w=new World(c); w.loadedEntityList.add(e);
  Map<BlockPos,IBlockState> before=new HashMap<>(c.overlay);
  generate(c,w,choice);
  ok(w.writes.isEmpty() && c.overlay.equals(before) && c.tiles.isEmpty(),"entity modified blocks, choice="+choice);
 }
 static class SpawnDuringWrite extends World {
  boolean injected;
  SpawnDuringWrite(Chunk c){super(c);}
  @Override public boolean setBlockState(BlockPos p,IBlockState s,int flags){
   boolean result=super.setBlockState(p,s,flags);
   if(!injected && s.getBlock()!=Blocks.CHEST){injected=true;loadedEntityList.add(new EntityLivingBase(INSIDE,false));}
   return result;
  }
 }
 static class SpawnDuringChest extends World {
  SpawnDuringChest(Chunk c){super(c);}
  @Override public boolean setBlockState(BlockPos p,IBlockState s,int flags){
   boolean result=super.setBlockState(p,s,flags);
   if(s.getBlock()==Blocks.CHEST) loadedEntityList.add(new EntityLivingBase(INSIDE,false));
   return result;
  }
 }
 public static void main(String[] args){
  for(int choice:new int[]{0,1,8}) {
   reject(new EntityLivingBase(INSIDE,true),choice); // mob/player-like living entity
   reject(new EntityLivingBase(INSIDE,false),choice); // living even if non-collidable
   reject(new Entity(INSIDE,true),choice); // vehicle or other collidable entity
   reject(new EntityLivingBase(box(-28,64,55,-26.5,66,56),true),choice); // box crosses footprint edge
   Chunk empty=chunk(); World ew=new World(empty); generate(empty,ew,choice);
   ok(ew.chunk.tiles.containsKey(SurfaceSafetyHarness.pos(0,65,0)),"empty world failed choice "+choice);
   Chunk item=chunk(); World iw=new World(item); iw.loadedEntityList.add(new Entity(INSIDE,false)); generate(item,iw,choice);
   ok(item.tiles.containsKey(SurfaceSafetyHarness.pos(0,65,0)),"noncollidable item blocked choice "+choice);
   Chunk outside=chunk(); World ow=new World(outside); ow.loadedEntityList.add(new EntityLivingBase(box(-30,64,55,-27,66,56),true)); generate(outside,ow,choice);
   ok(outside.tiles.containsKey(SurfaceSafetyHarness.pos(0,65,0)),"touching but outside box blocked choice "+choice);
   Chunk dead=chunk(); World dw=new World(dead); Entity gone=new EntityLivingBase(INSIDE,true); gone.isDead=true; dw.loadedEntityList.add(gone); generate(dead,dw,choice);
   ok(dead.tiles.containsKey(SurfaceSafetyHarness.pos(0,65,0)),"dead entity blocked choice "+choice);
   Chunk chest=chunk(); SpawnDuringChest cw=new SpawnDuringChest(chest); generate(chest,cw,choice);
   net.minecraft.tileentity.TileEntityChest tile=(net.minecraft.tileentity.TileEntityChest)chest.tiles.get(SurfaceSafetyHarness.pos(0,65,0));
   ok(tile!=null && tile.loot==null && chest.getBlockState(SurfaceSafetyHarness.pos(0,64,0)).getBlock()!=Blocks.AIR,"chest callback lost data/support or assigned loot");
   Chunk late=chunk(); SpawnDuringWrite lw=new SpawnDuringWrite(late); generate(late,lw,choice);
   ok(lw.injected && late.tiles.isEmpty(),"spawn callback allowed chest");
   for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) for(int y=64;y<=74;y++)
    ok(late.getBlockState(SurfaceSafetyHarness.pos(x,y,z)).getBlock()==Blocks.AIR,"spawn callback rollback");
  }
  for(net.minecraft.block.Block plant:new net.minecraft.block.Block[]{Blocks.TALLGRASS,Blocks.RED_FLOWER,Blocks.BROWN_MUSHROOM}){
   Chunk c=chunk(); World w=new World(c); BlockPos p=SurfaceSafetyHarness.pos(3,64,3); c.overlay.put(p,plant.getDefaultState());
   Map<BlockPos,IBlockState> before=new HashMap<>(c.overlay); generate(c,w,0);
   ok(w.writes.isEmpty() && c.overlay.equals(before) && c.tiles.isEmpty(),"vegetation overwritten");
  }
  // Vanilla adds snow before GameRegistry.generateWorld. Snow must not prevent
  // the original, nova or tower in the cold biome; it is not vegetation.
  for(int choice:new int[]{0,1,8}) {
   Chunk snowy=new Chunk(-2,3,NovaHarness.bs[1],64);
   for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++)
    snowy.overlay.put(SurfaceSafetyHarness.pos(x,64,z),Blocks.SNOW_LAYER.getDefaultState());
   World sw=new World(snowy); generate(snowy,sw,choice);
   ok(snowy.tiles.containsKey(SurfaceSafetyHarness.pos(0,65,0)),"snow blocked frost structure choice="+choice);
   Chunk interrupted=new Chunk(-2,3,NovaHarness.bs[1],64);
   for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++)
    interrupted.overlay.put(SurfaceSafetyHarness.pos(x,64,z),Blocks.SNOW_LAYER.getDefaultState());
   Map<BlockPos,IBlockState> before=new HashMap<>(interrupted.overlay);
   SpawnDuringWrite iw=new SpawnDuringWrite(interrupted); generate(interrupted,iw,choice);
   for(Map.Entry<BlockPos,IBlockState> e:before.entrySet())
    ok(interrupted.getBlockState(e.getKey()).equals(e.getValue()),"snow not restored after callback");
   ok(iw.injected && interrupted.tiles.isEmpty(),"snowy callback allowed chest");
  }
  System.out.println("PASS entity occupancy, bounding-box edges, noncolliding items, vegetation, frost snow and callback rollback for original/nova/tower");
 }
}
