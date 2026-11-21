#!/usr/bin/env python3
"""Resource invariants + Java 8 mapped Forge compile + actual model and loot loader.
Usage: JAVA_HOME=... GRADLE_USER_HOME=... python3 tests/creatures1516/check.py NEW_OUTPUT_DIR
No caches or pre-existing output are modified. Does not run Minecraft gameplay.
"""
from pathlib import Path
from io import BytesIO
import os, sys, json, base64, subprocess, hashlib
from PIL import Image
root=Path(__file__).resolve().parents[2]
if len(sys.argv)!=2: raise SystemExit(__doc__)
out=Path(sys.argv[1]).resolve()
if out.exists(): raise SystemExit('output already exists')
java=root/'src/main/java/com/nexusrpg/mobs'
assets=root/'src/main/resources/assets/nexusrpg'
doc=(root/'CRIATURAS_15_16.md').read_text()
expected={'dune_burrower':('Burrower',15,'sand','flint'),'root_warden':('Rootwarden',16,'stick','wheat_seeds')}
imgs=[]
for name,(cls,id,common,extra) in expected.items():
    entity=(java/f'Entity{cls}.java').read_text()
    assert f'"entities/{name}"' in entity and 'extends EntityNexusMob' in entity
    assert 'attackEntityAsMob' not in entity and 'Potion' not in entity
    assert f'new ResourceLocation("nexusrpg", "{name}"), {id}' in doc
    assert f'new Model{cls}()' in doc and f'entity.{name}.name=' in doc
    data=json.loads((assets/'loot_tables/entities'/f'{name}.json').read_text()); p=data['pools']
    assert len(p)==2 and p[0]['name']==name+'_common' and p[1]['name']==name+'_extra'
    assert p[0]['entries'][0]['name']=='minecraft:'+common and p[1]['entries'][0]['name']=='minecraft:'+extra
    assert p[0]['entries'][0]['functions']==[{'function':'minecraft:set_count','count':{'min':1,'max':2}}]
    assert p[1]['conditions']==[{'condition':'minecraft:random_chance','chance':0.25}]
    png=base64.b64decode((assets/'textures/entity/mobs'/f'{name}.png.b64').read_bytes(),validate=False)
    im=Image.open(BytesIO(png)); im.load(); assert im.format=='PNG' and im.size==(64,64) and im.mode=='RGBA'
    assert im.getextrema()[3]==(255,255); imgs.append(hashlib.sha256(png).digest())
assert imgs[0]!=imgs[1]
print('PASS resources, exact IDs, loot, no extra hit override, distinct 64x64 PNG',flush=True)
cache=Path(os.environ['GRADLE_USER_HOME'])/'caches'; jdk=Path(os.environ['JAVA_HOME'])
mapped=cache/'forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar'
if not mapped.is_file(): raise SystemExit('missing mapped forge jar')
jars=sorted((cache/'modules-2/files-2.1').rglob('*.jar')); preferred=('guava-21.0.jar','gson-2.8.0.jar','commons-io-2.5.jar'); first=[p for name in preferred for p in jars if p.name==name]
cp=os.pathsep.join(map(str,[mapped]+first+[p for p in jars if p not in first]))
out.mkdir(parents=True)
sources=[java/'EntityNexusMob.java']+[java/f'Entity{c}.java' for c in ('Burrower','Rootwarden')]+[java/'client'/f'Model{c}.java' for c in ('Burrower','Rootwarden')]+[root/'tests/creatures1516/CreatureChecks.java']
subprocess.run([str(jdk/'bin/javac'),'-source','8','-target','8','-cp',cp,'-d',str(out)]+list(map(str,sources)),check=True)
print('PASS new Java sources compiled using Java 8 and mapped Forge',flush=True)
runtime=os.pathsep.join([str(out),str(root/'src/main/resources'),cp]); subprocess.run([str(jdk/'bin/java'),'-cp',runtime,'CreatureChecks'],check=True)
