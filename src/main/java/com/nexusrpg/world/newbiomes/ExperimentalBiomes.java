package com.nexusrpg.world.newbiomes;

import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Registry-only prototypes. They are deliberately NOT in BiomeManager. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class ExperimentalBiomes {
    public static final Biome AMBER = make("amber_steppe", "Estepe de Âmbar", .25F, .18F, 1.0F, .15F, 0xB8A94A, 0);
    public static final Biome MIST = make("mistwood", "Bosque das Brumas", .12F, .25F, .65F, .95F, 0x779F9D, 1);
    public static final Biome CRYSTAL = make("crystal_peaks", "Picos de Cristal", 1.1F, .45F, -.3F, .4F, 0x88C8DB, 2);
    public static final Biome RED = make("red_oasis", "Oásis Rubro", .10F, .10F, 1.5F, .0F, 0x3B9DA6, 3);
    public static final Biome TWILIGHT = make("twilight_fen", "Brejo do Crepúsculo", -.12F, .08F, .8F, .9F, 0x69548A, 4);
    public static final Biome ASH = make("ash_plateau", "Planalto de Cinzas", .9F, .12F, 1.2F, .0F, 0x6F6760, 5);
    private ExperimentalBiomes() {}

    private static Biome make(String id, String name, float height, float variation, float temp,
                              float rain, int water, int style) {
        Biome.BiomeProperties p = new Biome.BiomeProperties(name).setBaseHeight(height)
            .setHeightVariation(variation).setTemperature(temp).setRainfall(rain).setWaterColor(water);
        if (style == 2) p.setSnowEnabled();
        if (style == 3 || style == 5) p.setRainDisabled();
        return new Prototype(p, style).setRegistryName("nexusrpg", id);
    }
    @SubscribeEvent
    public static void register(RegistryEvent.Register<Biome> event) {
        event.getRegistry().registerAll(AMBER, MIST, CRYSTAL, RED, TWILIGHT, ASH);
        // DO NOT add to BiomeManager: new-world safety has not been proven.
    }
    private static final class Prototype extends Biome {
        private final int tint;
        Prototype(BiomeProperties p, int style) {
            super(p);
            tint = new int[] {0xA8A446, 0x75918E, 0xB3CED1, 0x78A556, 0x705477, 0x655C58}[style];
            if (style == 3) { topBlock = Blocks.SAND.getDefaultState(); fillerBlock = Blocks.SANDSTONE.getDefaultState(); }
            if (style == 5) { topBlock = Blocks.GRAVEL.getDefaultState(); fillerBlock = Blocks.STONE.getDefaultState(); }
        }
        @Override public int getGrassColorAtPos(BlockPos pos) { return tint; }
        @Override public int getFoliageColorAtPos(BlockPos pos) { return tint; }
        // Vanilla decoration can reach other chunks and overwrite natural blocks.
        // No generation, no custom IWorldGenerator, and no retrofit hook here.
        @Override public void decorate(World world, Random random, BlockPos origin) { }
    }
}
