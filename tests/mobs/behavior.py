#!/usr/bin/env python3
"""Execute the real mob sources against tiny test doubles, not a running game.
Pass a new empty output directory and JAVA_HOME for Java 8; nothing is deleted.
"""
import json
import os
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[2]
if len(sys.argv) != 2:
    raise SystemExit('usage: JAVA_HOME=/path/to/jdk8 python3 tests/mobs/behavior.py NEW_OUTPUT_DIR')
out = Path(sys.argv[1]).resolve()
if out.exists():
    raise SystemExit('output path exists; use a new one to preserve files')
out.mkdir(parents=True)
src, classes = out / 'src', out / 'classes'
src.mkdir(); classes.mkdir()
java_home = Path(os.environ['JAVA_HOME'])
files = {
'net/minecraft/world/World.java': 'package net.minecraft.world; public class World { public boolean isRemote; }',
'net/minecraft/entity/Entity.java': 'package net.minecraft.entity; public class Entity { public boolean alive=true; public boolean isEntityAlive(){return alive;} }',
'net/minecraft/entity/SharedMonsterAttributes.java': 'package net.minecraft.entity; public class SharedMonsterAttributes { public static final Object MAX_HEALTH=new Object(), MOVEMENT_SPEED=new Object(), ATTACK_DAMAGE=new Object(), FOLLOW_RANGE=new Object(); }',
'net/minecraft/entity/ai/EntityAITasks.java': 'package net.minecraft.entity.ai; public class EntityAITasks { public void addTask(int n,Object ai){} }',
'net/minecraft/entity/monster/EntityMob.java': '''package net.minecraft.entity.monster;
import net.minecraft.entity.*; import net.minecraft.entity.ai.*; import net.minecraft.world.World;
public class EntityMob extends Entity { public World world; public boolean attackResult=true; protected int experienceValue; protected EntityAITasks tasks=new EntityAITasks(), targetTasks=new EntityAITasks();
public EntityMob(World w){world=w;} public void setSize(float a,float b){} protected void initEntityAI(){} protected void applyEntityAttributes(){} public Attribute getEntityAttribute(Object key){return new Attribute();} public boolean attackEntityAsMob(Entity e){return attackResult;} protected net.minecraft.util.ResourceLocation getLootTable(){return null;} public static class Attribute {public void setBaseValue(double x){}} }''',
'net/minecraft/entity/player/EntityPlayer.java': '''package net.minecraft.entity.player;
import net.minecraft.entity.Entity; import net.minecraft.potion.*;
public class EntityPlayer extends Entity { public static class Capabilities {public boolean isCreativeMode;} public Capabilities capabilities=new Capabilities(); public boolean spectator; public int added; public PotionEffect active;
public boolean isSpectator(){return spectator;} public PotionEffect getActivePotionEffect(Potion p){return active != null && active.potion == p ? active : null;} public void addPotionEffect(PotionEffect e){added++;active=e;} }''',
'net/minecraft/potion/Potion.java': 'package net.minecraft.potion; public class Potion {}',
'net/minecraft/potion/PotionEffect.java': '''package net.minecraft.potion; public class PotionEffect { public Potion potion; private int duration,amplifier; public PotionEffect(Potion p,int d,int a){potion=p;duration=d;amplifier=a;} public int getDuration(){return duration;} public int getAmplifier(){return amplifier;} }''',
'net/minecraft/init/MobEffects.java': 'package net.minecraft.init; import net.minecraft.potion.Potion; public class MobEffects {public static final Potion SLOWNESS=new Potion(), WEAKNESS=new Potion(), MINING_FATIGUE=new Potion();}',
'net/minecraft/util/ResourceLocation.java': 'package net.minecraft.util; public class ResourceLocation {private String value; public ResourceLocation(String a,String b){value=a+":"+b;} public String toString(){return value;} }',
}
for ai in ('EntityAIAttackMelee', 'EntityAIHurtByTarget', 'EntityAILookIdle', 'EntityAISwimming', 'EntityAIWanderAvoidWater', 'EntityAIWatchClosest'):
    files['net/minecraft/entity/ai/'+ai+'.java'] = 'package net.minecraft.entity.ai; public class '+ai+' {public '+ai+'(Object... args){}}'
files['net/minecraft/entity/ai/EntityAINearestAttackableTarget.java'] = 'package net.minecraft.entity.ai; public class EntityAINearestAttackableTarget<T> {public EntityAINearestAttackableTarget(Object... args){}}'
files['MobBehaviorChecks.java'] = r'''import com.nexusrpg.mobs.*;
import net.minecraft.entity.Entity; import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects; import net.minecraft.potion.*; import net.minecraft.world.World;
import java.lang.reflect.Method;
public class MobBehaviorChecks {
  static void check(boolean c,String msg){if(!c)throw new AssertionError(msg);}
  static void run(EntityNexusMob mob,Potion effect,int ticks,String loot) throws Exception {
    EntityPlayer p=new EntityPlayer();
    check(mob.attackEntityAsMob(p),"normal attack delegates to superclass");
    check(p.added==0,"no unsafe extra effect");
    mob.attackResult=false;
    check(!mob.attackEntityAsMob(p),"failed attack result propagated");
    check(p.added==0,"no extra effect when hit fails");
    Method m=mob.getClass().getDeclaredMethod("getLootTable"); m.setAccessible(true); check(m.invoke(mob).toString().equals("nexusrpg:entities/"+loot),"loot id");
  }
  static void attributes(EntityNexusMob mob, double h, double speed, double damage) throws Exception {
    String[] names={"health","speed","damage"}; double[] expected={h,speed,damage};
    for(int i=0;i<names.length;i++) { Method method=mob.getClass().getDeclaredMethod(names[i]); method.setAccessible(true); check(((Double)method.invoke(mob)).doubleValue()==expected[i],names[i]); }
    for(Method method:mob.getClass().getDeclaredMethods()) check(!method.getName().equals("attackEntityAsMob"),"must inherit ordinary attack");
  }
  public static void main(String[] args) throws Exception {
    attributes(new EntityLumenMoth(new World()),20,.30,3);
    attributes(new EntityDuneMole(new World()),34,.24,5);
    attributes(new EntityBellback(new World()),40,.19,4);
    run(new EntityBramble(new World()),MobEffects.SLOWNESS,40,"bramble_hound");
    run(new EntityCavern(new World()),MobEffects.WEAKNESS,60,"cavern_crawler");
    run(new EntityBasalt(new World()),MobEffects.MINING_FATIGUE,80,"basalt_guardian");
    run(new EntityStilt(new World()),MobEffects.SLOWNESS,40,"reed_stilt");
    run(new EntityPrism(new World()),MobEffects.SLOWNESS,40,"prism_tortoise");
    run(new EntityLumenMoth(new World()),MobEffects.SLOWNESS,40,"lumen_moth");
    run(new EntityDuneMole(new World()),MobEffects.SLOWNESS,40,"dune_mole");
    run(new EntityBellback(new World()),MobEffects.SLOWNESS,40,"bellback");
    System.out.println("PASS: ordinary attacks and eight loot IDs on test doubles");
  }
}'''
for name, content in files.items():
    path=src/name; path.parent.mkdir(parents=True, exist_ok=True); path.write_text(content)
real = root/'src/main/java/com/nexusrpg/mobs'
cmd=[str(java_home/'bin/javac'), '-source', '8', '-target', '8', '-d', str(classes)]
cmd += [str(p) for p in src.rglob('*.java')]
cmd += [str(real/name) for name in ('EntityNexusMob.java','EntityBramble.java','EntityCavern.java','EntityBasalt.java','EntityStilt.java','EntityPrism.java', 'EntityLumenMoth.java', 'EntityDuneMole.java', 'EntityBellback.java')]
subprocess.run(cmd,check=True)
subprocess.run([str(java_home/'bin/java'),'-cp',str(classes),'MobBehaviorChecks'],check=True)
expected = {'bramble_hound':('string','wheat_seeds'), 'cavern_crawler':('flint','coal'), 'basalt_guardian':('cobblestone','iron_nugget'), 'reed_stilt':('feather','wheat_seeds'), 'prism_tortoise':('quartz','gold_nugget'), 'lumen_moth':('glowstone_dust','string'), 'dune_mole':('sand','flint'), 'bellback':('clay_ball','prismarine_shard')}
for name, (common, extra) in expected.items():
    data=json.loads((root/'src/main/resources/assets/nexusrpg/loot_tables/entities'/f'{name}.json').read_text())
    pools=data['pools']; assert len(pools)==2
    assert len({p['name'] for p in pools})==2
    assert all(p['rolls']==1 and len(p['entries'])==1 for p in pools)
    assert pools[0]['entries'][0]['name']=='minecraft:'+common
    assert pools[0]['entries'][0]['functions']==[{'function':'minecraft:set_count','count':{'min':1,'max':2}}]
    assert pools[1]['entries'][0]['name']=='minecraft:'+extra
    assert pools[1]['conditions']==[{'condition':'minecraft:random_chance','chance':0.25}]
    assert all(p['entries'][0]['type']=='item' for p in pools)
print('PASS: JSON structure, loot IDs, counts and chances (not the Forge loot loader)')
