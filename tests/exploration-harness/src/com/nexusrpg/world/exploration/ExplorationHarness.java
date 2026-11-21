package com.nexusrpg.world.exploration;
import java.util.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.math.BlockPos;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.event.world.ChunkDataEvent;
public class ExplorationHarness {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 static long seed(int type){for(long i=0;i<100000;i++){Random r=new Random(i ^ 0x6e65787573524750L);if(r.nextInt(180)==0&&r.nextInt(3)==type)return i;}throw new AssertionError();}
 static void run(ExplorationGenerator g, Chunk c, World w){g.generate(new Random(1),c.x,c.z,w,null,(x,z)->x==c.x&&z==c.z?c:null);}
 // Enumerate the choices, not just Java Random seeds, so rare doorway combinations are covered.
 static final class Choices extends Random {
  final int[] values; int i;
  Choices(int... values){this.values=values;}
  @Override public int nextInt(int bound){
   ok(i<values.length && values[i]>=0 && values[i]<bound,"invalid scripted random choice");
   return values[i++];
  }
 }
 static void assertDungeonConnected(Map<BlockPos,IBlockState> p, String label){
  Set<String> seen=new HashSet<>(); ArrayDeque<int[]> q=new ArrayDeque<>(); q.add(new int[]{7,3});
  while(!q.isEmpty()){
   int[] a=q.remove(); int x=a[0],z=a[1]; String k=x+","+z;
   if(x<3||x>12||z<3||z>12||seen.contains(k)||
      p.containsKey(new BlockPos(x,65,z))||p.containsKey(new BlockPos(x,66,z))||(x==8&&z==10))continue;
   seen.add(k); q.add(new int[]{x+1,z}); q.add(new int[]{x-1,z}); q.add(new int[]{x,z+1}); q.add(new int[]{x,z-1});
  }
  for(int x=4;x<=11;x++)for(int z=4;z<=11;z++){
   if(x==8&&z==10)continue;
   if(!p.containsKey(new BlockPos(x,65,z))&&!p.containsKey(new BlockPos(x,66,z)))
    ok(seen.contains(x+","+z),"unreachable "+label+" "+x+","+z);
  }
  ok(seen.contains("8,9")||seen.contains("7,10")||seen.contains("9,10")||seen.contains("8,11"),"inaccessible chest "+label);
 }
 public static void main(String[] args) throws Exception {
  ok(ExplorationRegistration.class.getAnnotation(net.minecraftforge.fml.common.Mod.EventBusSubscriber.class).modid().equals("nexusrpg"),"subscriber modid");
  ok(ExplorationRegistration.class.getMethod("register",net.minecraftforge.event.RegistryEvent.Register.class).isAnnotationPresent(net.minecraftforge.fml.common.eventhandler.SubscribeEvent.class),"missing SubscribeEvent");
  ExplorationRegistration.register(new net.minecraftforge.event.RegistryEvent.Register<net.minecraft.block.Block>());
  ExplorationRegistration.register(new net.minecraftforge.event.RegistryEvent.Register<net.minecraft.block.Block>());
  ok(net.minecraftforge.fml.common.registry.GameRegistry.generators.size()==1,"registration must be once");
  ok(net.minecraftforge.fml.common.registry.GameRegistry.weight==2,"registration weight");
  for(int type=0;type<3;type++)for(int n=0;n<300;n++){
   Map<BlockPos,IBlockState> p=ExplorationGenerator.plan(0,64,0,type,new Random(n));
   ok(p.size()<=800,"size");
   for(BlockPos q:p.keySet())ok(q.getX()>=3&&q.getX()<=12&&q.getZ()>=3&&q.getZ()<=12&&q.getY()>=64&&q.getY()<=72,"bounds");
   BlockPos chest=new BlockPos(8,65,10);ok(!p.containsKey(chest)&&!p.containsKey(chest.up()),"chest clearance");
   Set<String> seen=new HashSet<>();ArrayDeque<int[]> queue=new ArrayDeque<>();queue.add(new int[]{7,3});
   while(!queue.isEmpty()){int[] a=queue.remove();int x=a[0],z=a[1];String key=x+","+z;if(x<3||x>12||z<3||z>12||seen.contains(key))continue;
    if(p.containsKey(new BlockPos(x,65,z))||p.containsKey(new BlockPos(x,66,z))||(x==8&&z==10))continue;
    seen.add(key);
    queue.add(new int[]{x+1,z});queue.add(new int[]{x-1,z});queue.add(new int[]{x,z+1});queue.add(new int[]{x,z-1});
   }
   ok(seen.contains("8,9")||seen.contains("7,10")||seen.contains("9,10")||seen.contains("8,11"),"chest unreachable type="+type+" seed="+n);
   if(type==0){
    // Every walkable cell must be reachable, not merely a face of the chest.
    for(int x=4;x<=11;x++)for(int z=4;z<=11;z++){
     if(x==8&&z==10)continue;
     if(!p.containsKey(new BlockPos(x,65,z))&&!p.containsKey(new BlockPos(x,66,z)))
      ok(seen.contains(x+","+z),"unreachable dungeon cell "+n+" "+x+","+z);
     ok(p.containsKey(new BlockPos(x,68,z)),"missing roof");
    }
    for(int z:new int[]{6,9}){
     int doors=0;for(int x=4;x<=11;x++)if(!p.containsKey(new BlockPos(x,65,z)))doors++;
     ok(doors==1,"crosswall must have one door");
    }
    ok(p.equals(ExplorationGenerator.plan(0,64,0,type,new Random(n))),"nondeterministic layout");
   }
  }
  int exhaustive=0;
  for(int divider=0;divider<4;divider++)for(int first=0;first<7;first++)
   for(int second=0;second<(divider==2?7:6);second++)for(int passage=0;passage<2;passage++){
    Choices r=new Choices(divider,first,second,passage);
    Map<BlockPos,IBlockState> p=ExplorationGenerator.plan(0,64,0,0,r);
    ok(r.i==4,"unexpected number of dungeon choices");
    ok(!p.containsKey(new BlockPos(8,65,10))&&!p.containsKey(new BlockPos(8,66,10)),"blocked chest");
    assertDungeonConnected(p,divider+"/"+first+"/"+second+"/"+passage);
    exhaustive++;
   }
  ok(exhaustive==350,"incomplete exhaustive dungeon combinations");
  Set<Map<BlockPos,IBlockState>> layouts=new HashSet<>();
  for(int n=0;n<100;n++)layouts.add(ExplorationGenerator.plan(0,64,0,0,new Random(n)));
  ok(layouts.size()>25,"insufficient dungeon variety");
  Map<BlockPos,IBlockState> origin=ExplorationGenerator.plan(0,64,0,0,new Random(42));
  Map<BlockPos,IBlockState> negative=ExplorationGenerator.plan(-32,64,-48,0,new Random(42));
  for(Map.Entry<BlockPos,IBlockState> e:origin.entrySet()){
   BlockPos p=e.getKey();ok(e.getValue().equals(negative.get(new BlockPos(p.getX()-32,p.getY(),p.getZ()-48))),"negative chunk translation");
  }
  for(int type=0;type<3;type++){
   Chunk c=new Chunk(0,0,null,64);World w=new World(c);w.seed=seed(type);run(new ExplorationGenerator(),c,w);
   BlockPos chest=new BlockPos(8,65,10);ok(c.tiles.get(chest) instanceof TileEntityChest,"missing chest type "+type);
   ok(((TileEntityChest)c.tiles.get(chest)).loot!=null,"loot missing");
   for(BlockPos p:w.writes)ok((p.getX()>>4)==0&&(p.getZ()>>4)==0,"outside chunk");
   int before=w.writes.size();run(new ExplorationGenerator(),c,w);ok(w.writes.size()==before,"duplicate generation");
  }
  for(net.minecraft.block.Block ore:new net.minecraft.block.Block[]{com.nexusrpg.init.ModBlocks.MITHRIL_ORE, com.nexusrpg.init.ModBlocks.ADAMANTITE_ORE, com.nexusrpg.init.ModBlocks.DRACONITE_ORE}){
   Chunk oc=new Chunk(0,0,null,64);World ow=new World(oc);ow.seed=seed(0);BlockPos op=new BlockPos(4,30,4);oc.overlay.put(op,ore.getDefaultState());
   run(new ExplorationGenerator(),oc,ow);ok(oc.tiles.get(new BlockPos(8,65,10)) instanceof TileEntityChest,"Nexus ore rejects valid site");
   ok(oc.getBlockState(op).getBlock()==ore,"Nexus ore changed");
  }
  Chunk c=new Chunk(0,0,null,64);World w=new World(c);w.seed=seed(0);c.overlay.put(new BlockPos(4,30,4),Blocks.LOG.getDefaultState());run(new ExplorationGenerator(),c,w);ok(w.writes.isEmpty(),"underground structure");
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);c.overlay.put(new BlockPos(8,65,8),Blocks.LOG.getDefaultState());run(new ExplorationGenerator(),c,w);ok(w.writes.isEmpty(),"surface structure");
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);ExplorationGenerator g=new ExplorationGenerator();g.onLoad(new ChunkDataEvent.Load(w,c));run(g,c,w);ok(w.writes.isEmpty(),"saved chunk");
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);w.border=false;run(new ExplorationGenerator(),c,w);ok(w.writes.isEmpty(),"border");
  for(String mode:new String[]{"remote","dimension","disabled","entity","height","tile","irregular"}){
   c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);
   if(mode.equals("remote"))w.isRemote=true;
   if(mode.equals("dimension"))w.provider.dim=1;
   if(mode.equals("disabled"))w.mapFeatures=false;
   if(mode.equals("entity"))w.entity=true;
   if(mode.equals("height"))w.height=70;
   if(mode.equals("tile"))c.tiles.put(new BlockPos(4,30,4),new TileEntityChest());
   if(mode.equals("irregular"))c.h[4][4]=63;
   run(new ExplorationGenerator(),c,w);ok(w.writes.isEmpty(),"unsafe guard "+mode);
  }
  // Reject caves and fluids immediately under both the foundation and its buffer.
  for(net.minecraft.block.Block hole:new net.minecraft.block.Block[]{Blocks.AIR,Blocks.WATER,Blocks.FLOWING_WATER,Blocks.LAVA,Blocks.FLOWING_LAVA})
   for(int depth=1;depth<=3;depth++)for(int[] pos:new int[][]{{2,2},{3,3},{8,8},{12,12},{13,13}}){
    c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);
    c.overlay.put(new BlockPos(pos[0],63-depth,pos[1]),hole.getDefaultState());
    run(new ExplorationGenerator(),c,w);ok(w.writes.isEmpty(),"unstable foundation "+depth+" "+pos[0]);
   }
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);
  c.overlay.put(new BlockPos(8,59,8),Blocks.AIR.getDefaultState());run(new ExplorationGenerator(),c,w);
  ok(c.tiles.get(new BlockPos(8,65,10)) instanceof TileEntityChest,"deep cave below four-layer support");
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);w.chestFailure="undermineBeforeChest";w.mutation=new BlockPos(8,61,8);
  run(new ExplorationGenerator(),c,w);
  ok(!c.tiles.containsKey(new BlockPos(8,65,10)),"chest created after support damaged");
  for(int x=3;x<=12;x++)for(int z=3;z<=12;z++)for(int y=64;y<75;y++)
   ok(c.getBlockState(new BlockPos(x,y,z)).getBlock()==Blocks.AIR,"rollback after support damaged");
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);w.chestFailure="undermineChest";w.mutation=new BlockPos(8,61,8);
  run(new ExplorationGenerator(),c,w);
  TileEntityChest savedChest=(TileEntityChest)c.tiles.get(new BlockPos(8,65,10));
  ok(savedChest!=null && savedChest.loot==null,"invalid support must not populate chest");
  ok(c.getBlockState(new BlockPos(8,64,10)).getBlock()!=Blocks.AIR,"must preserve chest support after callback");
  for(String mode:new String[]{"overlap","malformed","malformedY","boundary","outside"}){
   c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);
   net.minecraft.world.gen.structure.MapGenStructureData d=new net.minecraft.world.gen.structure.MapGenStructureData();
   net.minecraft.nbt.NBTTagCompound feature=new net.minecraft.nbt.NBTTagCompound();
   if(!mode.equals("malformed"))feature.entries.put("BB",mode.equals("outside")?new int[]{20,0,20,30,90,30}:
    mode.equals("malformedY")?new int[]{20,90,20,30,0,30}:
    mode.equals("boundary")?new int[]{13,0,13,20,90,20}:new int[]{0,0,0,5,90,5});
   d.tags.entries.put("one",feature);w.storage.data.put("Village",d);
   run(new ExplorationGenerator(),c,w);
   ok(mode.equals("outside") ? !w.writes.isEmpty() : w.writes.isEmpty(),"vanilla bounds guard "+mode);
  }
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);w.failFloor=true;
  run(new ExplorationGenerator(),c,w);
  ok(!c.tiles.containsKey(new BlockPos(8,65,10)),"chest after failed floor placement");
  ok(c.getBlockState(new BlockPos(3,64,3)).getBlock()==Blocks.AIR,"floor remained after failed write");
  c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);w.chestFailure="foreignPlan";w.mutation=new BlockPos(4,64,4);
  run(new ExplorationGenerator(),c,w);
  ok(c.getBlockState(w.mutation).getBlock()==Blocks.LOG,"foreign block overwritten or deleted");
  ok(!c.tiles.containsKey(new BlockPos(8,65,10)),"chest after foreign modification");
  ok(c.getBlockState(new BlockPos(3,64,3)).getBlock()==Blocks.AIR,"owned blocks not rolled back");
  for(String mode:new String[]{"falseBefore","throwBefore","falseNoTile","throwAfterNoTile"}){
   c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);w.chestFailure=mode;
   try{run(new ExplorationGenerator(),c,w);ok(!mode.startsWith("throw"),"expected exception");}catch(RuntimeException e){ok(mode.startsWith("throw"),"unexpected exception");}
   for(int x=3;x<=12;x++)for(int z=3;z<=12;z++)for(int y=64;y<75;y++)ok(c.getBlockState(new BlockPos(x,y,z)).getBlock()==Blocks.AIR,"rollback "+mode);
  }
  for(String mode:new String[]{"falseWithTile","throwAfterTile"}){
   c=new Chunk(0,0,null,64);w=new World(c);w.seed=seed(0);w.chestFailure=mode;try{run(new ExplorationGenerator(),c,w);}catch(RuntimeException e){ok(mode.startsWith("throw"),"exception");}
   ok(c.tiles.get(new BlockPos(8,65,10)) instanceof TileEntityChest,"lost chest");ok(c.getBlockState(new BlockPos(8,64,10)).getBlock()!=Blocks.AIR,"lost chest support");
  }
  System.out.println("PASS 900 seeded layouts + 350 exhaustive dungeon choices, chest access, generation, bounds, saved chunks, collision, border, rollback and tile preservation (simulated world)");
 }
}
