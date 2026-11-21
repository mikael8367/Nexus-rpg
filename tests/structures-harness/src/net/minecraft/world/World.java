package net.minecraft.world;
import java.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

// Deliberately small fake, not an implementation of Minecraft/Forge.
public class World {
 public boolean isRemote, border=true, failFloor=false;
 public Provider provider=new Provider(); public Chunk chunk; public int height=256;
 public List<BlockPos> writes=new ArrayList<>();
 public final List<net.minecraft.entity.Entity> loadedEntityList=new ArrayList<>();
 public String chestFailure=""; public BlockPos mutation;
 public World(Chunk c){chunk=c;}
 public static class Provider {public int dim=0; public int getDimension(){return dim;}}
 public class Border {public boolean contains(BlockPos p){return border;}}
 public Border getWorldBorder(){return new Border();}
 public Object getBiomeProvider(){return null;}
 public int getHeight(){return height;}
 public static class Storage {public final Map<String,Object> values=new HashMap<>(); public Object getOrLoadData(Class<?> c,String name){return values.get(name);}}
 public Storage storage=new Storage();
 public Storage getPerWorldStorage(){return storage;}
 public boolean setBlockState(BlockPos p,IBlockState s,int flags){
  writes.add(p);
  if(failFloor&&s.getBlock()!=Blocks.CHEST)return false;
  if(s.getBlock()==Blocks.CHEST){
   if(chestFailure.equals("throwBefore")) throw new RuntimeException("before chest");
   if(chestFailure.equals("falseBefore")) return false;
   chunk.overlay.put(p,s);
   if(chestFailure.equals("throwAfterNoTile")) throw new RuntimeException("after chest without tile");
   if(chestFailure.equals("falseNoTile")) return false;
   chunk.tiles.put(p,new TileEntityChest());
   if(chestFailure.equals("mutateFloor")) chunk.overlay.put(mutation,Blocks.LOG.getDefaultState());
   if(chestFailure.equals("throwAfterTile")) throw new RuntimeException("after chest with tile");
   if(chestFailure.equals("falseWithTile")) return false;
   return true;
  }
  chunk.overlay.put(p,s);
  if(chestFailure.equals("foreignBeforeChest")) {
   chestFailure="";
   chunk.overlay.put(new BlockPos((chunk.x << 4)+8,65,(chunk.z << 4)+8),Blocks.CHEST.getDefaultState());
  }
  return true;
 }
 public TileEntity getTileEntity(BlockPos p){return chunk.tiles.get(p);}
 public boolean setBlockToAir(BlockPos p){return setBlockState(p,Blocks.AIR.getDefaultState(),2);}
}
