#!/usr/bin/env python3
"""Compile sources and run real model/loot checks using existing mapped cache, not Gradle.
Usage: JAVA_HOME=... GRADLE_USER_HOME=... python3 tests/mobs/compile_mapped.py NEW_OUTPUT_DIR
Existing directories and caches are never modified or deleted.
"""
import os, subprocess, sys
from pathlib import Path
root=Path(__file__).resolve().parents[2]
if len(sys.argv)!=2: raise SystemExit(__doc__)
out=Path(sys.argv[1]).resolve()
if out.exists(): raise SystemExit('Output exists; choose a fresh directory')
out.mkdir(parents=True)
jdk=Path(os.environ['JAVA_HOME'])
cache=Path(os.environ['GRADLE_USER_HOME'])/'caches'
mapped=cache/'forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar'
if not mapped.is_file(): raise SystemExit('Missing mapped Forge JAR')
jars=sorted((cache/'modules-2/files-2.1').rglob('*.jar'))
# Forge itself requires the Minecraft-era versions ahead of old Gradle dependencies.
preferred=('guava-21.0.jar','gson-2.8.0.jar','commons-io-2.5.jar')
first=[p for name in preferred for p in jars if p.name==name]
cp=os.pathsep.join(map(str,[mapped]+first+[p for p in jars if p not in first]))
sources=sorted((root/'src/main/java').rglob('*.java'))+[root/'tests/mobs/ModelChecks.java',root/'tests/mobs/RealLootChecks.java']
subprocess.run([str(jdk/'bin/javac'),'-source','8','-target','8','-cp',cp,'-d',str(out)]+list(map(str,sources)),check=True)
print('PASS: complete Java sources compiled with JDK 8 against mapped Forge',flush=True)
runtime=os.pathsep.join([str(out),str(root/'src/main/resources'),cp])
for test in ['ModelChecks','RealLootChecks']:
    subprocess.run([str(jdk/'bin/java'),'-cp',runtime,test],check=True)
