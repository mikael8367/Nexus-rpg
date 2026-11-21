#!/usr/bin/env python3
"""Run the actual helper against minimal fakes; not a live Minecraft test."""
from pathlib import Path
import os, subprocess, tempfile
root = Path(__file__).resolve().parents[2]
sources = {
'net/minecraft/world/World.java': '''package net.minecraft.world; public class World {public boolean isRemote;}''',
'net/minecraft/entity/EntityLivingBase.java': '''package net.minecraft.entity; import net.minecraft.world.World; public class EntityLivingBase {public World world; public double posX,posY,posZ; public float height; public EntityLivingBase(World w){world=w;}}''',
'com/nexusrpg/dragons/EntityElementalDragon.java': '''package com.nexusrpg.dragons; import net.minecraft.entity.EntityLivingBase; import net.minecraft.world.World; public class EntityElementalDragon extends EntityLivingBase {public enum Element {FIRE,WATER,WIND,EARTH} public Element e; public EntityElementalDragon(World w,Element e){super(w);this.e=e;} public Element getElement(){return e;}}''',
'net/minecraft/util/EnumParticleTypes.java': '''package net.minecraft.util; public enum EnumParticleTypes {FLAME,SMOKE_NORMAL,WATER_SPLASH,WATER_DROP,CLOUD,SPELL_INSTANT,BLOCK_DUST}''',
'net/minecraft/block/Block.java': '''package net.minecraft.block; public class Block {public Object getDefaultState(){return this;} public static int getStateId(Object o){return 77;}}''',
'net/minecraft/init/Blocks.java': '''package net.minecraft.init; import net.minecraft.block.Block; public class Blocks {public static final Block DIRT=new Block();}''',
'net/minecraft/world/WorldServer.java': '''package net.minecraft.world; import java.util.*; import net.minecraft.util.EnumParticleTypes; public class WorldServer extends World {public static class Call {public EnumParticleTypes type; public double x,y,z,a,b,c,speed; public int count; public int[] args; Call(EnumParticleTypes t,double x,double y,double z,int n,double a,double b,double c,double s,int[] v){type=t;this.x=x;this.y=y;this.z=z;count=n;this.a=a;this.b=b;this.c=c;speed=s;args=v;}} public List<Call> calls=new ArrayList<Call>(); public int spawnParticle(EnumParticleTypes p,double x,double y,double z,int n,double a,double b,double c,double speed,int... args){calls.add(new Call(p,x,y,z,n,a,b,c,speed,args));return 1;}}''',
'DragonVisualTest.java': '''import com.nexusrpg.dragons.*; import net.minecraft.entity.EntityLivingBase; import net.minecraft.util.EnumParticleTypes; import net.minecraft.world.*; import static com.nexusrpg.dragons.EntityElementalDragon.Element.*;
public class DragonVisualTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);} static boolean eq(double a,double b){return Math.abs(a-b)<1e-6;}
 public static void main(String[] args){
  WorldServer w=new WorldServer(); EntityElementalDragon d=new EntityElementalDragon(w,FIRE); d.height=3.4F; EntityLivingBase t=new EntityLivingBase(w);t.posX=10;t.posY=0;t.height=2;
  EntityElementalDragon.Element[] elements={FIRE,WATER,WIND,EARTH}; EnumParticleTypes[][] types={{EnumParticleTypes.FLAME,EnumParticleTypes.SMOKE_NORMAL},{EnumParticleTypes.WATER_SPLASH,EnumParticleTypes.WATER_DROP},{EnumParticleTypes.CLOUD,EnumParticleTypes.SPELL_INSTANT},{EnumParticleTypes.BLOCK_DUST,EnumParticleTypes.CLOUD}};
  for(int j=0;j<elements.length;j++){w.calls.clear();d.e=elements[j];DragonBreathVisual.show(d,t);check(w.calls.size()==24,"24 packets " + d.e); for(int i=0;i<12;i++){WorldServer.Call a=w.calls.get(2*i),b=w.calls.get(2*i+1); double f=(i+1)/12.0;check(a.type==types[j][0]&&b.type==types[j][1],"particle palette");check(eq(a.x,.9+9.1*f)&&eq(a.y,3.4F*.7+(1-3.4F*.7)*f)&&eq(a.z,0),"beam position");check(eq(a.x,b.x)&&eq(a.y,b.y)&&eq(a.z,b.z),"paired sample");check(a.count==2&&b.count==2&&eq(a.a,.06+.2*f)&&eq(a.b,a.a)&&eq(a.c,a.a)&&eq(a.speed,0),"bounded counts and spread");check(b.args.length==0,"ordinary particle args");check(j==3 ? a.args.length==1&&a.args[0]==77 : a.args.length==0,"dust block id");}}
  w.calls.clear();DragonBreathVisual.show(null,t);DragonBreathVisual.show(d,null);d.e=null;DragonBreathVisual.show(d,t);d.e=FIRE;w.isRemote=true;DragonBreathVisual.show(d,t);w.isRemote=false; t.world=new WorldServer();DragonBreathVisual.show(d,t);t.world=w;
  double old=t.posX;t.posX=Double.NaN;DragonBreathVisual.show(d,t);t.posX=Double.POSITIVE_INFINITY;DragonBreathVisual.show(d,t);t.posX=100;DragonBreathVisual.show(d,t);t.posX=0;t.posY=d.height*.7-t.height*.5;DragonBreathVisual.show(d,t);check(w.calls.isEmpty(),"invalid inputs must be silent");
  t.posX=old;t.posY=0;d.world=new World();t.world=d.world;DragonBreathVisual.show(d,t);check(w.calls.isEmpty(),"non-server world");d.world=w;t.world=w;d.posX=Double.NEGATIVE_INFINITY;DragonBreathVisual.show(d,t);check(w.calls.isEmpty(),"invalid origin");d.posX=0;
  t.posX=0;t.posY=5;t.posZ=0;DragonBreathVisual.show(d,t);check(w.calls.size()==24,"vertical beam");for(WorldServer.Call c:w.calls)check(!Double.isNaN(c.x)&&!Double.isInfinite(c.y),"finite vertical points");
  java.util.Random random=new java.util.Random(8192); for(int k=0;k<500;k++){w.calls.clear(); d.posX=random.nextDouble()*200-100;d.posY=random.nextDouble()*100;d.posZ=random.nextDouble()*200-100;d.e=elements[k%4]; t.posX=d.posX+random.nextDouble()*12-6;t.posY=d.posY+random.nextDouble()*4-2;t.posZ=d.posZ+random.nextDouble()*12-6; DragonBreathVisual.show(d,t);check(w.calls.size()==24,"random sample count");for(WorldServer.Call c:w.calls){check(Double.isFinite(c.x)&&Double.isFinite(c.y)&&Double.isFinite(c.z),"finite random point");check(c.x>=Math.min(d.posX,t.posX)-1e-6&&c.x<=Math.max(d.posX,t.posX)+1e-6&&c.z>=Math.min(d.posZ,t.posZ)-1e-6&&c.z<=Math.max(d.posZ,t.posZ)+1e-6,"horizontal line bounds");check(c.y>=Math.min(d.posY+d.height*.7,t.posY+t.height*.5)-1e-6&&c.y<=Math.max(d.posY+d.height*.7,t.posY+t.height*.5)+1e-6,"vertical line bounds");}}
  System.out.println("PASS: 4 palettes, beam geometry, 500 randomized cases, bounds, dust metadata, remote/mismatch, null, NaN/infinity, range, vertical");
 }
}'''
}
with tempfile.TemporaryDirectory(dir=str(root / 'tests/dragon-visual')) as tmp:
    tmp = Path(tmp)
    for name, content in sources.items():
        p = tmp / name; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(content)
    home = os.environ.get('JAVA_HOME')
    javac = str(Path(home)/'bin/javac') if home else 'javac'
    java = str(Path(home)/'bin/java') if home else 'java'
    subprocess.run([javac, '-source', '8', '-target', '8', '-d', str(tmp)] + [str(tmp/p) for p in sources] + [str(root/'src/main/java/com/nexusrpg/dragons/DragonBreathVisual.java')], check=True)
    subprocess.run([java, '-cp', str(tmp), 'DragonVisualTest'], check=True)
