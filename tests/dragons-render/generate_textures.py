#!/usr/bin/env python3
"""Generate small deterministic, opaque pixel-art dragon atlases. Requires Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw
import base64, io, random

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'src/main/resources/assets/nexusrpg/textures/entity/dragons'
OUT.mkdir(parents=True, exist_ok=True)
PALETTES = {
    'fire':  ((125, 30, 21), (220, 71, 27), (250, 169, 55), (75, 23, 21), (251, 226, 125)),
    'water': ((20, 82, 104), (38, 150, 165), (126, 220, 207), (13, 56, 78), (219, 252, 160)),
    'wind':  ((121, 151, 167), (193, 219, 220), (242, 247, 222), (76, 108, 133), (255, 205, 80)),
    'earth': ((78, 75, 46), (134, 131, 70), (189, 171, 99), (53, 54, 38), (239, 202, 83)),
}
for index, (name, (base, light, accent, dark, eye)) in enumerate(PALETTES.items()):
    rng = random.Random(303 + index)
    img = Image.new('RGB', (256, 128), base)
    d = ImageDraw.Draw(img)
    # Overlapping, angular scales on the skin regions. All pixels stay opaque.
    for y in range(0, 80, 5):
        for x in range(-4 + (3 if y % 10 else 0), 256, 8):
            delta = rng.randint(-11, 11)
            c = tuple(max(0, min(255, v + delta)) for v in base)
            d.polygon([(x, y), (x+4, y+1), (x+7, y+3), (x+4, y+5), (x, y+4)], fill=c)
            d.line([(x+1, y+4), (x+4, y+5), (x+7, y+3)], fill=dark)
            if rng.random() < .25:
                d.point((x+3, y+1), fill=light)
    # Atlas area for wing membrane, with diagonal rib / scale patterns.
    d.rectangle((0, 80, 110, 110), fill=light)
    for x in range(-30, 130, 11):
        d.line((x, 80, x+20, 110), fill=base, width=1)
        d.line((x+20, 80, x, 110), fill=accent, width=1)
    # Bone / wing spars.
    d.rectangle((120, 80, 185, 123), fill=dark)
    for x in range(120, 186, 4):
        d.line((x, 80, x, 123), fill=base)
    # Crests and horns, contrasting to the body.
    d.rectangle((192, 0, 255, 66), fill=accent)
    for y in range(0, 66, 5):
        d.line((192, y, 255, y+9), fill=light)
    # All six faces of each eye cube sample this solid, bright patch.
    d.rectangle((218, 68, 235, 79), fill=eye)
    buf = io.BytesIO()
    img.save(buf, format='PNG', optimize=True)
    (OUT / (name + '.png.b64')).write_text(base64.b64encode(buf.getvalue()).decode('ascii') + '\n')
    print(name, len(buf.getvalue()), 'bytes')
