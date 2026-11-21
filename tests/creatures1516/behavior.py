#!/usr/bin/env python3
"""Behavior checks using minimal Minecraft doubles, not a game session; fresh output only."""
import os, subprocess, sys
from pathlib import Path
root=Path(__file__).resolve().parents[2]
if len(sys.argv)!=2: raise SystemExit('Usage: JAVA_HOME=... python3 behavior.py NEW_OUTPUT_DIR')
out=Path(sys.argv[1]).resolve()
if out.exists(): raise SystemExit('output exists')
src=out/'src'; classes=out/'classes'; src.mkdir(parents=True); classes.mkdir()
f={
'net/minecraft/world/World.java':'package net.minecraft.world; public class World {}',
'net/minecraft/entity/Entity.java':'package net.minecraft.entity; public class Entity {}',
'net/minecraft/entity/player/EntityPlayer.java':'package net.minecraft.entity.player; public class EntityPlayer extends net.minecraft.entity.Entity {}',
'net/minecraft/entity/SharedMonsterAttributes.java':'package net.minecraft.entity; public class SharedMonsterAttributes { public static final Object MAX_HEALTH=new Object(), MOVEMENT_SPEED=new Object(), ATTACK_DAMAGE=new Object(), FOLLOW_RANGE=new Object(); }',
'net/minecraft/entity/ai/EntityAITasks.java':'package net.minecraft.entity.ai; import java.util.*; public class EntityAITasks { public List<String> names=new ArrayList<String>(); public void addTask(int p,Object ai){names.add(p+":"+ai.getClass().getSimpleName());} }',
'net/minecraft/util/ResourceLocation.java':'package net.minecraft.util; public class ResourceLocation { private final String s; public ResourceLocation(String a,String b){s=a+":"+b;} public String toString(){return s;} }',
'net/minecraft/entity/monster/EntityMob.java':'''package net.minecraft.entity.monster;
import java.util.*; import net.minecraft.entity.*; import net.minecraft.entity.ai.EntityAITasks; import net.minecraft.world.World; import net.minecraft.util.ResourceLocation;
public class EntityMob extends Entity {
 protected int experienceValue; protected EntityAITasks tasks=new EntityAITasks(),targetTasks=new EntityAITasks();
 private final Map<Object,Attribute> attrs=new HashMap<Object,Attribute>(); public float width,height; public boolean result=true;
 public EntityMob(World w) {} public void setSize(float a,float b){width=a;height=b;} protected void initEntityAI(){} protected void applyEntityAttributes(){}
 public Attribute getEntityAttribute(Object k){if(!attrs.containsKey(k))attrs.put(k,new Attribute());return attrs.get(k);}
 public boolean attackEntityAsMob(Entity e){return result;} protected ResourceLocation getLootTable(){return null;}
 public static class Attribute {public double value; public void setBaseValue(double x){value=x;}}
}''',
'com/nexusrpg/mobs/BehaviorChecks.java':'''package com.nexusrpg.mobs;
import net.minecraft.world.World; import net.minecraft.entity.*;
public class BehaviorChecks {
 static void ck(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void check(EntityNexusMob e,double hp,double speed,double dmg,float width,float height,String loot){
  e.applyEntityAttributes(); e.initEntityAI();
  ck(e.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).value==hp,"health");
  ck(e.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).value==speed,"speed");
  ck(e.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).value==dmg,"damage");
  ck(e.width==width && e.height==height,"size");
  ck(e.tasks.names.contains("0:EntityAISwimming") && e.tasks.names.contains("2:EntityAIAttackMelee") && e.tasks.names.contains("5:EntityAIWanderAvoidWater"),"tasks");
  ck(e.targetTasks.names.contains("1:EntityAIHurtByTarget") && e.targetTasks.names.contains("2:EntityAINearestAttackableTarget"),"targets");
  ck(e.attackEntityAsMob(new Entity()),"attack succeeds"); e.result=false; ck(!e.attackEntityAsMob(new Entity()),"attack failure delegated");
  ck((e instanceof EntityBurrower ? ((EntityBurrower)e).getLootTable() : ((EntityRootwarden)e).getLootTable()).toString().equals("nexusrpg:entities/"+loot),"loot");
 }
 public static void main(String[] a){check(new EntityBurrower(new World()),32,.23,5,1.4f,.8f,"dune_burrower");check(new EntityRootwarden(new World()),40,.20,6,.9f,2.2f,"root_warden");System.out.println("PASS attributes, sizes, melee AI/target tasks, delegated attack outcomes and loot ids (test doubles)");}
}'''
}
# Make task lists public only in the double to let the harness inspect task priorities.
f['net/minecraft/entity/monster/EntityMob.java']=f['net/minecraft/entity/monster/EntityMob.java'].replace('protected EntityAITasks tasks','public EntityAITasks tasks')
for name in ('EntityAIAttackMelee','EntityAIHurtByTarget','EntityAILookIdle','EntityAISwimming','EntityAIWanderAvoidWater','EntityAIWatchClosest'):
 f['net/minecraft/entity/ai/'+name+'.java']='package net.minecraft.entity.ai; public class '+name+' {public '+name+'(Object... x){}}'
f['net/minecraft/entity/ai/EntityAINearestAttackableTarget.java']='package net.minecraft.entity.ai; public class EntityAINearestAttackableTarget<T> {public EntityAINearestAttackableTarget(Object... x){}}'
for name,content in f.items():
 p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content)
mob=root/'src/main/java/com/nexusrpg/mobs'
jdk=Path(os.environ['JAVA_HOME'])/'bin'; sources=list(src.rglob('*.java'))+[mob/(x+'.java') for x in ('EntityNexusMob','EntityBurrower','EntityRootwarden')]
subprocess.run([str(jdk/'javac'),'-source','8','-target','8','-d',str(classes)]+list(map(str,sources)),check=True)
subprocess.run([str(jdk/'java'),'-cp',str(classes),'com.nexusrpg.mobs.BehaviorChecks'],check=True)
