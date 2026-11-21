#!/usr/bin/env python3
"""Execute actual creative tab Java logic against minimal fake Minecraft APIs."""
from pathlib import Path
import subprocess, os, sys
root=Path(__file__).resolve().parents[2]
jdk=Path(os.environ.get('JAVA_HOME','/home/daytona/workspace/nexus-test/jdk'))
s={
'com/nexusrpg/NexusRpg':'public class NexusRpg { public static final String MODID="nexusrpg",NAME="Nexus RPG"; }',
'com/nexusrpg/init/ModBlocks':'public class ModBlocks { public static net.minecraft.block.Block MITHRIL_ORE = new net.minecraft.block.Block(); }',
'net/minecraft/util/ResourceLocation':'public class ResourceLocation { String d,p; public ResourceLocation(String d,String p){this.d=d;this.p=p;} public String getResourceDomain(){return d;} public boolean equals(Object x){return x instanceof ResourceLocation && d.equals(((ResourceLocation)x).d) && p.equals(((ResourceLocation)x).p);} public int hashCode(){return (d+":"+p).hashCode();} }',
'net/minecraft/util/NonNullList':'public class NonNullList<T> extends java.util.ArrayList<T> { public static <T> NonNullList<T> create(){return new NonNullList<T>();} }',
'net/minecraft/creativetab/CreativeTabs':'public abstract class CreativeTabs { public CreativeTabs(String s){} public abstract net.minecraft.item.ItemStack getTabIconItem(); public String getTranslatedTabLabel(){return "";} public void displayAllRelevantItems(net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> items){} }',
'net/minecraft/block/Block':'public class Block { public net.minecraft.util.ResourceLocation getRegistryName(){return new net.minecraft.util.ResourceLocation("nexusrpg","block");} public void setCreativeTab(net.minecraft.creativetab.CreativeTabs t){} }',
'net/minecraft/item/Item':'public class Item { public static java.util.List<Item> REGISTRY=new java.util.ArrayList<Item>(); net.minecraft.util.ResourceLocation id; public net.minecraft.creativetab.CreativeTabs tab; public Item(String domain,String path){id=new net.minecraft.util.ResourceLocation(domain,path);} public net.minecraft.util.ResourceLocation getRegistryName(){return id;} public void setCreativeTab(net.minecraft.creativetab.CreativeTabs t){tab=t;} public void getSubItems(net.minecraft.creativetab.CreativeTabs t,net.minecraft.util.NonNullList<ItemStack> a){a.add(new ItemStack(this));} }',
'net/minecraft/item/ItemBlock':'public class ItemBlock extends Item { public ItemBlock(){super("nexusrpg","block");} public net.minecraft.block.Block getBlock(){return new net.minecraft.block.Block();} }',
'net/minecraft/item/ItemStack':'public class ItemStack { Item item; net.minecraft.util.ResourceLocation entity; public ItemStack(Item i){item=i;} public ItemStack(net.minecraft.block.Block b){item=new Item("nexusrpg","block");} public boolean isEmpty(){return item==null;} public Item getItem(){return item;} }',
'net/minecraft/item/ItemMonsterPlacer':'public class ItemMonsterPlacer extends Item { public ItemMonsterPlacer(){super("minecraft","spawn_egg");} public static void applyEntityIdToItemStack(ItemStack s,net.minecraft.util.ResourceLocation id){s.entity=id;} public static net.minecraft.util.ResourceLocation getNamedIdFrom(ItemStack s){return s.entity;} }',
'net/minecraft/init/Items':'public class Items { public static final net.minecraft.item.Item COMPASS=new net.minecraft.item.Item("minecraft","compass"), SPAWN_EGG=new net.minecraft.item.ItemMonsterPlacer(); }',
'net/minecraft/entity/EntityList':'public class EntityList { public static final java.util.Map<net.minecraft.util.ResourceLocation,Object> ENTITY_EGGS=new java.util.HashMap<net.minecraft.util.ResourceLocation,Object>(); }',
'net/minecraftforge/event/RegistryEvent':'public class RegistryEvent { public static class Register<T>{public Registry<T> getRegistry(){return new Registry<T>();}} public static class Registry<T>{public java.util.Collection<T> getValuesCollection(){return java.util.Collections.emptyList();}} }',
'net/minecraftforge/fml/common/Mod':'public @interface Mod { public @interface EventBusSubscriber {String modid();} }',
'net/minecraftforge/fml/common/eventhandler/EventPriority':'public enum EventPriority { LOWEST }',
'net/minecraftforge/fml/common/eventhandler/SubscribeEvent':'public @interface SubscribeEvent { EventPriority priority(); }',
}
harness='''import com.nexusrpg.init.NexusCreativeTab;
import net.minecraft.item.*; import net.minecraft.init.Items; import net.minecraft.entity.EntityList; import net.minecraft.util.*;
public class TestCreative {
 static String[] names={"fire_dragon","water_dragon","wind_dragon","earth_dragon","bramble_hound","cavern_crawler","basalt_guardian","reed_stilt","prism_tortoise", "lumen_moth", "dune_mole", "bellback", "dune_burrower", "root_warden"};
 static ResourceLocation id(String s){return new ResourceLocation("nexusrpg",s);}
 static int count(NonNullList<ItemStack> list,ResourceLocation id){int n=0;for(ItemStack s:list)if(s.getItem()==Items.SPAWN_EGG && id.equals(ItemMonsterPlacer.getNamedIdFrom(s)))n++;return n;}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  Item existing=new Item("nexusrpg","existing"), foreign=new Item("other","foreign"); Item.REGISTRY.add(existing); Item.REGISTRY.add(foreign); Item.REGISTRY.add(Items.SPAWN_EGG);
  NonNullList<ItemStack> list=NonNullList.create(); ItemStack saved=new ItemStack(foreign); list.add(saved);
  NexusCreativeTab.TAB.displayAllRelevantItems(list); for(String s:names)check(count(list,id(s))==0,"unregistered egg");
  for(String s:names)EntityList.ENTITY_EGGS.put(id(s),new Object());
  ItemStack original=new ItemStack(Items.SPAWN_EGG); ItemMonsterPlacer.applyEntityIdToItemStack(original,id(names[0])); list.add(original);
  ItemStack unrelated=new ItemStack(Items.SPAWN_EGG); ItemMonsterPlacer.applyEntityIdToItemStack(unrelated,new ResourceLocation("other","fire_dragon")); list.add(unrelated);
  NexusCreativeTab.TAB.displayAllRelevantItems(list); for(String s:names)check(count(list,id(s))==1,"missing/duplicate: "+s);
  check(list.contains(saved)&&list.contains(original)&&list.contains(unrelated),"existing stack lost");
  check(existing.tab==NexusCreativeTab.TAB && foreign.tab==null && Items.SPAWN_EGG.tab==null,"changed vanilla or foreign tab");
  NexusCreativeTab.TAB.displayAllRelevantItems(list); for(String s:names)check(count(list,id(s))==1,"duplicated after second call");
  for(String missing:names) {
   EntityList.ENTITY_EGGS.remove(id(missing));
   NonNullList<ItemStack> fresh=NonNullList.create(); NexusCreativeTab.TAB.displayAllRelevantItems(fresh);
   for(String s:names) check(count(fresh,id(s))==(s.equals(missing)?0:1),"filter: "+missing+" / "+s);
   EntityList.ENTITY_EGGS.put(id(missing),new Object());
  }
  for(String existingName:names) {
   NonNullList<ItemStack> fresh=NonNullList.create(); ItemStack existingEgg=new ItemStack(Items.SPAWN_EGG);
   ItemMonsterPlacer.applyEntityIdToItemStack(existingEgg,id(existingName)); fresh.add(existingEgg);
   NexusCreativeTab.TAB.displayAllRelevantItems(fresh);
   for(String s:names)check(count(fresh,id(s))==1,"pre-existing dedup: "+existingName+" / "+s);
   check(fresh.contains(existingEgg),"pre-existing egg removed");
  }
  System.out.println("PASS: ten creature eggs and four dragon eggs, existing items, unregistered filtering and deduplication");
 }
}'''
if len(sys.argv) != 2: raise SystemExit('usage: creative_tab.py NEW_OUTPUT_DIR')
base = Path(sys.argv[1]).resolve()
if base.exists(): raise SystemExit('output exists; choose a new directory')
base.mkdir(parents=True)
if True:
    src=base/'src'; out=base/'out'; out.mkdir()
    for path,body in s.items():
        p=src/(path+'.java'); p.parent.mkdir(parents=True,exist_ok=True)
        package=path.rsplit('/',1)[0].replace('/','.')
        p.write_text('package '+package+'; '+body)
    (src/'TestCreative.java').write_text(harness)
    files=list(src.rglob('*.java'))+[root/'src/main/java/com/nexusrpg/init/NexusCreativeTab.java']
    subprocess.run([str(jdk/'bin/javac'),'-source','8','-target','8','-d',str(out)]+[str(p) for p in files],check=True)
    subprocess.run([str(jdk/'bin/java'),'-cp',str(out),'TestCreative'],check=True)
