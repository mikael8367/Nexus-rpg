#!/usr/bin/env python3
"""Recreate the three procedural textures. CAUTION: overwrites their .png.b64 files."""
from pathlib import Path
import base64, io, random
from PIL import Image
root = Path(__file__).resolve().parent
tex = root/'src/main/resources/assets/nexusrpg/textures/entity/mobs'
tex.mkdir(parents=True, exist_ok=True)
variants = [
    ('bramble_hound', (56,82,41), (95,125,57), (187,151,66)),
    ('cavern_crawler', (37,42,65), (76,80,105), (88,198,209)),
    ('basalt_guardian', (52,54,59), (91,92,95), (230,124,45)),
]
for name, base, light, accent in variants:
    random.seed(name)
    im = Image.new('RGBA', (64,64))
    for y in range(64):
        for x in range(64):
            v = random.randrange(-8,9)
            color = light if ((x//5+y//7)%4 == 0) else base
            if (x*3+y*5)%53 == 0: color = accent
            im.putpixel((x,y), tuple(max(0,min(255,c+v)) for c in color)+(255,))
    for y in range(48,54):
        for x in range(48,56): im.putpixel((x,y), accent+(255,))
    out=io.BytesIO(); im.save(out,format='PNG')
    (tex/(name+'.png.b64')).write_text(base64.b64encode(out.getvalue()).decode()+'\n')
