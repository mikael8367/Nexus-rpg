package com.nexusrpg.world.ent;

import com.nexusrpg.ent.EntityEntAnciao;
import com.nexusrpg.init.ModBiomes;
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

/** Natural, additive clearing. Register exactly one instance before opening worlds. */
public final class EntClearingGenerator implements IWorldGenerator {
    private static final Logger LOG = Logger.getLogger("NexusRpg.EntClearing");
    // Candidate probability only, before the deliberately restrictive checks.
    private static final int CANDIDATE_CHANCE = 80;
    private final Set<Chunk> saved = Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());
    private final Set<Chunk> attempted = Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());

    public EntClearingGenerator() { MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (!event.getWorld().isRemote) saved.add(event.getChunk());
    }

    @Override
    public void generate(Random ignored, int chunkX, int chunkZ, World world,
                         IChunkGenerator generator, IChunkProvider provider) {
        if (world.isRemote || world.provider.getDimension() != 0 ||
            !world.getWorldInfo().isMapFeaturesEnabled()) return;
        Chunk chunk = provider.getLoadedChunk(chunkX, chunkZ);
        // Existing saved chunks are never retrofitted. Also guard repeat calls within
        // the lifetime of a newly generated chunk, including failed attempts.
        if (chunk == null || saved.contains(chunk) || !attempted.add(chunk)) return;
        Random random = new Random(world.getSeed() ^ 0x454e545f434c5247L ^
            (long) chunkX * 341873128712L ^ (long) chunkZ * 132897987541L);
        if (random.nextInt(CANDIDATE_CHANCE) != 0) return;
        int ox = chunkX << 4, oz = chunkZ << 4, cx = ox + 8, cz = oz + 8;
        if (!world.getWorldBorder().contains(new BlockPos(cx - 5, 64, cz - 5)) ||
            !world.getWorldBorder().contains(new BlockPos(cx + 5, 64, cz + 5))) return;
        try {
            if (overlapsVanilla(world, cx - 5, cz - 5, cx + 5, cz + 5)) return;
            build(world, provider, chunk, chunkX, chunkZ, cx, cz);
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Ent clearing generation failed at chunk " + chunkX + "," + chunkZ, ex);
        }
    }

    private static void build(World world, IChunkProvider provider, Chunk chunk,
                              int chunkX, int chunkZ, int cx, int cz) {
        int centerGround = chunk.getHeightValue(8, 8) - 1;
        if (centerGround < 4 || centerGround + 12 >= world.getHeight()) return;
        int[][] grounds = new int[11][11];
        Map<BlockPos, IBlockState> support = new LinkedHashMap<BlockPos, IBlockState>();
        // Inspect 11x11 entirely inside the generated chunk. Reject trees, leaves,
        // unknown/modded blocks, built blocks, tiles and deep structures. Never cut
        // a tree or replace even a flower; the clearing must already exist naturally.
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            int x = cx + dx, z = cz + dz;
            if (chunk.getBiome(new BlockPos(x, 0, z), world.getBiomeProvider()) != ModBiomes.ENCHANTED_GROVE) return;
            int ground = chunk.getHeightValue(x & 15, z & 15) - 1;
            if (ground < 4 || ground + 12 >= world.getHeight() || Math.abs(ground - centerGround) > 2) return;
            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2 && ground != centerGround) return;
            if (!surface(chunk.getBlockState(x & 15, ground, z & 15).getBlock())) return;
            grounds[dx + 5][dz + 5] = ground;
            for (int y = 0; y < world.getHeight(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                IBlockState state = chunk.getBlockState(pos);
                Block b = state.getBlock();
                if (b.hasTileEntity(state) || chunk.getTileEntityMap().containsKey(pos)) return;
                if (y > ground ? !airOrPlant(b) : !natural(b)) return;
                if (y <= ground && y >= ground - 3) {
                    if (!solidNatural(b)) return;
                    support.put(pos, state);
                }
                // Guarantee a spacious, unobstructed central spawn area.
                if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2 && y > ground && y <= ground + 8 && b != Blocks.AIR) return;
            }
        }
        for (BlockPos p : chunk.getTileEntityMap().keySet())
            if (Math.abs((long) p.getX() - cx) <= 5 && Math.abs((long) p.getZ() - cz) <= 5) return;
        AxisAlignedBB area = new AxisAlignedBB(cx - 5, centerGround - 2, cz - 5,
                                               cx + 6, centerGround + 12, cz + 6);
        if (!world.getEntitiesWithinAABBExcludingEntity(null, area).isEmpty()) return;
        // Four mossy rune stones and four short pillars. Openings at each side;
        // there is no wall and no ground replacement. Every write starts in air.
        Map<BlockPos, IBlockState> plan = new LinkedHashMap<BlockPos, IBlockState>();
        for (int dx : new int[] {-4, 4}) for (int dz : new int[] {-4, 4}) {
            int y = grounds[dx + 5][dz + 5] + 1;
            plan.put(new BlockPos(cx + dx, y, cz + dz), Blocks.MOSSY_COBBLESTONE.getDefaultState());
            plan.put(new BlockPos(cx + dx, y + 1, cz + dz), Blocks.STONEBRICK.getDefaultState());
        }
        for (int[] d : new int[][] {{-4, 0}, {4, 0}, {0, -4}, {0, 4}}) {
            int y = grounds[d[0] + 5][d[1] + 5] + 1;
            plan.put(new BlockPos(cx + d[0], y, cz + d[1]), Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }
        for (BlockPos p : plan.keySet())
            if ((p.getX() >> 4) != chunkX || (p.getZ() >> 4) != chunkZ ||
                chunk.getBlockState(p).getBlock() != Blocks.AIR || chunk.getTileEntityMap().containsKey(p)) return;
        List<BlockPos> writes = new ArrayList<BlockPos>();
        boolean keep = false;
        try {
            for (Map.Entry<BlockPos, IBlockState> e : plan.entrySet()) {
                BlockPos p = e.getKey();
                if (provider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    chunk.getBlockState(p).getBlock() != Blocks.AIR || chunk.getTileEntityMap().containsKey(p)) return;
                writes.add(p); // even a false/throwing write might have changed the block
                if (!world.setBlockState(p, e.getValue(), 2) ||
                    provider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    !chunk.getBlockState(p).equals(e.getValue()) || chunk.getTileEntityMap().containsKey(p)) return;
            }
            if (provider.getLoadedChunk(chunkX, chunkZ) != chunk || !intact(chunk, plan) || !intact(chunk, support) ||
                !world.getEntitiesWithinAABBExcludingEntity(null, area).isEmpty()) return;
            // Construction and collision checks precede the only spawn attempt.
            EntityEntAnciao ent = new EntityEntAnciao(world);
            ent.setLocationAndAngles(cx + 0.5D, centerGround + 1, cz + 0.5D, 0.0F, 0.0F);
            AxisAlignedBB box = ent.getEntityBoundingBox();
            // The spawn's collision queries remain well inside this already-loaded chunk.
            if (box.minX < cx - 2 || box.maxX > cx + 3 || box.minZ < cz - 2 || box.maxZ > cz + 3 ||
                box.minY < centerGround + 1 || box.maxY > centerGround + 9 ||
                !world.getCollisionBoxes(ent, box).isEmpty() || world.containsAnyLiquid(box) ||
                !world.checkNoEntityCollision(box, ent)) return;
            if (provider.getLoadedChunk(chunkX, chunkZ) != chunk || !intact(chunk, plan) || !intact(chunk, support)) return;
            ent.enablePersistence();
            // If a foreign spawn callback throws after adding the entity, its outcome
            // is unknown. Keep the clearing rather than strand a potentially live boss.
            keep = true;
            if (!world.spawnEntity(ent)) {
                keep = false;
                LOG.warning("Ent spawn was cancelled at " + cx + "," + cz);
            }
        } finally {
            if (!keep) rollback(world, provider, chunk, chunkX, chunkZ, plan, writes);
        }
    }

    private static boolean intact(Chunk chunk, Map<BlockPos, IBlockState> expected) {
        for (Map.Entry<BlockPos, IBlockState> e : expected.entrySet())
            if (!chunk.getBlockState(e.getKey()).equals(e.getValue()) || chunk.getTileEntityMap().containsKey(e.getKey())) return false;
        return true;
    }
    private static void rollback(World world, IChunkProvider provider, Chunk chunk, int x, int z,
                                 Map<BlockPos, IBlockState> plan, List<BlockPos> writes) {
        for (int i = writes.size() - 1; i >= 0; i--) {
            if (provider.getLoadedChunk(x, z) != chunk) return;
            BlockPos p = writes.get(i);
            // Never overwrite another mod's block or tile. Rollback is best effort.
            if (chunk.getBlockState(p).equals(plan.get(p)) && !chunk.getTileEntityMap().containsKey(p))
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
        }
    }
    private static boolean surface(Block b) { return b == Blocks.GRASS || b == Blocks.DIRT; }
    private static boolean airOrPlant(Block b) {
        return b == Blocks.AIR || b == Blocks.TALLGRASS || b == Blocks.YELLOW_FLOWER ||
            b == Blocks.RED_FLOWER || b == Blocks.BROWN_MUSHROOM || b == Blocks.RED_MUSHROOM || b == Blocks.DEADBUSH;
    }
    private static boolean natural(Block b) {
        return surface(b) || b == Blocks.AIR || b == Blocks.STONE || b == Blocks.GRAVEL ||
            b == Blocks.SAND || b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE || b == Blocks.CLAY ||
            b == Blocks.BEDROCK || b == Blocks.WATER || b == Blocks.FLOWING_WATER ||
            b == Blocks.LAVA || b == Blocks.FLOWING_LAVA || b == Blocks.COAL_ORE || b == Blocks.IRON_ORE ||
            b == Blocks.GOLD_ORE || b == Blocks.REDSTONE_ORE || b == Blocks.LIT_REDSTONE_ORE ||
            b == Blocks.LAPIS_ORE || b == Blocks.DIAMOND_ORE || b == Blocks.EMERALD_ORE ||
            b == ModBlocks.MITHRIL_ORE || b == ModBlocks.ADAMANTITE_ORE || b == ModBlocks.DRACONITE_ORE;
    }
    private static boolean solidNatural(Block b) {
        return natural(b) && b != Blocks.AIR && b != Blocks.WATER && b != Blocks.FLOWING_WATER &&
            b != Blocks.LAVA && b != Blocks.FLOWING_LAVA;
    }
    private static boolean overlapsVanilla(World world, int minX, int minZ, int maxX, int maxZ) {
        for (String name : new String[] {"Village", "Mineshaft", "Temple", "Stronghold", "Monument", "Mansion"}) {
            MapGenStructureData data = (MapGenStructureData) world.getPerWorldStorage().getOrLoadData(MapGenStructureData.class, name);
            if (data == null) continue;
            NBTTagCompound tags = data.getTagCompound();
            for (String key : tags.getKeySet()) {
                if (!tags.hasKey(key, 10)) return true;
                NBTTagCompound feature = tags.getCompoundTag(key);
                if (!feature.hasKey("BB", 11)) return true;
                int[] bb = feature.getIntArray("BB");
                if (bb.length != 6 || bb[0] > bb[3] || bb[1] > bb[4] || bb[2] > bb[5]) return true;
                if (bb[0] <= maxX && bb[3] >= minX && bb[2] <= maxZ && bb[5] >= minZ) return true;
            }
        }
        return false;
    }
}
