"""Source-level regression checks only; these do not execute Minecraft or Forge."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / 'src/main/java/com/nexusrpg/init/ModBiomes.java').read_text(encoding='utf-8')
EXPECTED = {
    'ENCHANTED_GROVE': 'COOL', 'FROST_CROWN': 'ICY', 'SUNKEN_EMPIRE': 'DESERT',
    'WITCHING_MIRE': 'WARM', 'CINDER_HEART': 'DESERT', 'ROSA_SANCTUM': 'COOL',
    'UMBRAL_REACH': 'WARM', 'TITAN_CANOPY': 'WARM', 'MAGMA_WASTES': 'DESERT',
}

class BiomeSourceChecks(unittest.TestCase):
    def test_unique_biome_ids_and_register_all(self):
        defined = dict(re.findall(r'public static final Biome (\w+) = create\("([a-z_]+)"', SOURCE))
        self.assertEqual(set(defined), set(EXPECTED))
        self.assertEqual(len(set(defined.values())), 9)
        block = re.search(r'event\.getRegistry\(\)\.registerAll\((.*?)\);', SOURCE, re.S).group(1)
        self.assertEqual(set(re.findall(r'[A-Z_]+', block)), set(EXPECTED))
        self.assertEqual(len(re.findall(r'[A-Z_]+', block)), 9)

    def test_additions_have_climate_and_types(self):
        calls = re.findall(r'add\((\w+), BiomeType\.(\w+), ([^;]+)\);', SOURCE)
        self.assertEqual(len(calls), 9)
        self.assertEqual({biome: climate for biome, climate, types in calls}, EXPECTED)
        self.assertTrue(all('Type.' in types for _, _, types in calls))
        self.assertIn('new BiomeEntry(biome, 4)', SOURCE)
        self.assertNotRegex(SOURCE, r'BiomeManager\.remove|BiomeManager\.getBiomes\([^)]*\)\.clear')
        self.assertLess(SOURCE.index('registerAll('), SOURCE.index('add(ENCHANTED_GROVE'))

    def test_diagnostics_cover_all_climates_without_chunk_queries(self):
        calls = re.findall(r'report\((\w+), BiomeType\.(\w+)\);', SOURCE)
        self.assertEqual(len(calls), 9)
        self.assertEqual(dict(calls), EXPECTED)
        diagnostic = SOURCE.split('public static void reportOverworldBiomes', 1)[1].split('private static void add(', 1)[0]
        for forbidden in ('getChunk(', 'getChunkFrom', 'getBiome(', 'setBlockState(', 'setBiome'):
            self.assertNotIn(forbidden, diagnostic)
        self.assertIn('isRemote', diagnostic)
        self.assertIn('getDimension() != 0', diagnostic)
        self.assertIn('BiomeDictionary.hasAnyType(biome)', diagnostic)

    def test_subscriber_and_overworld_materials_are_present(self):
        self.assertIn('@Mod.EventBusSubscriber(modid = "nexusrpg")', SOURCE)
        self.assertRegex(SOURCE, r'@SubscribeEvent\s+public static void registerBiomes')
        self.assertRegex(SOURCE, r'@SubscribeEvent\s+public static void reportOverworldBiomes')
        for block in ('Blocks.SAND', 'Blocks.GRAVEL', 'Blocks.STONE'):
            self.assertIn(block, SOURCE)
        self.assertIn('properties.setSnowEnabled()', SOURCE)

if __name__ == '__main__':
    unittest.main()
