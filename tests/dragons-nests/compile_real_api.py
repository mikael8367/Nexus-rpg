#!/usr/bin/env python3
"""Compile the generator against local real Forge APIs and placeholder dragon classes.
This is not a full mod build and not an in-game test."""
import os
from pathlib import Path
import subprocess
import tempfile
root = Path(__file__).resolve().parents[2]
cache = Path(os.environ.get('GRADLE_USER_HOME', '/home/daytona/workspace/nexus-test/gradle'))
forge = cache / 'caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar'
if not forge.exists(): raise SystemExit('Mapped Forge jar not found: ' + str(forge))
home = Path(os.environ.get('JAVA_HOME', '/home/daytona/workspace/nexus-test/jdk'))
with tempfile.TemporaryDirectory(prefix='dragon-api-') as td:
    t = Path(td); source = t / 'com/nexusrpg/dragons'; source.mkdir(parents=True)
    for name in ('Fire','Water','Wind','Earth'):
        (source / ('Entity%sDragon.java'%name)).write_text('package com.nexusrpg.dragons; public class Entity%sDragon extends net.minecraft.entity.EntityLiving {public Entity%sDragon(net.minecraft.world.World w){super(w);}}'%(name,name))
    out = t / 'classes'; out.mkdir()
    cp = str(forge) + os.pathsep + str(root / 'build/classes/java/main')
    sources = [str(p) for p in source.glob('*.java')] + [str(root / 'src/main/java/com/nexusrpg/dragons/world/DragonNestGenerator.java')]
    subprocess.run([str(home/'bin/javac'), '-source','8','-target','8','-cp',cp,'-d',str(out)]+sources,check=True)
    print('PASS: generator compiles against mapped Forge 1.12.2 API and temporary entity stubs')
