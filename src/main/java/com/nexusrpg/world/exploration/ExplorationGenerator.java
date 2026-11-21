package com.nexusrpg.world.exploration;

import com.nexusrpg.init.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Conservative, single-chunk surface ruins. Never retrofits chunks loaded from disk. */
public final class ExplorationGenerator implements IWorldGenerator {
    private final Set<Chunk> saved = Collections.synchronizedSet(
        Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>()));

    public ExplorationGenerator() { MinecraftForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void onLoad(ChunkDataEvent.Load event) {
        if (!event.getWorld().isRemote) saved.add(event.getChunk());
    }

    @Override
    public void generate(Random ignored, int chunkX, int chunkZ, World world,
                         IChunkGenerator generator, IChunkProvider provider) {
        if (world.isRemote || world.provider.getDimension() != 0 ||
            !world.getWorldInfo().isMapFeaturesEnabled()) return;
        Chunk chunk = provider.getLoadedChunk(chunkX, chunkZ);
        if (chunk == null || saved.contains(chunk)) return;
        // Independent random stream: do not perturb the other registered generators.
        Random random = new Random(world.getSeed() ^ 0x6e65787573524750L ^
            (long)chunkX * 341873128712L ^ (long)chunkZ * 132897987541L);
        if (random.nextInt(180) != 0) return;
        int type = random.nextInt(3);
        int x0 = (chunkX << 4), z0 = (chunkZ << 4);
        if (!world.getWorldBorder().contains(new BlockPos(x0 + 2, 64, z0 + 2)) ||
            !world.getWorldBorder().contains(new BlockPos(x0 + 13, 64, z0 + 13))) return;
        if (overlapsVanilla(world, x0 + 2, z0 + 2, x0 + 13, z0 + 13)) return;
        int ground = chunk.getHeightValue(8, 8) - 1;
        // Require four contiguous, solid natural layers under every inspected column.
        // A level top alone can be a thin floating shelf or a roof over water/a cave.
        if (ground < 3 || ground + 13 >= world.getHeight()) return;
        // Strict flat natural ground, a 1-block horizontal buffer and completely clear sky.
        // Checking every column all the way down rejects known or unknown built blocks,
        // including underground constructions; false negatives are intentional.
        for (int x = 2; x <= 13; x++) for (int z = 2; z <= 13; z++) {
            if (chunk.getHeightValue(x, z) - 1 != ground ||
                !surface(chunk.getBlockState(x, ground, z).getBlock())) return;
            for (int y = 0; y < world.getHeight(); y++) {
                IBlockState state = chunk.getBlockState(x, y, z);
                if (state.getBlock().hasTileEntity(state) ||
                    (y > ground ? state.getBlock() != Blocks.AIR : !natural(state.getBlock())) ||
                    (y <= ground && y >= ground - 3 && !solidNatural(state.getBlock()))) return;
            }
        }
        for (BlockPos p : chunk.getTileEntityMap().keySet())
            if (p.getX() >= x0 + 2 && p.getX() <= x0 + 13 &&
                p.getZ() >= z0 + 2 && p.getZ() <= z0 + 13) return;
        if (!world.getEntitiesWithinAABBExcludingEntity(null,
                new AxisAlignedBB(x0 + 2, ground + 1, z0 + 2, x0 + 14, ground + 14, z0 + 14)).isEmpty()) return;
        Map<BlockPos, IBlockState> plan = plan(x0, ground + 1, z0, type, random);
        BlockPos chest = new BlockPos(x0 + 8, ground + 2, z0 + 10);
        if (plan.containsKey(chest) || plan.containsKey(chest.up()) || plan.size() > 800) return;
        for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet()) {
            BlockPos p = entry.getKey();
            if ((p.getX() >> 4) != chunkX || (p.getZ() >> 4) != chunkZ ||
                p.getY() <= ground || p.getY() >= world.getHeight() ||
                chunk.getBlockState(p).getBlock() != Blocks.AIR || chunk.getTileEntityMap().containsKey(p) ||
                entry.getValue().getBlock().hasTileEntity(entry.getValue())) return;
        }
        List<BlockPos> attempted = new ArrayList<BlockPos>();
        boolean chestAttempted = false, complete = false;
        try {
            for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet()) {
                BlockPos p = entry.getKey();
                if (provider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    chunk.getBlockState(p).getBlock() != Blocks.AIR || chunk.getTileEntityMap().containsKey(p)) return;
                attempted.add(p);
                if (!world.setBlockState(p, entry.getValue(), 2) ||
                    !chunk.getBlockState(p).equals(entry.getValue()) || chunk.getTileEntityMap().containsKey(p)) return;
            }
            if (!intact(provider, chunk, chunkX, chunkZ, plan) || !supported(chunk, ground) ||
                chunk.getBlockState(chest).getBlock() != Blocks.AIR ||
                chunk.getBlockState(chest.up()).getBlock() != Blocks.AIR ||
                chunk.getTileEntityMap().containsKey(chest) || chunk.getTileEntityMap().containsKey(chest.up())) return;
            chestAttempted = true;
            if (!world.setBlockState(chest, Blocks.CHEST.getDefaultState(), 2) ||
                provider.getLoadedChunk(chunkX, chunkZ) != chunk) return;
            TileEntity tile = chunk.getTileEntityMap().get(chest);
            if (chunk.getBlockState(chest).getBlock() != Blocks.CHEST || !(tile instanceof TileEntityChest) ||
                chunk.getBlockState(chest.up()).getBlock() != Blocks.AIR ||
                chunk.getTileEntityMap().containsKey(chest.up()) || !intact(provider, chunk, chunkX, chunkZ, plan) ||
                !supported(chunk, ground)) return;
            ((TileEntityChest) tile).setLootTable(LootTableList.CHESTS_SIMPLE_DUNGEON, random.nextLong());
            tile.markDirty();
            complete = true;
        } finally {
            if (!complete && provider.getLoadedChunk(chunkX, chunkZ) == chunk) {
                // Never remove a chest with a tile/inventory, even after a failed callback.
                if (chestAttempted && chunk.getTileEntityMap().get(chest) == null &&
                    chunk.getBlockState(chest).equals(Blocks.CHEST.getDefaultState()))
                    world.setBlockState(chest, Blocks.AIR.getDefaultState(), 2);
                if (chunk.getBlockState(chest).getBlock() == Blocks.AIR && !chunk.getTileEntityMap().containsKey(chest)) {
                    for (int i = attempted.size() - 1; i >= 0; i--) {
                        if (provider.getLoadedChunk(chunkX, chunkZ) != chunk) break;
                        BlockPos p = attempted.get(i);
                        if (chunk.getBlockState(p).equals(plan.get(p)) && !chunk.getTileEntityMap().containsKey(p))
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private static boolean intact(IChunkProvider provider, Chunk chunk, int x, int z, Map<BlockPos, IBlockState> plan) {
        if (provider.getLoadedChunk(x, z) != chunk) return false;
        for (Map.Entry<BlockPos, IBlockState> e : plan.entrySet())
            if (!chunk.getBlockState(e.getKey()).equals(e.getValue()) || chunk.getTileEntityMap().containsKey(e.getKey())) return false;
        return true;
    }

    /** Each type is a bounded, partially random design with a walkable entrance and loot. */
    static Map<BlockPos, IBlockState> plan(int ox, int y, int oz, int type, Random r) {
        Map<BlockPos, IBlockState> out = new LinkedHashMap<BlockPos, IBlockState>();
        Block primary = type == 2 ? Blocks.STONEBRICK : Blocks.COBBLESTONE;
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++)
            put(out, ox+x, y, oz+z, primary);
        int height = type == 2 ? 8 : type == 1 ? 4 : 3;
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++) {
            if (x != 3 && x != 12 && z != 3 && z != 12) continue;
            for (int h = 1; h <= height; h++) {
                if (z == 3 && (x == 7 || x == 8) && h <= 2) continue;
                // Varied crenellations/openings do not block the entrance or interior.
                if (h == height && type != 0 && ((x + z) & 1) != 0) continue;
                if (type == 1 && h >= 3 && r.nextInt(5) == 0) continue;
                put(out, ox+x, y+h, oz+z, primary);
            }
        }
        if (type == 0) {
            // Two rooms with one randomized opening, chest on the far side.
            int doorway = 5 + r.nextInt(6);
            for (int x = 4; x <= 11; x++) if (x != doorway)
                for (int h = 1; h <= 3; h++) put(out, ox+x, y+h, oz+7, Blocks.MOSSY_COBBLESTONE);
        } else if (type == 2) {
            for (int x : new int[]{4, 11}) for (int z : new int[]{4, 11})
                put(out, ox+x, y+4, oz+z, Blocks.GLOWSTONE);
        }
        return out;
    }
    private static void put(Map<BlockPos, IBlockState> p, int x, int y, int z, Block b) {
        p.put(new BlockPos(x, y, z), b.getDefaultState());
    }
    private static boolean surface(Block b) {
        return b == Blocks.GRASS || b == Blocks.DIRT || b == Blocks.STONE || b == Blocks.SAND ||
            b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE || b == Blocks.GRAVEL || b == Blocks.MYCELIUM;
    }
    private static boolean natural(Block b) {
        return surface(b) || b == Blocks.AIR || b == Blocks.BEDROCK || b == Blocks.WATER ||
            b == Blocks.FLOWING_WATER || b == Blocks.LAVA || b == Blocks.FLOWING_LAVA || b == Blocks.CLAY ||
            b == Blocks.COAL_ORE || b == Blocks.IRON_ORE || b == Blocks.GOLD_ORE || b == Blocks.REDSTONE_ORE ||
            b == Blocks.LIT_REDSTONE_ORE || b == Blocks.LAPIS_ORE || b == Blocks.DIAMOND_ORE || b == Blocks.EMERALD_ORE ||
            // Our ore generator runs first; these veins are terrain, like the vanilla ores above.
            b == ModBlocks.MITHRIL_ORE || b == ModBlocks.ADAMANTITE_ORE || b == ModBlocks.DRACONITE_ORE;
    }
    private static boolean solidNatural(Block b) {
        return natural(b) && b != Blocks.AIR && b != Blocks.WATER && b != Blocks.FLOWING_WATER &&
            b != Blocks.LAVA && b != Blocks.FLOWING_LAVA;
    }
    // Third-party callbacks can change the ground while the plan is being placed.
    private static boolean supported(Chunk chunk, int ground) {
        for (int x = 2; x <= 13; x++) for (int z = 2; z <= 13; z++) {
            if (!surface(chunk.getBlockState(x, ground, z).getBlock())) return false;
            for (int y = ground; y >= ground - 3; y--) {
                IBlockState state = chunk.getBlockState(x, y, z);
                if (!solidNatural(state.getBlock()) || state.getBlock().hasTileEntity(state)) return false;
            }
        }
        return true;
    }
    private static boolean overlapsVanilla(World world, int minX, int minZ, int maxX, int maxZ) {
        for (String name : new String[]{"Village", "Mineshaft", "Temple", "Stronghold", "Monument", "Mansion"}) {
            MapGenStructureData data = (MapGenStructureData)world.getPerWorldStorage().getOrLoadData(MapGenStructureData.class, name);
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
