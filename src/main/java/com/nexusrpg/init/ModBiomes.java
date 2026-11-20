package com.nexusrpg.init;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Oito biomas originais do Nexus RPG.
 * Cada um possui identidade climática, relevo e cor de água próprios.
 * A geração de estruturas, plantas e blocos exclusivos será adicionada em etapas seguintes.
 */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class ModBiomes {
    public static final Biome ENCHANTED_GROVE = create("enchanted_grove", "Bosque das Runas", 0.25F, 0.85F, 0.18F, 0.12F, 0x36B86A);
    public static final Biome FROST_CROWN = create("frost_crown", "Coroa Boreal", -0.35F, 0.25F, 0.05F, 0.08F, 0x75D9E8);
    public static final Biome SUNKEN_EMPIRE = create("sunken_empire", "Império Soterrado", 0.20F, 0.0F, 0.12F, 0.02F, 0xD9A52B);
    public static final Biome WITCHING_MIRE = create("witching_mire", "Charco das Bruxas", -0.05F, 0.35F, 0.28F, 0.95F, 0x7950A8);
    public static final Biome CINDER_HEART = create("cinder_heart", "Coração de Cinzas", 0.55F, 0.15F, 0.45F, 0.0F, 0xD94B32);
    public static final Biome ROSA_SANCTUM = create("rosa_sanctum", "Santuário Rosado", 0.15F, 0.75F, 0.12F, 0.75F, 0xE58BB8);
    public static final Biome UMBRAL_REACH = create("umbral_reach", "Ermos do Umbral", 0.65F, 0.05F, 0.55F, 0.0F, 0x29243D);
    public static final Biome TITAN_CANOPY = create("titan_canopy", "Dossel dos Titãs", 0.35F, 0.90F, 0.38F, 0.85F, 0xE58A35);

    private ModBiomes() {}

    private static Biome create(String id, String name, float baseHeight, float heightVariation,
                                float temperature, float rainfall, int waterColor) {
        Biome.BiomeProperties properties = new Biome.BiomeProperties(name)
            .setBaseHeight(baseHeight)
            .setHeightVariation(heightVariation)
            .setTemperature(temperature)
            .setRainfall(rainfall)
            .setWaterColor(waterColor);
        return new NexusBiome(properties).setRegistryName("nexusrpg", id);
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        event.getRegistry().registerAll(
            ENCHANTED_GROVE, FROST_CROWN, SUNKEN_EMPIRE, WITCHING_MIRE,
            CINDER_HEART, ROSA_SANCTUM, UMBRAL_REACH, TITAN_CANOPY
        );
    }

    /** Classe-base para permitir evolução da geração de superfície sem copiar biomas vanilla. */
    private static class NexusBiome extends Biome {
        NexusBiome(BiomeProperties properties) {
            super(properties);
        }
    }
}
