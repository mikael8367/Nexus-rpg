import java.util.*;
import com.nexusrpg.world.magma.MagmaArenaGenerator; import com.nexusrpg.init.ModBiomes;
import net.minecraft.init.Blocks; import net.minecraft.util.math.BlockPos; import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk; import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraft.nbt.NBTTagCompound; import net.minecraft.world.gen.structure.MapGenStructureData;
public class MagmaArenaHarness {
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 static long seed(int x,int z){for(long n=0;n<100000;n++)if(new Random(n ^ 0x4d41474d415f4152L ^ (long)x*341873128712L ^ (long)z*132897987541L).nextInt(40)==0)return n;throw new AssertionError();}
 static Chunk c(int x,int z){Chunk c=new Chunk(x,z,ModBiomes.MAGMA_WASTES,64);c.top=Blocks.NETHERRACK;return c;}
 static World w(Chunk c){World w=new World(c);w.seed=seed(c.x,c.z);return w;}
 static void run(MagmaArenaGenerator g,Chunk c,World w){g.generate(new Random(0),c.x,c.z,w,null,(x,z)->x==c.x&&z==c.z?c:null);}
 public static void main(String[] a){
  for(int[] xy:new int[][]{{0,0},{-1,-2},{300,-400}}){Chunk c=c(xy[0],xy[1]);World w=w(c);MagmaArenaGenerator g=new MagmaArenaGenerator();run(g,c,w);ok(w.writes.size()==28,"success writes "+Arrays.toString(xy));ok(w.spawned==1,"boss spawned exactly once");for(BlockPos p:w.writes)ok((p.getX()>>4)==c.x&&(p.getZ()>>4)==c.z,"write crosses chunk");run(g,c,w);ok(w.writes.size()==28,"repeat");}
  for(String mode:new String[]{"remote","dimension","features","biome","border","spawn","tree","building","tile","entity","saved","badFoundation","flower","writeFail","overlap","badStructure"}){
   Chunk c=c(0,0);World w=w(c);MagmaArenaGenerator g=new MagmaArenaGenerator();
   if(mode.equals("remote"))w.isRemote=true;if(mode.equals("dimension"))w.provider.dim=1;if(mode.equals("features"))w.mapFeatures=false;if(mode.equals("biome"))c.biome=ModBiomes.FROST_CROWN;if(mode.equals("border"))w.border=false;if(mode.equals("spawn"))w.spawn=new BlockPos(8,64,8);
   if(mode.equals("tree"))c.overlay.put(new BlockPos(4,65,4),Blocks.LOG.getDefaultState());if(mode.equals("building"))c.overlay.put(new BlockPos(4,40,4),Blocks.PLANKS.getDefaultState());if(mode.equals("tile"))c.tiles.put(new BlockPos(4,40,4),new net.minecraft.tileentity.TileEntityChest());if(mode.equals("entity"))w.entity=true;if(mode.equals("saved"))g.onChunkDataLoad(new ChunkDataEvent.Load(w,c));if(mode.equals("badFoundation"))c.overlay.put(new BlockPos(4,61,4),Blocks.AIR.getDefaultState());if(mode.equals("flower"))c.overlay.put(new BlockPos(4,64,4),Blocks.YELLOW_FLOWER.getDefaultState());if(mode.equals("writeFail"))w.failFloor=true;
   if(mode.equals("overlap")||mode.equals("badStructure")){MapGenStructureData d=new MapGenStructureData();NBTTagCompound t=new NBTTagCompound();if(mode.equals("overlap"))t.put("BB",new int[]{0,0,0,15,255,15});d.features.put("test",t);w.storage.data.put("Village",d);}
   run(g,c,w);ok(w.writes.isEmpty()||mode.equals("writeFail"),mode+" unexpectedly wrote "+w.writes.size());
  }
  for(String mode:new String[]{"cancel","collision","liquid"}){Chunk c=c(0,0);World w=w(c);if(mode.equals("cancel"))w.spawnResult=false;if(mode.equals("collision"))w.collision=true;if(mode.equals("liquid"))w.liquid=true;run(new MagmaArenaGenerator(),c,w);ok(w.spawned==0,mode+" spawned");ok(w.writes.size()==56,mode+" rollback");}
  {Chunk c=c(0,0);World w=w(c);w.chestFailure="undermineBeforeChest";w.mutation=new BlockPos(3,60,3);run(new MagmaArenaGenerator(),c,w);ok(w.writes.size()==56,"partial write rollback");for(int x=3;x<=13;x++)for(int z=3;z<=13;z++)for(int y=64;y<=66;y++)ok(c.getBlockState(new BlockPos(x,y,z)).getBlock()==Blocks.AIR,"rollback air");}
  {Chunk c=c(0,0);World w=new World(c){public boolean setBlockState(BlockPos p,net.minecraft.block.state.IBlockState s,int f){boolean result=super.setBlockState(p,s,f);if(s.getBlock()==Blocks.OBSIDIAN){chunk.overlay.put(p,Blocks.LOG.getDefaultState());}return result;}};w.seed=seed(0,0);run(new MagmaArenaGenerator(),c,w);ok(w.writes.size()==1,"foreign mutation stop");ok(c.getBlockState(w.writes.get(0)).getBlock()==Blocks.LOG,"foreign block preserved");}
  System.out.println("PASS: 3 chunk coords, repeat, 16 guards, rollback and foreign-block preservation (mock world; no Forge runtime)");
 }
}