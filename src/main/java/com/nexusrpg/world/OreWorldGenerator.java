package com.nexusrpg.world;

import com.nexusrpg.init.ModBlocks;
import java.util.Random;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

/** Gera veios apenas no Overworld, substituindo pedra natural. */
public final class OreWorldGenerator implements IWorldGenerator {
    private final WorldGenMinable mithril = new WorldGenMinable(ModBlocks.MITHRIL_ORE.getDefaultState(), 5);
    private final WorldGenMinable adamantite = new WorldGenMinable(ModBlocks.ADAMANTITE_ORE.getDefaultState(), 4);
    private final WorldGenMinable draconite = new WorldGenMinable(ModBlocks.DRACONITE_ORE.getDefaultState(), 3);

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() != 0) return;
        generateVeins(mithril, world, random, chunkX, chunkZ, 6, 8, 48);
        generateVeins(adamantite, world, random, chunkX, chunkZ, 3, 5, 28);
        generateVeins(draconite, world, random, chunkX, chunkZ, 1, 4, 16);
    }

    private static void generateVeins(WorldGenMinable generator, World world, Random random,
                                      int chunkX, int chunkZ, int attempts, int minY, int maxY) {
        for (int i = 0; i < attempts; i++) {
            int x = chunkX * 16 + random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY + 1);
            int z = chunkZ * 16 + random.nextInt(16);
            generator.generate(world, random, new net.minecraft.util.math.BlockPos(x, y, z));
        }
    }
}
