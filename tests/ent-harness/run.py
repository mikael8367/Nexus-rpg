#!/usr/bin/env python3
"""Runs a simulated world test; not a replacement for testing in Minecraft."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='ent-harness-') as td:
    temp = Path(td)
    src = temp / 'src'
    shutil.copytree(root / 'tests/exploration-harness/src/net', src / 'net')
    init = src / 'com/nexusrpg/init'
    init.mkdir(parents=True)
    shutil.copy(root / 'tests/exploration-harness/src/com/nexusrpg/init/ModBlocks.java', init)
    shutil.copy(root / 'tests/structures-harness/src/com/nexusrpg/init/ModBiomes.java', init)
    box = src / 'net/minecraft/util/math/AxisAlignedBB.java'
    box.write_text('package net.minecraft.util.math; public class AxisAlignedBB { public double minX,minY,minZ,maxX,maxY,maxZ; public AxisAlignedBB(double a,double b,double c,double d,double e,double f){minX=a;minY=b;minZ=c;maxX=d;maxY=e;maxZ=f;} }')
    ent = src / 'com/nexusrpg/ent/EntityEntAnciao.java'
    ent.parent.mkdir(parents=True)
    ent.write_text('package com.nexusrpg.ent; import net.minecraft.world.World; import net.minecraft.util.math.AxisAlignedBB; public class EntityEntAnciao {public AxisAlignedBB box; public boolean persistent; public EntityEntAnciao(World w){} public void setLocationAndAngles(double x,double y,double z,float a,float b){box=new AxisAlignedBB(x-1,y,z-1,x+1,y+4,z+1);} public AxisAlignedBB getEntityBoundingBox(){return box;} public void enablePersistence(){persistent=true;}}')
    world = src / 'net/minecraft/world/World.java'
    text = world.read_text()
    end = text.rfind('}')
    extra = ''' public boolean spawnResult=true, collision=false; public int spawns=0; public boolean spawnEntity(com.nexusrpg.ent.EntityEntAnciao e){spawns++; if(!e.persistent) throw new AssertionError("not persistent"); return spawnResult;} public java.util.List<Object> getCollisionBoxes(com.nexusrpg.ent.EntityEntAnciao e, net.minecraft.util.math.AxisAlignedBB b){return collision?java.util.Collections.singletonList(new Object()):java.util.Collections.emptyList();} public boolean containsAnyLiquid(net.minecraft.util.math.AxisAlignedBB b){return false;} public boolean checkNoEntityCollision(net.minecraft.util.math.AxisAlignedBB b,com.nexusrpg.ent.EntityEntAnciao e){return !collision;}\n'''
    world.write_text(text[:end] + extra + text[end:])
    test = root / 'tests/ent-harness/src/com/nexusrpg/world/ent/EntClearingHarness.java'
    generator = root / 'src/main/java/com/nexusrpg/world/ent/EntClearingGenerator.java'
    files = [str(p) for p in src.rglob('*.java')] + [str(test), str(generator)]
    classes = temp / 'classes'
    classes.mkdir()
    java_home = os.environ.get('JAVA_HOME')
    javac = str(Path(java_home) / 'bin/javac') if java_home else 'javac'
    java = str(Path(java_home) / 'bin/java') if java_home else 'java'
    subprocess.run([javac, '-source', '8', '-target', '8', '-d', str(classes)] + files, check=True)
    subprocess.run([java, '-cp', str(classes), 'com.nexusrpg.world.ent.EntClearingHarness'], check=True)
