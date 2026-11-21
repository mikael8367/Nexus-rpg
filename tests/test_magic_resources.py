import base64
import io
import json
import unittest
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/nexusrpg'
STAFFS = {'arcane_staff': 'minecraft:diamond', 'healing_staff': 'minecraft:golden_apple',
          'ward_staff': 'minecraft:iron_ingot', 'swift_staff': 'minecraft:feather',
          'flameward_staff': 'minecraft:magma_cream', 'nightvision_staff': 'minecraft:golden_carrot',
          'waterbreathing_staff': 'minecraft:fish'}

class MagicResourcesTest(unittest.TestCase):
    def test_recipes(self):
        for staff, ingredient in STAFFS.items():
            with self.subTest(staff=staff):
                recipe = json.loads((ASSETS / 'recipes' / (staff + '.json')).read_text())
                self.assertEqual(recipe['type'], 'minecraft:crafting_shaped')
                self.assertEqual(recipe['pattern'], [' G ', ' RS', 'S  '])
                self.assertEqual(recipe['key']['G']['item'], ingredient)
                if staff == 'waterbreathing_staff': self.assertEqual(recipe['key']['G']['data'], 3)
                self.assertEqual(recipe['key']['R']['item'], 'minecraft:redstone')
                self.assertEqual(recipe['key']['S']['item'], 'minecraft:stick')
                self.assertEqual(recipe['result']['item'], 'nexusrpg:' + staff)

    def test_models_and_textures(self):
        for staff in STAFFS:
            with self.subTest(staff=staff):
                model = json.loads((ASSETS / 'models/item' / (staff + '.json')).read_text())
                self.assertEqual(model['textures']['layer0'], 'nexusrpg:items/' + staff)
                data = base64.b64decode((ASSETS / 'textures/items' / (staff + '.png.b64')).read_text(), validate=False)
                image = Image.open(io.BytesIO(data))
                image.verify()
                self.assertEqual(image.size, (16, 16))

    def test_names_and_tooltips(self):
        for language in ['pt_br', 'en_us']:
            entries = {}
            for line in (ASSETS / 'lang' / (language + '.lang')).read_text(encoding='utf8').splitlines():
                if line and not line.startswith('#') and '=' in line:
                    key, val = line.split('=', 1)
                    self.assertNotIn(key, entries, 'Duplicate translation ' + key)
                    entries[key] = val
            for staff in STAFFS:
                self.assertTrue(entries.get('item.nexusrpg.' + staff + '.name'))
            for key in ['arcane', 'heal', 'ward', 'swift', 'flameward', 'nightvision', 'waterbreathing', 'cost']:
                self.assertTrue(entries.get('tooltip.nexusrpg.magic.' + key))

if __name__ == '__main__':
    unittest.main()
