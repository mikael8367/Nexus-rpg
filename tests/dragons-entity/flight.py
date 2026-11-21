#!/usr/bin/env python3
"""Flight unit tests with instrumented stubs, not a Forge physics/integration test."""
import os, pathlib, subprocess, tempfile
from stubs import stubs
root = pathlib.Path(__file__).resolve().parents[2]
test = r'''package com.nexusrpg.dragons;
import net.minecraft.world.World; import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB; import net.minecraft.nbt.NBTTagCompound;
public class FlightHarness {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void tick(EntityElementalDragon d){d.tasks.list.get(1).updateTask(); d.updateAITasks();}
 static boolean close(double a,double b){return Math.abs(a-b)<1e-8;}
 public static void main(String[] args){
  for(int i=0;i<4;i++){
   World w=new World(); EntityElementalDragon d=i==0?new EntityFireDragon(w):i==1?new EntityWaterDragon(w):i==2?new EntityWindDragon(w):new EntityEarthDragon(w);
   EntityPlayer p=new EntityPlayer(w); d.posY=p.posY=64; p.posX=9; d.setAttackTarget(p);
   tick(d); ok(close(d.motionY,.16)&&close(d.motionX,.16),"takeoff/chase "+i);
   NBTTagCompound save=new NBTTagCompound(); d.writeEntityToNBT(save); ok(save.getBoolean("DragonFlightLanding"),"persist flight");
   d.onGround=false; w.obstacles.add(new AxisAlignedBB(1.45,63,-2,2,69,2)); tick(d); ok(close(d.motionX,0)&&d.motionY>0,"wall blocks horizontal but permits upward");
   w.obstacles.add(new AxisAlignedBB(-2,67.45,-2,2,68,2)); tick(d); ok(close(d.motionX,0)&&close(d.motionY,0),"ceiling blocks ascent");
   w.obstacles.clear(); w.loaded=false; int q=w.queries; tick(d); ok(w.queries==q&&close(d.motionX,0)&&d.motionY<=0,"unloaded: no collision query or powered flight");
   w.loaded=true; tick(d); d.setAttackTarget(null); d.updateAITasks(); ok(close(d.motionY,-.12)&&close(d.motionX,0),"landing on target loss");
   ok(d.fallDistance==0,"controlled descent clears accumulated distance");
   EntityElementalDragon reloaded=i==2?new EntityWindDragon(w):new EntityFireDragon(w); reloaded.posY=64; reloaded.onGround=false; reloaded.readEntityFromNBT(save); reloaded.updateAITasks(); ok(close(reloaded.motionY,-.12),"reload resumes landing");
   reloaded.onGround=true; reloaded.updateAITasks(); NBTTagCompound landed=new NBTTagCompound(); reloaded.writeEntityToNBT(landed); ok(!landed.getBoolean("DragonFlightLanding"),"landing finishes");
   if(i!=2){reloaded.fall(10,1); ok(reloaded.falls==1,"normal fall is never overridden");}
  }
  World w=new World(); w.height=128; EntityFireDragon d=new EntityFireDragon(w); EntityPlayer p=new EntityPlayer(w); d.posY=p.posY=122.95; p.posX=2; d.setAttackTarget(p); tick(d); ok(d.posY+d.motionY<=123.00000001,"dimension height cap");
  d.posY=p.posY=64; d.onGround=false; p.capabilities.isCreativeMode=true; tick(d); ok(d.motionY<=0,"invalid target lands");
  p.capabilities.isCreativeMode=false; d.water=true; d.motionX=.03; d.motionY=.04; tick(d); ok(close(d.motionX,.03)&&close(d.motionY,.04),"water leaves swimming in control");
  d.water=false; d.riding=true; tick(d); ok(close(d.motionY,.04),"riding not controlled"); d.riding=false;
  w.isRemote=true; d.motionY=.03; tick(d); ok(close(d.motionY,.03),"client no control");
  // A grounded dragon at an unloaded border must not retain landing state or velocity.
  w.isRemote=false; p.capabilities.isCreativeMode=false; d.posY=p.posY=64; d.onGround=true; tick(d);
  d.motionX=.16; d.onGround=true; w.loaded=false; int before=w.queries; d.updateAITasks();
  NBTTagCompound edge=new NBTTagCompound(); d.writeEntityToNBT(edge);
  ok(!edge.getBoolean("DragonFlightLanding")&&close(d.motionX,0)&&w.queries==before,"grounded unloaded border clears and stops without lava/box queries");
  w.loaded=true; d.onGround=true; tick(d); d.onGround=false; d.noAI=true; d.motionX=.16; d.motionY=.16;
  d.onLivingUpdate(); NBTTagCompound disabled=new NBTTagCompound(); d.writeEntityToNBT(disabled);
  ok(!disabled.getBoolean("DragonFlightLanding")&&close(d.motionX,0)&&d.motionY<=0,"NoAI abandons powered flight");
  d.fall(10,1); ok(d.falls>0,"NoAI normal fall");
  d.noAI=false; d.onGround=false; d.setAttackTarget(p); w.liquids.add(new AxisAlignedBB(1.41,64,-2,2,68,2)); p.posX=9; d.posY=64; d.motionY=.2; tick(d);
  NBTTagCompound liquid=new NBTTagCompound(); d.writeEntityToNBT(liquid);
  ok(!liquid.getBoolean("DragonFlightLanding")&&close(d.motionX,0)&&d.motionY<=0,"projected liquid abandons powered flight");
  w.liquids.clear(); d.onGround=true; p.posY=d.posY=64; d.motionX=0; tick(d); d.onGround=false;
  d.setAttackTarget(null); d.updateAITasks(); ok(close(d.motionY,-.12),"target loss descends");
  w.obstacles.add(new AxisAlignedBB(-2,63.95,-2,2,64,2)); d.updateAITasks(); ok(close(d.motionY,-.12),"floor descent left to vanilla collision");
  d.onGround=true; d.updateAITasks(); NBTTagCompound finished=new NBTTagCompound(); d.writeEntityToNBT(finished); ok(!finished.getBoolean("DragonFlightLanding"),"floor finishes landing");
  System.out.println("Dragon flight harness: PASS (all 4, sweep, borders, ceiling, landing, NBT, liquids, riding, client)");
 }
}'''
with tempfile.TemporaryDirectory(prefix='dragon-flight-') as td:
 p=pathlib.Path(td); sources=[]
 for name,text in stubs.items():
  f=p/name; f.parent.mkdir(parents=True,exist_ok=True); f.write_text(text); sources.append(str(f))
 f=p/'com/nexusrpg/dragons/FlightHarness.java'; f.parent.mkdir(parents=True,exist_ok=True); f.write_text(test); sources.append(str(f))
 sources += [str(root/'src/main/java/com/nexusrpg/dragons'/name) for name in ('EntityElementalDragon.java','EntityFireDragon.java','EntityWaterDragon.java','EntityWindDragon.java','EntityEarthDragon.java')]
 home=pathlib.Path(os.environ.get('JAVA_HOME','/home/daytona/workspace/nexus-test/jdk')); (p/'classes').mkdir()
 subprocess.run([str(home/'bin/javac'),'-source','8','-target','8','-d',str(p/'classes')]+sources,check=True)
 subprocess.run([str(home/'bin/java'),'-cp',str(p/'classes'),'com.nexusrpg.dragons.FlightHarness'],check=True)
