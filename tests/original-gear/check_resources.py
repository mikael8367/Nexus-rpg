#!/usr/bin/env python3
"""Auditoria leve dos recursos dos novos equipamentos, sem Gradle nem cliente."""
import base64
import json
from pathlib import Path
import struct
import zlib

root = Path(__file__).resolve().parents[2]
a = root / 'src/main/resources/assets/nexusrpg'
tiers = ('auronite', 'umbrilite')
parts = ('ingot', 'sword', 'pickaxe', 'axe', 'shovel', 'hoe', 'helmet', 'chestplate', 'leggings', 'boots')
ids = {t + '_' + p for t in tiers for p in ('ore',) + parts}

def load(path):
    return json.loads(path.read_text(encoding='utf-8'))

def png(path, size):
    data = base64.b64decode(''.join(path.read_text().split()), validate=True)
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    offset, chunks, compressed = 8, [], bytearray()
    while offset < len(data):
        n = struct.unpack('>I', data[offset:offset+4])[0]
        kind, body = data[offset+4:offset+8], data[offset+8:offset+8+n]
        crc = struct.unpack('>I', data[offset+8+n:offset+12+n])[0]
        assert zlib.crc32(kind + body) & 0xffffffff == crc, path
        chunks.append(kind)
        if kind == b'IHDR': assert struct.unpack('>II', body[:8]) == size, path
        if kind == b'IDAT': compressed.extend(body)
        offset += n + 12
        if kind == b'IEND': break
    assert offset == len(data) and chunks[0] == b'IHDR' and chunks[-1] == b'IEND', path
    assert zlib.decompress(compressed), path

for tier in tiers:
    for part in ('ore',) + parts:
        name = tier + '_' + part
        model = load(a / 'models/item' / (name + '.json'))
        if part == 'ore':
            assert model['parent'] == 'nexusrpg:block/' + name
            assert load(a / 'blockstates' / (name + '.json'))['variants']['normal']['model'] == 'nexusrpg:' + name
            assert load(a / 'models/block' / (name + '.json'))['textures']['all'] == 'nexusrpg:blocks/' + name
            png(a / 'textures/blocks' / (name + '.png.b64'), (16, 16))
        else:
            assert model['textures']['layer0'] == 'nexusrpg:items/' + name
            png(a / 'textures/items' / (name + '.png.b64'), (16, 16))
        recipe = load(a / 'recipes' / (name + '.json'))
        assert recipe['result']['item'] == 'nexusrpg:' + name
        if recipe['type'] == 'minecraft:crafting_shaped':
            pattern = recipe['pattern']
            assert 1 <= len(pattern) <= 3 and len({len(x) for x in pattern}) == 1 and 1 <= len(pattern[0]) <= 3
            assert set(''.join(pattern)) - {' '} == set(recipe['key'])
            ingredients = list(recipe['key'].values())
        else:
            assert recipe['type'] == 'minecraft:crafting_shapeless'
            ingredients = recipe['ingredients']
            assert 1 <= len(ingredients) <= 9
        for ingredient in ingredients:
            item = ingredient['item']
            assert item.startswith('minecraft:') or item.removeprefix('nexusrpg:') in ids, (name, item)
    for layer in ('1', '2'):
        png(a / 'textures/models/armor' / (tier + '_layer_' + layer + '.png.b64'), (64, 32))
for lang in ('pt_br', 'en_us'):
    lines = (a / 'lang' / (lang + '.lang')).read_text(encoding='utf-8').splitlines()
    for name in ids:
        key = ('tile.' if name.endswith('_ore') else 'item.') + 'nexusrpg.' + name + '.name='
        assert sum(line.startswith(key) and bool(line[len(key):]) for line in lines) == 1, (lang, name)

# Explicit progression and conservation: craft one ore for one ingot + stone;
# one ore yields one ingot when smelted (no duplication loop).
for tier in tiers:
    ore = load(a / 'recipes' / (tier + '_ore.json'))
    assert sorted(x['item'] for x in ore['ingredients']) == ['minecraft:stone', 'nexusrpg:' + tier + '_ingot']
    assert ore['result'].get('count', 1) == 1
for tier, expected in {
    'auronite': ['minecraft:iron_ingot', 'minecraft:iron_ingot', 'minecraft:gold_ingot', 'minecraft:redstone'],
    'umbrilite': ['nexusrpg:auronite_ingot', 'nexusrpg:auronite_ingot', 'minecraft:ender_pearl', 'minecraft:obsidian'],
}.items():
    recipe = load(a / 'recipes' / (tier + '_ingot.json'))
    assert sorted(i['item'] for i in recipe['ingredients']) == sorted(expected)
    assert recipe['result']['count'] == 2

# Compare each new shaped recipe with the matching existing template, and
# check exact duplicate shapeless ingredients against every project recipe.
for tier in tiers:
    for part in parts[1:]:
        new = load(a / 'recipes' / (tier + '_' + part + '.json'))
        old = load(a / 'recipes' / ('mithril_' + part + '.json'))
        expected = json.loads(json.dumps(old).replace('mithril', tier))
        assert new == expected, (tier, part)
for name in (t + '_' + p for t in tiers for p in ('ore', 'ingot')):
    new = load(a / 'recipes' / (name + '.json'))
    signature = sorted(json.dumps(i, sort_keys=True) for i in new['ingredients'])
    for path in (a / 'recipes').glob('*.json'):
        other = load(path)
        if path.stem != name and other.get('type') == 'minecraft:crafting_shapeless':
            assert signature != sorted(json.dumps(i, sort_keys=True) for i in other['ingredients']), (name, path.name)
java = (root / 'src/main/java/com/nexusrpg/gear/OriginalGear.java').read_text()
models = (root / 'src/main/java/com/nexusrpg/gear/OriginalGearModels.java').read_text()
entry = (root / 'src/main/java/com/nexusrpg/NexusRpg.java').read_text()
assert '@Mod.EventBusSubscriber' in java and 'registerBlocks(' in java and 'registerItems(' in java
assert 'value = Side.CLIENT' in models and 'Item.getItemFromBlock(block)' in models
assert 'OriginalGear.registerSmelting()' in entry
assert java.count('GameRegistry.addSmelting(') == 2
assert 'registerWorldGenerator' not in java and 'WorldGen' not in java
print('PASS: 22 IDs, JSON recipes/models, PNG CRC/dimensions, translations, registries and smelting wiring')
