#!/usr/bin/env python3
"""Compile and exercise production dragon classes against an instrumented 1.12-shaped fake world.
This is a unit harness, not Minecraft or a substitute for a Forge integration test.
"""
import os, pathlib, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parents[2]
from stubs import stubs
test = r'''package com.nexusrpg.dragons;
import net.minecraft.world.World; import net.minecraft.entity.player.EntityPlayer; import net.minecraft.entity.ai.EntityAIBase; import net.minecraft.nbt.NBTTagCompound;
public class Harness {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);} static EntityAIBase ai(EntityElementalDragon d){return d.tasks.list.get(1);} static void run(EntityElementalDragon d){ai(d).updateTask();}
 public static void main(String[] a) throws Exception {
  ok(DragonRegistration.class.getAnnotation(net.minecraftforge.fml.common.Mod.EventBusSubscriber.class).modid().equals("nexusrpg"),"subscriber");
  ok(DragonRegistration.class.getMethod("registerEntities",net.minecraftforge.event.RegistryEvent.Register.class).isAnnotationPresent(net.minecraftforge.fml.common.eventhandler.SubscribeEvent.class),"event method");
  net.minecraftforge.event.RegistryEvent.Register<net.minecraftforge.fml.common.registry.EntityEntry> event=new net.minecraftforge.event.RegistryEvent.Register<>();
  DragonRegistration.registerEntities(event); ok(event.registry.entries.size()==4,"four registered");
  String[] names={"fire_dragon","water_dragon","wind_dragon","earth_dragon"}; Class<?>[] types={EntityFireDragon.class,EntityWaterDragon.class,EntityWindDragon.class,EntityEarthDragon.class};
  for(int i=0;i<4;i++){ net.minecraftforge.fml.common.registry.EntityEntry e=event.registry.entries.get(i); ok(e.entity==types[i]&&e.id==i+3&&e.key.domain.equals("nexusrpg")&&e.key.path.equals(names[i])&&e.name.equals(names[i])&&e.egg,"entity registration/egg "+i); }
  for(int i=0;i<4;i++) { World w=new World(); EntityElementalDragon d=i==0?new EntityFireDragon(w):i==1?new EntityWaterDragon(w):i==2?new EntityWindDragon(w):new EntityEarthDragon(w);
   EntityPlayer p=new EntityPlayer(w), ally=new EntityPlayer(w); p.posX=8; d.setAttackTarget(p); ok(!d.getCanSpawnHere()&&d.persistent,"spawn/persistence");
   w.loaded=false; run(d); ok(p.hits==0&&d.nav.paths==0,"unloaded"); w.loaded=true;
   p.capabilities.isCreativeMode=true; ok(!ai(d).shouldExecute(),"creative"); run(d); ok(p.hits==0,"creative hit"); p.capabilities.isCreativeMode=false;
   p.capabilities.disableDamage=true; run(d); ok(p.hits==0,"invulnerable player"); p.capabilities.disableDamage=false;
   p.spectator=true; run(d); ok(p.hits==0,"spectator"); p.spectator=false;
   p.alive=false; run(d); ok(p.hits==0,"dead player"); p.alive=true;
   p.world=new World(); run(d); ok(p.hits==0,"different world"); p.world=w;
   p.team=true; ok(!ai(d).shouldExecute(),"ally"); run(d); ok(p.hits==0,"ally hit"); p.team=false;
   d.senses.visible=false; run(d); ok(p.hits==0&&d.nav.paths==1,"LOS/path"); d.senses.visible=true; run(d); ok(p.hits==1&&p.damage==6&&ally.hits==0,"ranged/only target");
   ok((i==0&&p.fire==3)||(i==1&&p.effects.size()==2)||(i==2&&p.motionX>0&&p.velocityChanged)||(i==3&&p.effects.size()==2),"element");
   run(d); ok(p.hits==1,"cooldown"); NBTTagCompound n=new NBTTagCompound(); d.writeEntityToNBT(n); ok(n.getInteger("DragonAttackCooldown")==80,"save");
   EntityElementalDragon copy=new EntityFireDragon(w); copy.readEntityFromNBT(n); copy.setAttackTarget(p); run(copy); ok(p.hits==1,"reload cooldown"); n.setInteger("DragonAttackCooldown",9999); copy.readEntityFromNBT(n); NBTTagCompound n2=new NBTTagCompound(); copy.writeEntityToNBT(n2); ok(n2.getInteger("DragonAttackCooldown")==200,"clamp");
   for(int j=0;j<80;j++) d.updateAITasks(); p.posX=2; run(d); ok(p.hits==2&&p.damage==8,"melee after cooldown");
   for(int j=0;j<30;j++) d.updateAITasks(); p.posX=8; p.damageResult=false; int effects=p.effects.size(), fire=p.fire; double v=p.motionX; run(d); ok(p.hits==3&&p.effects.size()==effects&&p.fire==fire&&p.motionX==v,"cancelled damage"); run(d); ok(p.hits==3,"cancelled cooldown");
   for(int j=0;j<80;j++) d.updateAITasks(); p.damageResult=true; p.cancelLate=true;
   run(d); ok(p.hits==4&&p.effects.size()==effects&&p.fire==fire&&p.motionX==v,"late cancelled damage");
   run(d); ok(p.hits==4,"late cancelled cooldown");
   w.isRemote=true; for(int j=0;j<90;j++)d.updateAITasks(); run(d); ok(p.hits==4,"client guard");
  }
  ok(new EntityWaterDragon(new World()).canBreatheUnderwater(),"water breathe");
  World w=new World(); EntityFireDragon d=new EntityFireDragon(w); net.minecraft.pathfinding.PathNavigate n=d.createNavigator(w); w.loaded=false;
  ok(n.getPathToPos(new net.minecraft.util.math.BlockPos())==null,"unloaded pos path"); ok(n.getPathToEntityLiving(new EntityPlayer(w))==null,"unloaded entity path"); n.onUpdateNavigation(); ok(n.paths==0&&n.ticks==0&&n.clears==1,"unloaded repath");
  w.loaded=true; ok(n.getPathToEntityLiving(new EntityPlayer(w))!=null,"loaded path"); n.onUpdateNavigation(); ok(n.paths==1&&n.ticks==1,"loaded nav"); n.range=200; ok(n.getPathToPos(new net.minecraft.util.math.BlockPos())==null,"oversize range");
  System.out.println("Dragon entity harness: PASS (4 elements, safety, cooldown, NBT, guarded navigation)");
 }
}'''
with tempfile.TemporaryDirectory(prefix='dragon-entity-') as td:
 p=pathlib.Path(td); sources=[]
 for name,text in stubs.items():
  f=p/name; f.parent.mkdir(parents=True,exist_ok=True); f.write_text(text); sources.append(str(f))
 f=p/'com/nexusrpg/dragons/Harness.java'; f.parent.mkdir(parents=True,exist_ok=True); f.write_text(test); sources.append(str(f))
 sources += [str(root/'src/main/java/com/nexusrpg/dragons'/name) for name in ('EntityElementalDragon.java','EntityFireDragon.java','EntityWaterDragon.java','EntityWindDragon.java','EntityEarthDragon.java','DragonRegistration.java')]
 home=pathlib.Path(os.environ.get('JAVA_HOME','/home/daytona/workspace/nexus-test/jdk'))
 (p/'classes').mkdir()
 subprocess.run([str(home/'bin/javac'),'-source','8','-target','8','-d',str(p/'classes')]+sources,check=True)
 subprocess.run([str(home/'bin/java'),'-cp',str(p/'classes'),'com.nexusrpg.dragons.Harness'],check=True)
