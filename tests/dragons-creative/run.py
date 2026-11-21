#!/usr/bin/env python3
"""Static translation regression and Java 8 API compile; not an in-game test."""
import os, re, subprocess, tempfile
from pathlib import Path

root = Path(__file__).resolve().parents[2]
jdk = Path(os.environ.get('JAVA_HOME', '/home/daytona/workspace/nexus-test/jdk'))
cache = Path(os.environ.get('GRADLE_USER_HOME', '/home/daytona/workspace/nexus-test/gradle'))
forge = cache / 'caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12/forge-1.12.2-14.23.5.2860_mapped_snapshot_20171003-1.12.jar'
reg = (root / 'src/main/java/com/nexusrpg/dragons/DragonRegistration.java').read_text()
ids = ('fire_dragon', 'water_dragon', 'wind_dragon', 'earth_dragon')
for name in ids:
    assert re.search(r'\.id\(new ResourceLocation\("nexusrpg", "' + name + r'"\),\s*\d+\).*?\.name\("' + name + r'"\).*?\.egg\(', reg, re.S), name
for lang in ('pt_br', 'en_us'):
    lines = (root / ('src/main/resources/assets/nexusrpg/lang/' + lang + '.lang')).read_text(encoding='utf8').splitlines()
    for name in ids:
        key = 'entity.' + name + '.name='
        matches = [line for line in lines if line.startswith(key)]
        assert len(matches) == 1 and matches[0][len(key):].strip(), (lang, name)
print('PASS: all four registered dragon names translated in both languages, without duplicate keys')
if not forge.exists(): raise SystemExit('Missing mapped Forge jar: ' + str(forge))
with tempfile.TemporaryDirectory(prefix='dragon-creative-') as d:
    base = Path(d)
    for path, source in {
        'com/nexusrpg/NexusRpg.java': 'package com.nexusrpg; public class NexusRpg { public static final String MODID="nexusrpg", NAME="Nexus RPG"; }',
        'com/nexusrpg/init/ModBlocks.java': 'package com.nexusrpg.init; public class ModBlocks { public static net.minecraft.block.Block MITHRIL_ORE; }',
    }.items():
        p = base / path; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(source)
    target = base / 'classes'; target.mkdir()
    subprocess.run([str(jdk/'bin/javac'), '-source', '8', '-target', '8', '-cp', str(forge), '-d', str(target)] + [str(p) for p in base.rglob('*.java')] + [str(root/'src/main/java/com/nexusrpg/init/NexusCreativeTab.java')], check=True)
    print('PASS: Java 8 compile against mapped Forge 1.12.2 (two project classes stubbed)')
