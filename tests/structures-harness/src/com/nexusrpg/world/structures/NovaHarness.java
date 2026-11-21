package com.nexusrpg.world.structures;
import java.util.*; import java.lang.reflect.*;
import com.nexusrpg.init.*; import net.minecraft.world.*; import net.minecraft.world.chunk.*; import net.minecraft.world.biome.*; import net.minecraft.block.state.*; import net.minecraft.init.Blocks; import net.minecraft.util.math.BlockPos; import net.minecraft.tileentity.*;
public class NovaHarness {
 static int[] first={0,7,14,20,26,32,38,44}, count={7,7,6,6,6,6,6,6};
 static Biome[] bs={ModBiomes.ENCHANTED_GROVE,ModBiomes.FROST_CROWN,ModBiomes.SUNKEN_EMPIRE,ModBiomes.WITCHING_MIRE,ModBiomes.CINDER_HEART,ModBiomes.ROSA_SANCTUM,ModBiomes.UMBRAL_REACH,ModBiomes.TITAN_CANOPY};
 static class Pick extends Random {int choice, calls;Pick(int c){choice=c;}public int nextInt(int n){if(calls++==0){ok(n==4,"candidate chance");return 0;}ok(choice<n,"choice bound");return choice;}public long nextLong(){return 42;}}
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void build(Map<BlockPos,IBlockState> p,int id,int style){if(id<=16)NovaStructuresA.build(p,8,64,8,id,style);else if(id<=33)NovaStructuresB.build(p,8,64,8,id,style);else NovaStructuresC.build(p,8,64,8,id,style);}
 static void generate(Chunk c, World w, int choice){new NexusStructureGenerator().generate(new Pick(choice),c.x,c.z,w,null,(x,z)->x==c.x&&z==c.z?c:null);}
 public static void main(String[] a)throws Exception {
  // Plants on a one-block higher natural ring do not obstruct walking, but used
  // to make every approach fail even though no part of the ring is modified.
  Chunk flowering=new Chunk(0,0,bs[0],64); World flowerWorld=new World(flowering);
  for(int x=4;x<=12;x++)for(int z=4;z<=12;z++) {
    if(x==4||x==12||z==4||z==12) {
      flowering.h[x][z]=65;
      flowering.overlay.put(new BlockPos(x,65,z),Blocks.RED_FLOWER.getDefaultState());
    }
  }
  generate(flowering,flowerWorld,1);
  ok(flowerWorld.getTileEntity(new BlockPos(8,65,8)) instanceof TileEntityChest,"passable flowers blocked every approach");
  ok(flowering.getBlockState(new BlockPos(4,65,8)).getBlock()==Blocks.RED_FLOWER,"outside flowers overwritten");
  Method valid=NexusStructureGenerator.class.getDeclaredMethod("validNewDecoration",Map.class,int.class,int.class,int.class);valid.setAccessible(true);
  Method approach=NexusStructureGenerator.class.getDeclaredMethod("hasChestApproach",Chunk.class,Map.class,int.class,int.class,int.class);approach.setAccessible(true);
  Set<Set<BlockPos>> all=new HashSet<>(); int min=10000,max=0,total=0;
  for(int style=0;style<8;style++)for(int k=0;k<count[style];k++){
    int id=first[style]+k; Map<BlockPos,IBlockState> p=new LinkedHashMap<>();build(p,id,style);
    ok((Boolean)valid.invoke(null,p,8,64,8),"validator rejected id "+id);
    ok(all.add(new HashSet<>(p.keySet())),"duplicate geometry id "+id);
    Chunk c=new Chunk(0,0,bs[style],64); ok((Boolean)approach.invoke(null,c,p,8,64,8),"no path id "+id);
    ok(!p.containsKey(new BlockPos(8,65,8))&&!p.containsKey(new BlockPos(8,66,8)),"chest clearance id "+id);
    World w=new World(c); generate(c,w,k+1);ok(w.getTileEntity(new BlockPos(8,65,8)) instanceof TileEntityChest,"generation id "+id+" writes "+w.writes.size());
    for(BlockPos pos:w.writes)ok((pos.getX()>>4)==0&&(pos.getZ()>>4)==0,"out of chunk id "+id);
    min=Math.min(min,p.size());max=Math.max(max,p.size());total+=p.size();
    System.out.println("PASS id="+id+" style="+style+" decoration="+p.size()+" writes="+w.writes.size());
  }
  Method old=NexusStructureGenerator.class.getDeclaredMethod("decorate",Map.class,int.class,int.class,int.class,int.class);old.setAccessible(true);
  for(int s=0;s<8;s++){
    Map<BlockPos,IBlockState> p=new LinkedHashMap<>();old.invoke(null,p,8,64,8,s);
    ok(all.add(new HashSet<>(p.keySet())),"duplicate geometry involving original style "+s);
    Chunk c=new Chunk(-2,3,bs[s],64);World w=new World(c);generate(c,w,0);
    ok(w.getTileEntity(new BlockPos(-24,65,56)) instanceof TileEntityChest,"original style "+s);
    int writes=w.writes.size();generate(c,w,0);ok(w.writes.size()==writes,"duplicate generation on same chunk "+s);
  }
  System.out.println("PASS 58 unique coordinate layouts; original 8; decoration min="+min+" max="+max+" total="+total);
  for(int[] p:new int[][]{{4,1,0},{-4,1,0},{0,1,4},{0,1,-4},{1,0,1},{1,11,1},{0,1,0},{0,2,0}}){Map<BlockPos,IBlockState> bad=new HashMap<>();bad.put(new BlockPos(8+p[0],64+p[1],8+p[2]),Blocks.STONEBRICK.getDefaultState());ok(!(Boolean)valid.invoke(null,bad,8,64,8),"bad bound");}
  for(net.minecraft.block.Block b:new net.minecraft.block.Block[]{Blocks.CHEST,Blocks.LAVA,Blocks.MAGMA,Blocks.SAND}){Map<BlockPos,IBlockState> bad=new HashMap<>();bad.put(new BlockPos(9,65,9),b.getDefaultState());ok(!(Boolean)valid.invoke(null,bad,8,64,8),"unsafe "+b);}
  ok(!(Boolean)valid.invoke(null,new HashMap<BlockPos,IBlockState>(),8,64,8),"empty");
  Map<BlockPos,IBlockState> wall=new HashMap<>();for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(Math.abs(x)==3||Math.abs(z)==3)wall.put(new BlockPos(8+x,65,8+z),Blocks.STONEBRICK.getDefaultState());
  ok(!(Boolean)approach.invoke(null,new Chunk(0,0,bs[0],64),wall,8,64,8),"sealed path");
  Chunk low=new Chunk(0,0,bs[0],60);for(int x=5;x<=11;x++)for(int z=5;z<=11;z++)low.h[x][z]=64;ok(!(Boolean)approach.invoke(null,low,new HashMap<BlockPos,IBlockState>(),8,64,8),"unreachable ledge");
  try{NovaStructuresC.build(new HashMap<BlockPos,IBlockState>(),8,64,8,50,5);throw new AssertionError("bad id");}catch(IllegalArgumentException expected){}try{NovaStructuresC.build(new HashMap<BlockPos,IBlockState>(),8,64,8,34,4);throw new AssertionError("bad style");}catch(IllegalArgumentException expected){}
  Chunk fail=new Chunk(0,0,bs[0],64); World fw=new World(fail);fw.failFloor=true;generate(fail,fw,1);ok(fw.getTileEntity(new BlockPos(8,65,8))==null,"failed write placed chest");
  Chunk saved=new Chunk(0,0,bs[0],64);saved.overlay.put(new BlockPos(5,30,5),Blocks.LOG.getDefaultState());World sw=new World(saved);generate(saved,sw,1);ok(sw.writes.isEmpty(),"modified terrain");
  Chunk disk=new Chunk(0,0,bs[0],64); World dw=new World(disk); NexusStructureGenerator dg=new NexusStructureGenerator(); dg.onChunkDataLoad(new net.minecraftforge.event.world.ChunkDataEvent.Load(dw,disk)); dg.generate(new Pick(1),0,0,dw,null,(x,z)->disk); ok(dw.writes.isEmpty(),"disk chunk");
  Chunk border=new Chunk(0,0,bs[0],64);World bw=new World(border);bw.border=false;generate(border,bw,1);ok(bw.writes.isEmpty(),"world border");
  // Candidate outcomes: 0 attempts and 1..3 skip; dimensions, client, biome, unloaded chunk.
  for(int roll=1;roll<4;roll++){
    final int r=roll;Chunk c=new Chunk(0,0,bs[0],64);World w=new World(c);
    new NexusStructureGenerator().generate(new Random(){public int nextInt(int n){ok(n==4,"wrong chance");return r;}},0,0,w,null,(x,z)->c);
    ok(w.writes.isEmpty(),"chance skip "+roll);
  }
  Chunk dim=new Chunk(0,0,bs[0],64);World dimWorld=new World(dim);dimWorld.provider.dim=-1;generate(dim,dimWorld,1);ok(dimWorld.writes.isEmpty(),"nether");
  dimWorld.provider.dim=1;generate(dim,dimWorld,1);ok(dimWorld.writes.isEmpty(),"end");
  dimWorld.provider.dim=0;dimWorld.isRemote=true;generate(dim,dimWorld,1);ok(dimWorld.writes.isEmpty(),"client side");
  Chunk other=new Chunk(0,0,new Biome(),64);World ow=new World(other);generate(other,ow,1);ok(ow.writes.isEmpty(),"non Nexus biome");
  Chunk unloaded=new Chunk(0,0,bs[0],64);World uw=new World(unloaded);
  new NexusStructureGenerator().generate(new Pick(1),0,0,uw,null,(x,z)->null);ok(uw.writes.isEmpty(),"unloaded chunk");
  Chunk built=new Chunk(0,0,bs[0],64); World builtWorld=new World(built);
  BlockPos chest=new BlockPos(5,64,5);built.overlay.put(chest,Blocks.CHEST.getDefaultState());built.tiles.put(chest,new TileEntityChest());
  generate(built,builtWorld,1);ok(builtWorld.writes.isEmpty()&&built.tiles.containsKey(chest),"existing chest");
  for(boolean overlap:new boolean[]{true,false}) {
    Chunk c=new Chunk(0,0,bs[0],64);World w=new World(c);
    net.minecraft.world.gen.structure.MapGenStructureData data=new net.minecraft.world.gen.structure.MapGenStructureData();
    net.minecraft.nbt.NBTTagCompound start=new net.minecraft.nbt.NBTTagCompound();
    start.put("BB",overlap?new int[]{2,0,2,15,100,15}:new int[]{100,0,100,120,100,120});
    data.features.put("start",start);w.storage.values.put("Village",data);generate(c,w,1);
    ok(overlap?w.writes.isEmpty():!w.writes.isEmpty(),"vanilla bounding box overlap="+overlap);
  }
  Chunk malformed=new Chunk(0,0,bs[0],64);World mw=new World(malformed);
  net.minecraft.world.gen.structure.MapGenStructureData data=new net.minecraft.world.gen.structure.MapGenStructureData();
  data.features.put("bad",new net.minecraft.nbt.NBTTagCompound());mw.storage.values.put("Village",data);
  generate(malformed,mw,1);ok(mw.writes.isEmpty(),"malformed bounds must fail closed");
  // A chest failure with no tile should undo our exact writes, including exceptions.
  for(String mode:new String[]{"falseBefore","throwBefore","falseNoTile","throwAfterNoTile"}){
    Chunk c=new Chunk(0,0,bs[0],64); World w=new World(c);w.chestFailure=mode;
    try { generate(c,w,1); if(mode.startsWith("throw")) throw new AssertionError("exception expected "+mode); }
    catch(RuntimeException e){ok(mode.startsWith("throw"),"unexpected exception "+mode);}
    for(int x=5;x<=11;x++)for(int z=5;z<=11;z++)for(int y=64;y<=74;y++)
      ok(c.getBlockState(new BlockPos(x,y,z)).getBlock()==Blocks.AIR,"incomplete rollback "+mode+" at "+x+","+y+","+z);
    ok(c.tiles.isEmpty(),"unexpected tile "+mode);
  }
  // A foreign tile-less chest appearing before our chest attempt is not ours to remove.
  Chunk fc=new Chunk(0,0,bs[0],64); World foreign=new World(fc);foreign.chestFailure="foreignBeforeChest";
  generate(fc,foreign,1);ok(fc.getBlockState(new BlockPos(8,65,8)).getBlock()==Blocks.CHEST,"removed foreign chest");
  // A chest with a tile must be left intact, even when the write reports failure/throws.
  for(String mode:new String[]{"falseWithTile","throwAfterTile","mutateFloor"}){
    Chunk c=new Chunk(0,0,bs[0],64);World w=new World(c);w.chestFailure=mode;w.mutation=new BlockPos(5,64,5);
    try{generate(c,w,1);if(mode.startsWith("throw"))throw new AssertionError("expected exception");}
    catch(RuntimeException e){ok(mode.startsWith("throw"),"unexpected exception");}
    ok(c.tiles.get(new BlockPos(8,65,8)) instanceof TileEntityChest,"lost tile "+mode);
    ok(c.getBlockState(new BlockPos(8,64,8)).getBlock()!=Blocks.AIR,"removed chest support "+mode);
    if(mode.equals("mutateFloor")){
      ok(c.getBlockState(w.mutation).getBlock()==Blocks.LOG,"overwrote third-party block");
      ok(((TileEntityChest)c.tiles.get(new BlockPos(8,65,8))).loot==null,"ignored callback mutation");
    }
  }
  System.out.println("PASS bounds/chest/unsafe/empty/sealed/ledge/invalid style-ID/write failure/non-natural/border");
  System.out.println("PASS chest failure/exception rollback and conservative preservation of tile and foreign blocks");
 }
}
