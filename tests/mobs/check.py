#!/usr/bin/env python3
"""Resource and registry invariants; does not start Minecraft."""
from pathlib import Path
from io import BytesIO
import base64, hashlib, re
from PIL import Image
root = Path(__file__).resolve().parents[2]
java = root/'src/main/java/com/nexusrpg'
reg = (java/'mobs/MobRegistration.java').read_text()
expected = {'bramble_hound':7, 'cavern_crawler':8, 'basalt_guardian':9, 'reed_stilt':10, 'prism_tortoise':11, 'lumen_moth':12, 'dune_mole':13, 'bellback':14, 'dune_burrower':15, 'root_warden':16}
entries = re.findall(r'new ResourceLocation\("nexusrpg", "([a-z_]+)"\),\s*(\d+)\).*?\.egg\((0x[0-9A-F]+),\s*(0x[0-9A-F]+)\)', reg)
assert {n:int(i) for n,i,_,_ in entries} == expected, entries
assert len(entries) == len(expected) and all(a != b for _,_,a,b in entries)
assert len({i for _,i,_,_ in entries}) == len(entries)
for p in java.rglob('*.java'):
    if p.name == 'MobRegistration.java': continue
    for i in re.findall(r'\.id\(new ResourceLocation\([^)]*\),\s*(\d+)\)',p.read_text()):
        assert int(i) not in expected.values(), (p,i)
render = (java/'mobs/client/MobClientRegistration.java').read_text()
assert 'value = Side.CLIENT' in render
for p in (java/'mobs').glob('*.java'):
    text = p.read_text()
    assert 'net.minecraft.client' not in text and 'com.nexusrpg.mobs.client' not in text
assert 'EntityAIAttackMelee' in (java/'mobs/EntityNexusMob.java').read_text()
textures=[]
for name in expected:
    assert '"'+name+'"' in render
    raw=base64.b64decode((root/f'src/main/resources/assets/nexusrpg/textures/entity/mobs/{name}.png.b64').read_text(),validate=False)
    image=Image.open(BytesIO(raw)); image.load(); assert image.size == (64,64) and image.format == 'PNG'
    assert len(set(image.getdata())) > 20
    textures.append(hashlib.sha256(raw).digest())
    for lang in ['en_us','pt_br']:
        assert f'entity.{name}.name=' in (root/f'src/main/resources/assets/nexusrpg/lang/{lang}.lang').read_text(encoding='utf8')
assert len(set(textures)) == len(expected)
for cls, name in [('LumenMoth','lumen_moth'), ('DuneMole','dune_mole'), ('Bellback','bellback'), ('Burrower','dune_burrower'), ('Rootwarden','root_warden')]:
    source=(java/f'mobs/Entity{cls}.java').read_text()
    assert f'extends EntityNexusMob' in source and f'entities/{name}' in source
    assert all(word not in source for word in ('attackEntityAsMob','addPotionEffect','setBlockState','spawnEntity','NBTTag','onUpdate','addSpawn'))
    assert f'Entity{cls}.class, m -> new MobRenderer<Entity{cls}>(m, new Model{cls}(), "{name}")' in render
    for p in java.rglob('*.java'):
        assert not ('addSpawn' in p.read_text() and f'Entity{cls}' in p.read_text()), p
print('PASS: IDs, egg declarations, client isolation, translations and ten valid distinct textures')
