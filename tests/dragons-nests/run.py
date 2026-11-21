#!/usr/bin/env python3
"""Compile and exercise the real generator against a tiny simulated Forge world, not Minecraft."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='dragon-nests-') as td:
    t = Path(td); src = t / 'src'
    shutil.copytree(root / 'tests/exploration-harness/src/net', src / 'net')
    init = src / 'com/nexusrpg/init'; init.mkdir(parents=True)
    shutil.copy(root / 'tests/exploration-harness/src/com/nexusrpg/init/ModBlocks.java', init)
    def put(path, content):
        p = src / path; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(content)
    blocks = src / 'net/minecraft/init/Blocks.java'
    s = blocks.read_text(); i = s.rfind('}')
    blocks.write_text(s[:i] + 'public static final Block SEA_LANTERN=new Block("SEA_LANTERN");' + s[i:])
    put('net/minecraft/util/math/AxisAlignedBB.java', '''package net.minecraft.util.math;
public class AxisAlignedBB {public double minX,minY,minZ,maxX,maxY,maxZ; public AxisAlignedBB(double a,double b,double c,double d,double e,double f){minX=a;minY=b;minZ=c;maxX=d;maxY=e;maxZ=f;} public boolean intersects(AxisAlignedBB o){return minX<o.maxX&&maxX>o.minX&&minY<o.maxY&&maxY>o.minY&&minZ<o.maxZ&&maxZ>o.minZ;}}''')
    put('net/minecraft/entity/Entity.java', '''package net.minecraft.entity; import net.minecraft.util.math.AxisAlignedBB; public class Entity { public boolean isDead; protected AxisAlignedBB box=new AxisAlignedBB(0,0,0,1,1,1); public AxisAlignedBB getEntityBoundingBox(){return box;} }''')
    put('net/minecraft/entity/EntityLiving.java', '''package net.minecraft.entity; import net.minecraft.util.math.AxisAlignedBB; import net.minecraft.world.World; public class EntityLiving extends Entity {public boolean persistent; public EntityLiving(World w){} public void setLocationAndAngles(double x,double y,double z,float a,float b){box=new AxisAlignedBB(x-1,y,z-1,x+1,y+4,z+1);} public void enablePersistence(){persistent=true;}}''')
    for name in ('Fire', 'Water', 'Wind', 'Earth'):
        put('com/nexusrpg/dragons/Entity%sDragon.java' % name, 'package com.nexusrpg.dragons; public class Entity%sDragon extends net.minecraft.entity.EntityLiving {public Entity%sDragon(net.minecraft.world.World w){super(w);}}' % (name, name))
    world = src / 'net/minecraft/world/World.java'; s = world.read_text(); i = s.rfind('}')
    extra = ''' public java.util.List<net.minecraft.entity.Entity> loadedEntityList=new java.util.ArrayList<>();
 public boolean spawnResult=true, collision=false, liquid=false, throwSpawn=false, addThenFalse=false;
 public int spawns=0; public net.minecraft.entity.EntityLiving last;
 public boolean spawnEntity(net.minecraft.entity.EntityLiving e){spawns++; last=e; if(!e.persistent)throw new AssertionError("not persistent"); if(throwSpawn) throw new RuntimeException("spawn uncertain"); if(spawnResult||addThenFalse)loadedEntityList.add(e); return spawnResult;}
 public java.util.List<Object> getCollisionBoxes(net.minecraft.entity.Entity e,net.minecraft.util.math.AxisAlignedBB b){if(chestFailure.equals("obstacleDuringCollision")){chestFailure="";chunk.overlay.put(mutation,Blocks.LOG.getDefaultState());} return collision?java.util.Collections.singletonList(new Object()):java.util.Collections.emptyList();}
 public boolean containsAnyLiquid(net.minecraft.util.math.AxisAlignedBB b){return liquid;}
 public boolean checkNoEntityCollision(net.minecraft.util.math.AxisAlignedBB b,net.minecraft.entity.Entity e){return true;}
'''
    s = s[:i] + extra + s[i:]
    needle = '  chunk.overlay.put(p,s);\n  if(chestFailure.equals("undermineBeforeChest")'
    replacement = '''  chunk.overlay.put(p,s);
  if(chestFailure.equals("obstacleDuringWrite") && s.getBlock()!=Blocks.AIR){chestFailure="";chunk.overlay.put(mutation,Blocks.LOG.getDefaultState());}
  if(chestFailure.equals("failAfterWrite") && s.getBlock()!=Blocks.AIR){chestFailure="";return false;}
  if(chestFailure.equals("throwAfterWrite") && s.getBlock()!=Blocks.AIR){chestFailure="";throw new RuntimeException("after placement");}
  if(chestFailure.equals("tileAfterWrite") && s.getBlock()!=Blocks.AIR){chestFailure="";chunk.tiles.put(p,new TileEntityChest());return false;}
  if(chestFailure.equals("undermineBeforeChest")'''
    assert needle in s
    world.write_text(s.replace(needle, replacement))
    test = root / 'tests/dragons-nests/DragonNestHarness.java'
    generator = root / 'src/main/java/com/nexusrpg/dragons/world/DragonNestGenerator.java'
    classes = t / 'classes'; classes.mkdir()
    home = os.environ.get('JAVA_HOME'); javac = str(Path(home)/'bin/javac') if home else 'javac'; java = str(Path(home)/'bin/java') if home else 'java'
    subprocess.run([javac,'-source','8','-target','8','-d',str(classes)] + [str(p) for p in src.rglob('*.java')] + [str(test),str(generator)],check=True)
    subprocess.run([java,'-cp',str(classes),'com.nexusrpg.dragons.world.DragonNestHarness'],check=True)
