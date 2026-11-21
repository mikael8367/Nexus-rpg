package com.nexusrpg.dragons.world;

import com.nexusrpg.dragons.EntityFireDragon;
import com.nexusrpg.dragons.EntityWaterDragon;
import com.nexusrpg.dragons.EntityWindDragon;
import com.nexusrpg.dragons.EntityEarthDragon;
import com.nexusrpg.init.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Register exactly one instance before worlds open; this also subscribes to chunk loads. */
public final class DragonNestGenerator implements IWorldGenerator {
    private static final Logger LOG = Logger.getLogger("NexusRpg.DragonNest");
    private static final int CHANCE = 120;
    private final Set<Chunk> saved = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>()));
    private final Set<Chunk> attempted = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>()));

    public DragonNestGenerator() { MinecraftForge.EVENT_BUS.register(this); }
    @SubscribeEvent public void onChunkDataLoad(ChunkDataEvent.Load e) {
        if (!e.getWorld().isRemote) saved.add(e.getChunk());
    }

    @Override public void generate(Random ignored, int chunkX, int chunkZ, World world,
                                   IChunkGenerator generator, IChunkProvider provider) {
        if (world.isRemote || world.provider.getDimension() != 0 || !world.getWorldInfo().isMapFeaturesEnabled()) return;
        Chunk chunk = provider.getLoadedChunk(chunkX, chunkZ);
        if (chunk == null || saved.contains(chunk) || !attempted.add(chunk)) return;
        Random random = new Random(world.getSeed() ^ 0x445241474e455354L ^
                (long) chunkX * 341873128712L ^ (long) chunkZ * 132897987541L);
        if (random.nextInt(CHANCE) != 0) return;
        int cx = (chunkX << 4) + 8, cz = (chunkZ << 4) + 8;
        if (!world.getWorldBorder().contains(new BlockPos(cx - 7, 64, cz - 7)) ||
            !world.getWorldBorder().contains(new BlockPos(cx + 7, 64, cz + 7))) return;
        try {
            if (!overlapsVanilla(world, cx - 7, cz - 7, cx + 7, cz + 7))
                build(world, provider, chunk, chunkX, chunkZ, cx, cz, random.nextInt(4));
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Dragon nest aborted at chunk " + chunkX + "," + chunkZ, e);
        }
    }

    private static void build(World w, IChunkProvider provider, Chunk chunk, int chunkX, int chunkZ,
                              int cx, int cz, int style) {
        int ground = chunk.getHeightValue(8, 8) - 1;
        if (ground < 5 || ground + 12 >= w.getHeight()) return;
        Map<BlockPos, IBlockState> support = new LinkedHashMap<BlockPos, IBlockState>();
        // A single flat 15x15 footprint, all in this chunk. Scan the full columns,
        // including underground, rejecting trees, player builds and unknown blocks.
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            int x = cx + dx, z = cz + dz;
            if (chunk.getHeightValue(x & 15, z & 15) - 1 != ground ||
                !surface(chunk.getBlockState(x & 15, ground, z & 15).getBlock())) return;
            for (int y = 0; y < w.getHeight(); y++) {
                BlockPos p = new BlockPos(x, y, z);
                IBlockState state = chunk.getBlockState(p);
                Block b = state.getBlock();
                if (b.hasTileEntity(state) || chunk.getTileEntityMap().containsKey(p) ||
                    (y > ground ? b != Blocks.AIR : !natural(b))) return;
                if (y <= ground && y >= ground - 4) {
                    if (!solidNatural(b)) return;
                    support.put(p, state);
                }
            }
        }
        for (BlockPos p : chunk.getTileEntityMap().keySet())
            if (p.getX() >= cx - 7 && p.getX() <= cx + 7 && p.getZ() >= cz - 7 && p.getZ() <= cz + 7) return;
        AxisAlignedBB area = new AxisAlignedBB(cx - 7, ground, cz - 7, cx + 8, ground + 12, cz + 8);
        if (occupied(w, area)) return;
        Map<BlockPos, IBlockState> plan = plan(cx, ground + 1, cz, style);
        List<BlockPos> writes = new ArrayList<BlockPos>();
        boolean keep = false;
        try {
            for (Map.Entry<BlockPos, IBlockState> e : plan.entrySet()) {
                BlockPos p = e.getKey();
                if (provider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    (p.getX() >> 4) != chunkX || (p.getZ() >> 4) != chunkZ ||
                    chunk.getBlockState(p).getBlock() != Blocks.AIR || chunk.getTileEntityMap().containsKey(p) || occupied(w, area)) return;
                writes.add(p); // Include writes which throw or return false after changing state.
                if (!w.setBlockState(p, e.getValue(), 2) || provider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    !chunk.getBlockState(p).equals(e.getValue()) || chunk.getTileEntityMap().containsKey(p)) return;
            }
            if (provider.getLoadedChunk(chunkX, chunkZ) != chunk || !intact(chunk, plan) ||
                !intact(chunk, support) || !clearAbove(chunk, plan, cx, cz, ground, w.getHeight()) ||
                occupied(w, area)) return;
            EntityLiving dragon = makeDragon(w, style);
            dragon.setLocationAndAngles(cx + 0.5D, ground + 1, cz + 0.5D, 0F, 0F);
            AxisAlignedBB box = dragon.getEntityBoundingBox();
            // Leave several blocks of padding for world collision-query expansion.
            if (box.minX < cx - 4 || box.maxX > cx + 5 || box.minZ < cz - 4 || box.maxZ > cz + 5 ||
                box.minY < ground + 1 || box.maxY > ground + 11 ||
                !w.getCollisionBoxes(dragon, box).isEmpty() || w.containsAnyLiquid(box) ||
                !w.checkNoEntityCollision(box, dragon)) return;
            if (provider.getLoadedChunk(chunkX, chunkZ) != chunk || !intact(chunk, plan) ||
                !intact(chunk, support) || !clearAbove(chunk, plan, cx, cz, ground, w.getHeight()) ||
                occupied(w, area)) return;
            dragon.enablePersistence();
            // If callbacks throw, spawn outcome is uncertain: preserve the nest.
            keep = true;
            boolean spawned = w.spawnEntity(dragon);
            boolean present = w.loadedEntityList.contains(dragon) && !dragon.isDead;
            if (!present) {
                keep = false;
                LOG.warning("Dragon spawn did not complete at " + cx + "," + cz);
            } else if (!spawned) {
                LOG.warning("Spawn callback returned false after adding a dragon; preserving nest at " + cx + "," + cz);
            }
        } finally {
            if (!keep) rollback(w, provider, chunk, chunkX, chunkZ, plan, writes);
        }
    }

    private static Map<BlockPos, IBlockState> plan(int x, int y, int z, int style) {
        Map<BlockPos, IBlockState> result = new LinkedHashMap<BlockPos, IBlockState>();
        Block main = new Block[] {Blocks.NETHER_BRICK, Blocks.PRISMARINE, Blocks.QUARTZ_BLOCK, Blocks.STONEBRICK}[style];
        Block accent = new Block[] {Blocks.RED_NETHER_BRICK, Blocks.SEA_LANTERN, Blocks.GLOWSTONE, Blocks.MOSSY_COBBLESTONE}[style];
        // A broad circular rim with four cardinal entrances; all blocks start in air.
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
            int r = dx * dx + dz * dz;
            if (r >= 36 && r <= 49 && !(Math.abs(dx) <= 1 || Math.abs(dz) <= 1)) {
                result.put(new BlockPos(x + dx, y, z + dz), main.getDefaultState());
                if (r >= 40) result.put(new BlockPos(x + dx, y + 1, z + dz), main.getDefaultState());
            }
        }
        // Eight staggered spires provide a tall, element-specific silhouette.
        int height = new int[] {7, 5, 9, 6}[style];
        int[][] posts = {{-5,-4},{-4,-5},{4,-5},{5,-4},{-5,4},{-4,5},{4,5},{5,4}};
        for (int i = 0; i < posts.length; i++) {
            int h = height - (i % 2) * 2;
            for (int k = 0; k < h; k++) result.put(new BlockPos(x + posts[i][0], y + k, z + posts[i][1]),
                (k == h - 1 ? accent : main).getDefaultState());
        }
        // Outer endpoints establish the full 15x15 dimension without blocking entrances.
        for (int[] p : new int[][] {{-7,-2},{-7,2},{7,-2},{7,2},{-2,-7},{2,-7},{-2,7},{2,7}})
            result.put(new BlockPos(x + p[0], y, z + p[1]), accent.getDefaultState());
        return result;
    }

    private static EntityLiving makeDragon(World w, int style) {
        switch (style) {
            case 0: return new EntityFireDragon(w);
            case 1: return new EntityWaterDragon(w);
            case 2: return new EntityWindDragon(w);
            default: return new EntityEarthDragon(w);
        }
    }
    private static boolean occupied(World w, AxisAlignedBB area) {
        for (Entity e : w.loadedEntityList)
            if (!e.isDead && e.getEntityBoundingBox().intersects(area)) return true;
        return false;
    }
    // Block-placement and collision callbacks may add an obstacle outside the plan.
    // Revalidate the originally empty columns, not just the dragon's hitbox.
    private static boolean clearAbove(Chunk chunk, Map<BlockPos, IBlockState> plan,
                                      int cx, int cz, int ground, int height) {
        for (int x = cx - 7; x <= cx + 7; x++) for (int z = cz - 7; z <= cz + 7; z++) {
            for (int y = ground + 1; y < height; y++) {
                BlockPos p = new BlockPos(x, y, z);
                IBlockState state = chunk.getBlockState(p);
                IBlockState expected = plan.get(p);
                if (chunk.getTileEntityMap().containsKey(p) || state.getBlock().hasTileEntity(state) ||
                    (expected == null ? state.getBlock() != Blocks.AIR : !state.equals(expected))) return false;
            }
        }
        return true;
    }
    private static boolean intact(Chunk chunk, Map<BlockPos, IBlockState> expected) {
        for (Map.Entry<BlockPos, IBlockState> e : expected.entrySet())
            if (!chunk.getBlockState(e.getKey()).equals(e.getValue()) || chunk.getTileEntityMap().containsKey(e.getKey())) return false;
        return true;
    }
    private static void rollback(World w, IChunkProvider provider, Chunk chunk, int x, int z,
                                 Map<BlockPos, IBlockState> plan, List<BlockPos> writes) {
        for (int i = writes.size() - 1; i >= 0; i--) {
            if (provider.getLoadedChunk(x, z) != chunk) return;
            BlockPos p = writes.get(i);
            if (chunk.getBlockState(p).equals(plan.get(p)) && !chunk.getTileEntityMap().containsKey(p)) {
                try { w.setBlockState(p, Blocks.AIR.getDefaultState(), 2); }
                catch (RuntimeException e) { LOG.log(Level.WARNING, "Nest rollback failed at " + p, e); }
            }
        }
    }
    private static boolean surface(Block b) {
        return b == Blocks.GRASS || b == Blocks.DIRT || b == Blocks.SAND || b == Blocks.GRAVEL;
    }
    private static boolean natural(Block b) {
        return surface(b) || b == Blocks.AIR || b == Blocks.STONE || b == Blocks.SANDSTONE ||
            b == Blocks.RED_SANDSTONE || b == Blocks.CLAY || b == Blocks.BEDROCK || b == Blocks.WATER ||
            b == Blocks.FLOWING_WATER || b == Blocks.LAVA || b == Blocks.FLOWING_LAVA ||
            b == Blocks.COAL_ORE || b == Blocks.IRON_ORE || b == Blocks.GOLD_ORE ||
            b == Blocks.REDSTONE_ORE || b == Blocks.LIT_REDSTONE_ORE || b == Blocks.LAPIS_ORE ||
            b == Blocks.DIAMOND_ORE || b == Blocks.EMERALD_ORE || b == ModBlocks.MITHRIL_ORE ||
            b == ModBlocks.ADAMANTITE_ORE || b == ModBlocks.DRACONITE_ORE;
    }
    private static boolean solidNatural(Block b) {
        return natural(b) && b != Blocks.AIR && b != Blocks.WATER && b != Blocks.FLOWING_WATER &&
            b != Blocks.LAVA && b != Blocks.FLOWING_LAVA;
    }
    private static boolean overlapsVanilla(World w, int minX, int minZ, int maxX, int maxZ) {
        for (String name : new String[] {"Village", "Mineshaft", "Temple", "Stronghold", "Monument", "Mansion"}) {
            MapGenStructureData data = (MapGenStructureData) w.getPerWorldStorage().getOrLoadData(MapGenStructureData.class, name);
            if (data == null) continue;
            NBTTagCompound tags = data.getTagCompound();
            for (String key : tags.getKeySet()) {
                if (!tags.hasKey(key, 10)) return true;
                NBTTagCompound f = tags.getCompoundTag(key);
                if (!f.hasKey("BB", 11)) return true;
                int[] bb = f.getIntArray("BB");
                if (bb.length != 6 || bb[0] > bb[3] || bb[1] > bb[4] || bb[2] > bb[5]) return true;
                if (bb[0] <= maxX && bb[3] >= minX && bb[2] <= maxZ && bb[5] >= minZ) return true;
            }
        }
        return false;
    }
}
