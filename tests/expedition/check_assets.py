#!/usr/bin/env python3
"""Validate our recipe/model/localization links and detect same-input mod recipes."""
import base64, json, struct, zlib
from collections import Counter
from pathlib import Path

root = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/nexusrpg'
names = {'quenching_salve':'quench', 'divers_reserve':'dive', 'lantern_dust':'sight'}
expected = {
    'quenching_salve':['minecraft:snowball','minecraft:slime_ball','minecraft:paper'],
    'divers_reserve':['minecraft:prismarine_shard','minecraft:sugar','minecraft:paper'],
    'lantern_dust':['minecraft:glowstone_dust','minecraft:carrot','minecraft:paper'],
}
for name, kind in names.items():
    recipe = json.loads((root/'recipes'/f'{name}.json').read_text())
    assert recipe['type'] == 'minecraft:crafting_shapeless'
    assert Counter(i['item'] for i in recipe['ingredients']) == Counter(expected[name])
    assert recipe['result'] == {'item':'nexusrpg:'+name}
    model = json.loads((root/'models/item'/f'{name}.json').read_text())
    assert model == {'parent':'item/generated','textures':{'layer0':'nexusrpg:items/'+name}}
    png = base64.b64decode((root/'textures/items'/f'{name}.png.b64').read_bytes(), validate=False)
    assert png.startswith(b'\x89PNG\r\n\x1a\n') and struct.unpack('>II', png[16:24]) == (16,16)
    pos=8
    while pos < len(png):
        length=struct.unpack('>I',png[pos:pos+4])[0]; chunk=png[pos+4:pos+8+length]
        assert zlib.crc32(chunk) & 0xffffffff == struct.unpack('>I',png[pos+8+length:pos+12+length])[0]
        pos += 12+length
    assert pos == len(png)
    for lang in ('en_us','pt_br'):
        lines=(root/'lang'/f'{lang}.lang').read_text().splitlines()
        for key in (f'item.nexusrpg.{name}.name', f'tooltip.nexusrpg.expedition.{kind}'):
            vals=[line for line in lines if line.startswith(key+'=')]
            assert len(vals)==1 and vals[0].split('=',1)[1], (lang,key)
# Detect same multiset of exact vanilla inputs even if another recipe is shaped.
for path in (root/'recipes').glob('*.json'):
    if path.stem in names: continue
    recipe=json.loads(path.read_text())
    ty=recipe.get('type')
    if ty == 'minecraft:crafting_shapeless': entries=recipe.get('ingredients',[])
    elif ty == 'minecraft:crafting_shaped':
        entries=[recipe['key'][c] for row in recipe['pattern'] for c in row if c != ' ']
    else: continue
    # This check only proves no collision for recipes with exact item-only ingredients.
    if not all(isinstance(x,dict) and set(x)=={'item'} for x in entries): continue
    for name, items in expected.items():
        assert Counter(x['item'] for x in entries) != Counter(items), (name,path.name)
print('Expedition assets and exact-item mod recipe collision checks passed')
