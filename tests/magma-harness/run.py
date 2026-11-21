#!/usr/bin/env python3
"""Small mocked-world test. Does not run Minecraft or Forge."""
import os, shutil, subprocess, tempfile
from pathlib import Path
root = Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='magma-harness-') as td:
    src=Path(td)/'src'
    shutil.copytree(root/'tests/structures-harness/src/net',src/'net')
    (src/'net/minecraft/world/World.java').write_text((root/'tests/exploration-harness/src/net/minecraft/world/World.java').read_text().replace(' public boolean isRemote,', ' public net.minecraft.util.math.BlockPos spawn=new net.minecraft.util.math.BlockPos(10000,64,10000); public net.minecraft.util.math.BlockPos getSpawnPoint(){return spawn;} public boolean isRemote,'))
    world=src/'net/minecraft/world/World.java'
    world.write_text(world.read_text().replace(' public boolean isRemote,', ' public boolean spawnResult=true, collision=false, liquid=false; public int spawned=0; public java.util.List<Object> getCollisionBoxes(Object e, net.minecraft.util.math.AxisAlignedBB b){return collision?java.util.Collections.singletonList(new Object()):java.util.Collections.emptyList();} public boolean containsAnyLiquid(net.minecraft.util.math.AxisAlignedBB b){return liquid;} public boolean spawnEntity(Object e){if(spawnResult) spawned++; return spawnResult;} public boolean isRemote,'))
    boss=src/'com/nexusrpg/magma/EntityColossoMagma.java'; boss.parent.mkdir(parents=True)
    boss.write_text('package com.nexusrpg.magma; public class EntityColossoMagma { public EntityColossoMagma(net.minecraft.world.World w){} public void setPosition(double x,double y,double z){} public net.minecraft.util.math.AxisAlignedBB getEntityBoundingBox(){return new net.minecraft.util.math.AxisAlignedBB(0,0,0,1,1,1);} }')
    box=src/'net/minecraft/util/math/AxisAlignedBB.java'
    box.write_text('package net.minecraft.util.math; public class AxisAlignedBB { public AxisAlignedBB(double a,double b,double c,double d,double e,double f){} }')
    init=src/'com/nexusrpg/init';init.mkdir(parents=True)
    shutil.copy(root/'tests/structures-harness/src/com/nexusrpg/init/ModBlocks.java',init)
    bio=(root/'tests/structures-harness/src/com/nexusrpg/init/ModBiomes.java').read_text().replace('public class ModBiomes {','public class ModBiomes { public static final Biome MAGMA_WASTES=new Biome();')
    (init/'ModBiomes.java').write_text(bio)
    files=[str(x) for x in src.rglob('*.java')]+[str(root/'tests/magma-harness/MagmaArenaHarness.java'),str(root/'src/main/java/com/nexusrpg/world/magma/MagmaArenaGenerator.java')]
    classes=Path(td)/'classes';classes.mkdir()
    home=os.environ.get('JAVA_HOME');javac=str(Path(home)/'bin/javac') if home else 'javac';java=str(Path(home)/'bin/java') if home else 'java'
    subprocess.run([javac,'-source','8','-target','8','-d',str(classes)]+files,check=True)
    subprocess.run([java,'-cp',str(classes),'MagmaArenaHarness'],check=True)
