#!/usr/bin/env python3
"""Synthetic event test; also verifies separation from production world-generation hooks."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[2]
out = Path('/home/daytona/workspace/nexus-test/biomes-1436-stubs')
files = {
 'net/minecraft/block/Block.java': '''package net.minecraft.block; public class Block { public Object getDefaultState(){return this;} }''',
 'net/minecraft/init/Blocks.java': '''package net.minecraft.init; import net.minecraft.block.Block; public class Blocks { public static final Block SAND=new Block(),SANDSTONE=new Block(),GRAVEL=new Block(),STONE=new Block(); }''',
 'net/minecraft/util/math/BlockPos.java': 'package net.minecraft.util.math; public class BlockPos {}',
 'net/minecraft/world/World.java': 'package net.minecraft.world; public class World {}',
 'net/minecraftforge/fml/common/Mod.java': '''package net.minecraftforge.fml.common; import java.lang.annotation.*; public @interface Mod { @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface EventBusSubscriber { String modid(); } }''',
 'net/minecraftforge/fml/common/eventhandler/SubscribeEvent.java': '''package net.minecraftforge.fml.common.eventhandler; import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD) public @interface SubscribeEvent {}''',
 'net/minecraftforge/event/RegistryEvent.java': '''package net.minecraftforge.event; import java.util.*; public class RegistryEvent { public static class Registry<T> { public final List<T> entries=new ArrayList<T>(); public void registerAll(T... values){ for(T v:values) entries.add(v); } } public static class Register<T> { final Registry<T> r=new Registry<T>(); public Registry<T> getRegistry(){return r;} } }''',
 'net/minecraft/world/biome/Biome.java': '''package net.minecraft.world.biome;
import java.util.Random; import net.minecraft.world.World; import net.minecraft.util.math.BlockPos;
public class Biome {
 public static class BiomeProperties { public String name; public float height,var,temp,rain; public int water; public boolean snow,dry;
 public BiomeProperties(String n){name=n;} public BiomeProperties setBaseHeight(float x){height=x;return this;}
 public BiomeProperties setHeightVariation(float x){var=x;return this;} public BiomeProperties setTemperature(float x){if(x>.1f && x<.2f) throw new IllegalArgumentException();temp=x;return this;}
 public BiomeProperties setRainfall(float x){rain=x;return this;} public BiomeProperties setWaterColor(int x){water=x;return this;}
 public BiomeProperties setSnowEnabled(){snow=true;return this;} public BiomeProperties setRainDisabled(){dry=true;return this;} }
 public BiomeProperties p; public String id; protected Object topBlock, fillerBlock; public Biome(BiomeProperties p){this.p=p;}
 public Biome setRegistryName(String mod,String id){this.id=mod+":"+id; return this;}
 public int getGrassColorAtPos(BlockPos p){return 0;} public int getFoliageColorAtPos(BlockPos p){return 0;}
 public void decorate(World w, Random r, BlockPos p){throw new AssertionError("vanilla decorator called");} }''',
 'RegistrationTest.java': '''import java.util.*; import com.nexusrpg.world.newbiomes.ExperimentalBiomes; import net.minecraft.world.biome.Biome; import net.minecraft.world.World; import net.minecraft.util.math.BlockPos; import net.minecraftforge.event.RegistryEvent;
public class RegistrationTest { public static void main(String[] args) {
 RegistryEvent.Register<Biome> e=new RegistryEvent.Register<Biome>(); ExperimentalBiomes.register(e);
 List<Biome> list=e.getRegistry().entries; if(list.size()!=6)throw new AssertionError("count");
 Set<String> ids=new HashSet<String>(); Set<Float> heights=new HashSet<Float>(); Set<Integer> tints=new HashSet<Integer>();
 for(Biome b:list){ if(!ids.add(b.id))throw new AssertionError("duplicate"); heights.add(b.p.height); tints.add(b.getGrassColorAtPos(new BlockPos())); b.decorate(new World(),new Random(1),new BlockPos()); }
 if(heights.size()!=6 || tints.size()!=6)throw new AssertionError("biome diversity");
 if(!ExperimentalBiomes.CRYSTAL.p.snow || !ExperimentalBiomes.ASH.p.dry || !ExperimentalBiomes.RED.p.dry)throw new AssertionError("climate");
 System.out.println("RegistrationTest: six unique registry prototypes and disabled decoration passed"); } }'''
}
for name, data in files.items():
 p=out/name; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(data)
sources=[str(out/n) for n in files]+[str(root/'src/main/java/com/nexusrpg/world/newbiomes/ExperimentalBiomes.java')]
jdk='/home/daytona/workspace/nexus-test/jdk/bin/'
subprocess.run([jdk+'javac','-source','8','-target','8','-d',str(out)]+sources,check=True)
subprocess.run([jdk+'java','-cp',str(out),'RegistrationTest'],check=True)
src=(root/'src/main/java/com/nexusrpg/world/newbiomes/ExperimentalBiomes.java').read_text()
assert 'BiomeManager.addBiome' not in src and 'registerWorldGenerator' not in src
all_java=list((root/'src/main/java').rglob('*.java'))
uses=[str(p.relative_to(root)) for p in all_java if p.name not in ('FeatureBlueprint.java','ExperimentalBiomes.java') and ('FeatureBlueprint' in p.read_text() or 'ExperimentalBiomes' in p.read_text())]
assert not uses, uses
print('Static isolation: no biome distribution or world-generator integration')
