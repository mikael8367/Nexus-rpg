#!/usr/bin/env python3
"""Static atlas, UV, client isolation and Forge 1.12.2 API compilation checks.

Uses the mapped Forge jar and a Java 8 JDK; the entity stubs only supply the
promised inheritance/constructor contract and are not part of the mod.
"""
import base64
import io
import os
from pathlib import Path
import re
import subprocess
import tempfile
from PIL import Image

root = Path(__file__).resolve().parents[2]
java = root / 'src/main/java/com/nexusrpg/dragons/client'
assets = root / 'src/main/resources/assets/nexusrpg/textures/entity/dragons'
kinds = ('fire', 'water', 'wind', 'earth')
model = (java / 'DragonModel.java').read_text()
registration = (java / 'DragonClientRegistration.java').read_text()
renderer = (java / 'DragonRenderer.java').read_text()
assert '@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)' in registration
assert '@SideOnly(Side.CLIENT)' in registration and '@SideOnly(Side.CLIENT)' in renderer
assert '@SideOnly(Side.CLIENT)' in model
assert registration.count('registerEntityRenderingHandler(') == 4
for name in kinds:
    assert 'Entity' + name.capitalize() + 'Dragon.class' in registration
    assert name.upper() + '("' + name + '")' in model
    data = base64.b64decode((assets / (name + '.png.b64')).read_text(), validate=False)
    image = Image.open(io.BytesIO(data))
    image.load()
    assert image.size == (256, 128) and image.mode in ('RGB', 'RGBA')
    assert not (assets / (name + '.png')).exists(), 'duplicate PNG breaks processResources'
    assert len(data) < 20000
    if image.mode == 'RGBA':
        assert image.getchannel('A').getextrema() == (255, 255)
# Validate every static box's vanilla ModelBox UV extent: width 2*(w+d), height h+d.
boxes = re.findall(r'box\([^,]+,\s*(\d+),\s*(\d+),\s*[^,]+,\s*[^,]+,\s*[^,]+,\s*(\d+),\s*(\d+),\s*(\d+)\s*\)', model)
assert len(boxes) >= 25, len(boxes)
for u, v, w, h, d in map(lambda p: tuple(map(int, p)), boxes):
    assert u + 2 * (w + d) <= 256 and v + h + d <= 128, (u, v, w, h, d)
assert len({base64.b64decode((assets / (k + '.png.b64')).read_text()) for k in kinds}) == 4

jdk = Path(os.environ.get('JAVA8_HOME', '/home/daytona/workspace/nexus-test/jdk'))
forge = Path(os.environ.get('FORGE_MAPPED_JAR', '/home/daytona/workspace/nexus-test/gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar'))
assert forge.is_file(), 'Set FORGE_MAPPED_JAR to the 1.12.2 mapped Forge jar'
with tempfile.TemporaryDirectory(prefix='dragon-render-test-') as td:
    tmp = Path(td)
    src = tmp / 'com/nexusrpg/dragons'
    src.mkdir(parents=True)
    (src / 'EntityElementalDragon.java').write_text('package com.nexusrpg.dragons; public abstract class EntityElementalDragon extends net.minecraft.entity.EntityLiving { public EntityElementalDragon(net.minecraft.world.World w) { super(w); } }')
    for k in kinds:
        cl = 'Entity' + k.capitalize() + 'Dragon'
        (src / (cl + '.java')).write_text('package com.nexusrpg.dragons; public abstract class ' + cl + ' extends EntityElementalDragon { public ' + cl + '(net.minecraft.world.World w) { super(w); } }')
    # Use the actual mapped Forge/Minecraft APIs, not stubs for renderer/model classes.
    (tmp / 'classes').mkdir()
    cmd = [str(jdk / 'bin/javac'), '-source', '8', '-target', '8', '-cp', str(forge), '-d', str(tmp / 'classes')]
    cmd += [str(p) for p in sorted(src.glob('*.java'))] + [str(p) for p in sorted(java.glob('*.java'))] + [str(Path(__file__).with_name('DragonModelChecks.java'))]
    subprocess.run(cmd, check=True)
    # The cache supplies vanilla libraries such as Guava; no graphics context is used.
    cache = Path(os.environ.get('GRADLE_USER_HOME', '/home/daytona/workspace/nexus-test/gradle'))
    jars = sorted((cache / 'caches/modules-2/files-2.1').rglob('*.jar'))
    cp = os.pathsep.join([str(tmp / 'classes'), str(forge)] + [str(p) for p in jars])
    subprocess.run([str(jdk / 'bin/java'), '-cp', cp, 'DragonModelChecks'], check=True)
print('PASS: four distinct 256x128 atlases, UV bounds, client-only registration and Java 8 mapped-Forge compile (entity contract stubs).')
