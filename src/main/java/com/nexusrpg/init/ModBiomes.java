package com.nexusrpg.init;

import java.util.List;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.BiomeManager.BiomeType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Biomas próprios: registro, distribuição natural e decoração vanilla apropriada. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class ModBiomes {
    private static final Logger LOGGER = LogManager.getLogger("Nexus RPG Biomes");

    // 1.12 rejects temperatures strictly between 0.1 and 0.2 at construction.
    public static final Biome ENCHANTED_GROVE = create("enchanted_grove", "Bosque das Runas", 0.25F, 0.85F, 0.20F, 0.12F, 0x36B86A);
    public static final Biome FROST_CROWN = create("frost_crown", "Coroa Boreal", -0.35F, 0.25F, 0.05F, 0.08F, 0x75D9E8);
    public static final Biome SUNKEN_EMPIRE = create("sunken_empire", "Império Soterrado", 0.20F, 0.0F, 0.20F, 0.02F, 0xD9A52B);
    public static final Biome WITCHING_MIRE = create("witching_mire", "Charco das Bruxas", -0.05F, 0.35F, 0.28F, 0.95F, 0x7950A8);
    public static final Biome CINDER_HEART = create("cinder_heart", "Coração de Cinzas", 0.55F, 0.15F, 0.45F, 0.0F, 0xD94B32);
    public static final Biome ROSA_SANCTUM = create("rosa_sanctum", "Santuário Rosado", 0.15F, 0.75F, 0.20F, 0.75F, 0xE58BB8);
    public static final Biome UMBRAL_REACH = create("umbral_reach", "Ermos do Umbral", 0.65F, 0.05F, 0.55F, 0.0F, 0x29243D);
    public static final Biome TITAN_CANOPY = create("titan_canopy", "Dossel dos Titãs", 0.35F, 0.90F, 0.38F, 0.85F, 0xE58A35);

    private ModBiomes() {}

    private static Biome create(String id, String name, float baseHeight, float heightVariation,
                                float temperature, float rainfall, int waterColor) {
        Biome.BiomeProperties properties = new Biome.BiomeProperties(name)
            .setBaseHeight(baseHeight).setHeightVariation(heightVariation)
            .setTemperature(temperature).setRainfall(rainfall).setWaterColor(waterColor);
        if ("frost_crown".equals(id)) properties.setSnowEnabled();
        if ("sunken_empire".equals(id) || "cinder_heart".equals(id) || "umbral_reach".equals(id)) {
            properties.setRainDisabled();
        }
        return new NexusBiome(id, properties).setRegistryName("nexusrpg", id);
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        event.getRegistry().registerAll(ENCHANTED_GROVE, FROST_CROWN, SUNKEN_EMPIRE, WITCHING_MIRE,
            CINDER_HEART, ROSA_SANCTUM, UMBRAL_REACH, TITAN_CANOPY);

        // Entries are additive; existing vanilla and other mods' entries are never removed.
        add(ENCHANTED_GROVE, BiomeType.COOL, Type.FOREST, Type.MAGICAL, Type.LUSH);
        add(FROST_CROWN, BiomeType.ICY, Type.COLD, Type.SNOWY, Type.CONIFEROUS, Type.SPARSE);
        add(SUNKEN_EMPIRE, BiomeType.DESERT, Type.DRY, Type.SANDY, Type.SPARSE);
        add(WITCHING_MIRE, BiomeType.WARM, Type.SWAMP, Type.WET, Type.SPOOKY);
        add(CINDER_HEART, BiomeType.DESERT, Type.DRY, Type.WASTELAND, Type.HILLS, Type.SPARSE);
        add(ROSA_SANCTUM, BiomeType.COOL, Type.FOREST, Type.MAGICAL, Type.LUSH);
        add(UMBRAL_REACH, BiomeType.WARM, Type.DRY, Type.WASTELAND, Type.SPOOKY, Type.SPARSE);
        add(TITAN_CANOPY, BiomeType.WARM, Type.FOREST, Type.DENSE, Type.HILLS);
        LOGGER.info("Oito biomas Nexus registrados e adicionados ao BiomeManager.");
    }

    /** Somente diagnostico: nao consulta nem carrega chunks e nao altera a geracao. */
    @SubscribeEvent
    public static void reportOverworldBiomes(WorldEvent.Load event) {
        if (event.getWorld().isRemote || event.getWorld().provider.getDimension() != 0) return;
        LOGGER.info("Diagnostico dos biomas Nexus no Overworld; biome provider: {}",
            event.getWorld().getBiomeProvider().getClass().getName());
        report(ENCHANTED_GROVE, BiomeType.COOL);
        report(FROST_CROWN, BiomeType.ICY);
        report(SUNKEN_EMPIRE, BiomeType.DESERT);
        report(WITCHING_MIRE, BiomeType.WARM);
        report(CINDER_HEART, BiomeType.DESERT);
        report(ROSA_SANCTUM, BiomeType.COOL);
        report(UMBRAL_REACH, BiomeType.WARM);
        report(TITAN_CANOPY, BiomeType.WARM);
    }

    private static void report(Biome biome, BiomeType climate) {
        boolean registered = ForgeRegistries.BIOMES.getValue(biome.getRegistryName()) == biome;
        int entries = 0;
        int weight = 0;
        List<BiomeEntry> biomes = BiomeManager.getBiomes(climate);
        if (biomes != null) {
            for (BiomeEntry entry : biomes) {
                if (entry.biome == biome) {
                    entries++;
                    weight += entry.itemWeight;
                }
            }
        }
        boolean hasTypes = registered && BiomeDictionary.hasAnyType(biome);
        if (!registered || entries == 0 || weight <= 0 || !hasTypes) {
            LOGGER.warn("{} registro={} clima={} entradas={} peso={} temTipos={}",
                biome.getRegistryName(), registered, climate, entries, weight, hasTypes);
        } else {
            LOGGER.info("{} registro={} id={} clima={} entradas={} peso={} tipos={}",
                biome.getRegistryName(), true, Biome.getIdForBiome(biome), climate, entries,
                weight, BiomeDictionary.getTypes(biome));
        }
    }

    private static void add(Biome biome, BiomeType climate, Type... types) {
        BiomeDictionary.addTypes(biome, types);
        BiomeManager.addBiome(climate, new BiomeEntry(biome, 4));
    }

    private static class NexusBiome extends Biome {
        private static final WorldGenAbstractTree SPRUCE = new WorldGenTaiga2(false);
        private final String id;

        NexusBiome(String id, BiomeProperties properties) {
            super(properties);
            this.id = id;
            if ("enchanted_grove".equals(id)) {
                decorator.treesPerChunk = 7; decorator.flowersPerChunk = 5; decorator.grassPerChunk = 6;
            } else if ("frost_crown".equals(id)) {
                decorator.treesPerChunk = 2; decorator.flowersPerChunk = 0; decorator.grassPerChunk = 1;
            } else if ("sunken_empire".equals(id)) {
                topBlock = Blocks.SAND.getDefaultState(); fillerBlock = Blocks.SAND.getDefaultState();
                decorator.treesPerChunk = -999; decorator.flowersPerChunk = 0; decorator.grassPerChunk = 0;
                decorator.deadBushPerChunk = 2; decorator.cactiPerChunk = 2;
            } else if ("witching_mire".equals(id)) {
                decorator.treesPerChunk = 4; decorator.flowersPerChunk = 1; decorator.grassPerChunk = 5;
                decorator.mushroomsPerChunk = 4; decorator.waterlilyPerChunk = 4;
            } else if ("cinder_heart".equals(id)) {
                topBlock = Blocks.GRAVEL.getDefaultState(); fillerBlock = Blocks.STONE.getDefaultState();
                decorator.treesPerChunk = -999; decorator.flowersPerChunk = 0; decorator.grassPerChunk = 0;
            } else if ("rosa_sanctum".equals(id)) {
                decorator.treesPerChunk = 5; decorator.flowersPerChunk = 10; decorator.grassPerChunk = 6;
            } else if ("umbral_reach".equals(id)) {
                topBlock = Blocks.STONE.getDefaultState(); fillerBlock = Blocks.STONE.getDefaultState();
                decorator.treesPerChunk = -999; decorator.flowersPerChunk = 0; decorator.grassPerChunk = 0;
            } else if ("titan_canopy".equals(id)) {
                decorator.treesPerChunk = 10; decorator.flowersPerChunk = 2; decorator.grassPerChunk = 8;
            }
        }

        @Override
        public WorldGenAbstractTree getRandomTreeFeature(Random random) {
            if ("frost_crown".equals(id)) return SPRUCE;
            if ("witching_mire".equals(id)) return SWAMP_FEATURE;
            if ("titan_canopy".equals(id) && random.nextInt(3) == 0) return BIG_TREE_FEATURE;
            return super.getRandomTreeFeature(random);
        }

        @Override
        public int getGrassColorAtPos(BlockPos pos) {
            if ("witching_mire".equals(id)) return 0x685F59;
            if ("rosa_sanctum".equals(id)) return 0xC57B99;
            if ("umbral_reach".equals(id)) return 0x36313F;
            return super.getGrassColorAtPos(pos);
        }

        @Override
        public int getFoliageColorAtPos(BlockPos pos) {
            if ("witching_mire".equals(id)) return 0x66575F;
            if ("rosa_sanctum".equals(id)) return 0xD68BAB;
            if ("umbral_reach".equals(id)) return 0x3B3345;
            return super.getFoliageColorAtPos(pos);
        }
    }
}
