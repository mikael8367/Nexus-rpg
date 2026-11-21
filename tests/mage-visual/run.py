#!/usr/bin/env python3
"""Offline JDK 8/mapped Forge checks; no Gradle and no Minecraft/GL launch.
Set JAVA_HOME and GRADLE_USER_HOME to the existing JDK and Forge cache.
"""
import base64
import io
import os
from pathlib import Path
import subprocess
import tempfile
from PIL import Image

root = Path(__file__).resolve().parents[2]
java = root / 'src/main/java/com/nexusrpg/mage'
resources = root / 'src/main/resources/assets/nexusrpg/textures/entity'
encoded = resources / 'tower_mage.png.b64'
data = base64.b64decode(encoded.read_text(), validate=False)
image = Image.open(io.BytesIO(data)); image.load()
assert image.format == 'PNG' and image.size == (64, 64) and image.mode == 'RGBA'
assert image.getextrema()[3] == (255, 255), 'texture has invisible pixels'
assert len(image.getcolors(4096)) > 30, 'texture missing details'
assert not (resources / 'tower_mage.png').exists(), 'duplicate binary would break processResources'
renderer = (java / 'MageClientRenderer.java').read_text()
model = (java / 'ModelTowerMage.java').read_text()
entity = (java / 'EntityTowerMage.java').read_text()
assert '@SideOnly(Side.CLIENT)' in model and '@SideOnly(Side.CLIENT)' in renderer
assert '@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)' in renderer
assert 'registerEntityRenderingHandler(EntityTowerMage.class, MageRender::new)' in renderer
assert 'registerEntityRenderingHandler(EntityMageFireball.class' in renderer
assert 'new ModelTowerMage()' in renderer and '"nexusrpg", "textures/entity/tower_mage.png"' in renderer
assert 'ModelWitch' not in renderer and 'textures/entity/witch.png' not in renderer
assert entity.index('world.setEntityState(this, CAST_ANIMATION_EVENT)') > entity.index('if (!world.spawnEntity(fireball)) return;')
assert 'if (world.isRemote && castAnimationTicks > 0) castAnimationTicks--;' in entity
print('PASS: original PNG, client-only renderer/registry, status and resource wiring', flush=True)

jdk = Path(os.environ['JAVA_HOME'])
cache = Path(os.environ['GRADLE_USER_HOME']) / 'caches'
mapped = cache / 'forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar'
if not mapped.is_file(): raise SystemExit('Missing mapped Forge JAR')
jars = sorted((cache / 'modules-2/files-2.1').rglob('*.jar'))
preferred = ('guava-21.0.jar', 'gson-2.8.0.jar', 'commons-io-2.5.jar')
first = [p for name in preferred for p in jars if p.name == name]
cp = os.pathsep.join(map(str, [mapped] + first + [p for p in jars if p not in first]))
sources = sorted((root / 'src/main/java').rglob('*.java')) + [root / 'tests/mage-visual/MageModelChecks.java']
with tempfile.TemporaryDirectory(prefix='nexus-mage-visual-') as out:
    subprocess.run([str(jdk / 'bin/javac'), '-source', '8', '-target', '8', '-cp', cp, '-d', out] + list(map(str, sources)), check=True)
    print('PASS: all project Java sources compile against mapped Forge with JDK 8', flush=True)
    subprocess.run([str(jdk / 'bin/java'), '-cp', os.pathsep.join([out, str(root / 'src/main/resources'), cp]), 'MageModelChecks'], check=True)
