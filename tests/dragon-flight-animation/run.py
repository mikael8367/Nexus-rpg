#!/usr/bin/env python3
"""Run model animation against mapped Forge 1.12.2 and Java 8, without a game client."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
here = Path(__file__).resolve().parent
jdk = Path(os.environ.get('JAVA8_HOME', '/home/daytona/workspace/nexus-test/jdk'))
cache = Path(os.environ.get('GRADLE_USER_HOME', '/home/daytona/workspace/nexus-test/gradle'))
forge = Path(os.environ.get('FORGE_MAPPED_JAR', str(cache / 'caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar')))
assert forge.is_file(), 'Set FORGE_MAPPED_JAR to mapped Forge 1.12.2'
model = root / 'src/main/java/com/nexusrpg/dragons/client/DragonModel.java'
with tempfile.TemporaryDirectory(prefix='run-', dir=here) as td:
    subprocess.run([str(jdk / 'bin/javac'), '-source', '8', '-target', '8', '-cp', str(forge), '-d', td, str(model), str(here / 'FlightAnimationChecks.java')], check=True)
    jars = sorted((cache / 'caches/modules-2/files-2.1').rglob('*.jar'))
    cp = os.pathsep.join([td, str(forge)] + [str(p) for p in jars])
    subprocess.run([str(jdk / 'bin/java'), '-cp', cp, 'FlightAnimationChecks'], check=True, cwd=td)
