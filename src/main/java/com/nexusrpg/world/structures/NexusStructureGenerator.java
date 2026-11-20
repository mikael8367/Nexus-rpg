package com.nexusrpg.world.structures;

import com.nexusrpg.init.ModBiomes;
import com.nexusrpg.init.ModBlocks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.LinkedHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Register ONE instance in preInit, before any worlds are opened.
 * Disk-loaded chunks are eligible only when their saved terrain is explicitly unpopulated.
 */
public final class NexusStructureGenerator implements IWorldGenerator {
    // A live loaded chunk holds its own identity; weak keys do not retain unloaded chunks/worlds.
    private final Set<Chunk> loadedFromDisk = Collections.synchronizedSet(
        Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>()));

    public NexusStructureGenerator() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (!event.getWorld().isRemote) {
            NBTTagCompound data = event.getData();
            // Missing or malformed data is not proof that this chunk is safe to populate.
            if (data == null || !data.hasKey("Level", 10)) {
                loadedFromDisk.add(event.getChunk());
                return;
            }
            NBTTagCompound level = data.getCompoundTag("Level");
            if (!level.hasKey("TerrainPopulated", 1) || level.getBoolean("TerrainPopulated"))
                loadedFromDisk.add(event.getChunk());
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.isRemote || world.provider.getDimension() != 0 || random.nextInt(80) != 0) return;
        // getLoadedChunk never generates or loads a neighbouring chunk.
        Chunk chunk = chunkProvider.getLoadedChunk(chunkX, chunkZ);
        if (chunk == null || loadedFromDisk.contains(chunk)) return;
        int cx = (chunkX << 4) + 8, cz = (chunkZ << 4) + 8;
        if (!world.getWorldBorder().contains(new BlockPos(cx - 4, 64, cz - 4)) ||
            !world.getWorldBorder().contains(new BlockPos(cx + 4, 64, cz + 4))) return;
        Biome biome = chunk.getBiome(new BlockPos(cx, 0, cz), world.getBiomeProvider());
        int style = styleOf(biome);
        if (style < 0) return;

        // 9x9 inspection area stays entirely inside this chunk. No chunk-border writes.
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
        int[][] heights = new int[9][9];
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int x = cx + dx, z = cz + dz;
            if (chunk.getBiome(new BlockPos(x, 0, z), world.getBiomeProvider()) != biome) return;
            int h = chunk.getHeightValue(x & 15, z & 15);
            if (h < 2 || h + 12 >= world.getHeight()) return;
            Block surface = chunk.getBlockState(x & 15, h - 1, z & 15).getBlock();
            if (!isGround(surface)) return;
            heights[dx + 4][dz + 4] = h;
            if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) {
                min = Math.min(min, h); max = Math.max(max, h);
            }
            // Reject buildings, tile entities and unknown blocks anywhere in the column,
            // including underground structures and roofs above the surface.
            for (int y = 0; y < world.getHeight(); y++) {
                IBlockState state = chunk.getBlockState(x & 15, y, z & 15);
                if (state.getBlock().hasTileEntity(state) || !isNatural(state.getBlock())) return;
                if (y >= h && !isReplaceableSurface(state.getBlock())) return;
            }
        }
        if (max - min > 2 || max + 12 >= world.getHeight()) return;
        if (overlapsVanillaStructure(world, cx - 4, cz - 4, cx + 4, cz + 4)) return;
        for (BlockPos pos : chunk.getTileEntityMap().keySet()) {
            if (Math.abs(pos.getX() - cx) <= 4 && Math.abs(pos.getZ() - cz) <= 4) return;
        }

        Map<BlockPos, IBlockState> plan = new LinkedHashMap<BlockPos, IBlockState>();
        Block floor = floor(style);
        // Raise a tiny platform without digging, using at most two support blocks per column.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            for (int y = heights[dx + 4][dz + 4]; y <= max; y++)
                put(plan, cx, y, cz, dx, 0, dz, floor);
        }
        decorate(plan, cx, max, cz, style);
        BlockPos chestPos = new BlockPos(cx, max + 1, cz);
        if (plan.containsKey(chestPos) || plan.containsKey(chestPos.up())) return;
        // Snapshot exact states, including plant/snow metadata, for conditional rollback.
        Map<BlockPos, IBlockState> original = new LinkedHashMap<BlockPos, IBlockState>();
        for (BlockPos pos : plan.keySet()) {
            IBlockState state = chunk.getBlockState(pos);
            if ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ ||
                !isReplaceableSurface(state.getBlock()) || state.getBlock().hasTileEntity(state) ||
                chunk.getTileEntityMap().containsKey(pos)) return;
            original.put(pos, state);
        }
        if (chunk.getBlockState(chestPos).getBlock() != Blocks.AIR ||
            chunk.getBlockState(chestPos.up()).getBlock() != Blocks.AIR ||
            chunk.getTileEntityMap().containsKey(chestPos) ||
            chunk.getTileEntityMap().containsKey(chestPos.up())) return;

        List<BlockPos> attempted = new ArrayList<BlockPos>();
        boolean complete = false;
        try {
            // Build first, chest last: a rejected floor cannot leave a floating chest.
            for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet()) {
                BlockPos pos = entry.getKey();
                if (chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    !chunk.getBlockState(pos).equals(original.get(pos)) ||
                    chunk.getTileEntityMap().containsKey(pos)) return;
                // Include failed writes: a mod may change a block while returning false.
                attempted.add(pos);
                if (!world.setBlockState(pos, entry.getValue(), 2) ||
                    chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    !chunk.getBlockState(pos).equals(entry.getValue()) ||
                    chunk.getTileEntityMap().containsKey(pos)) return;
            }
            // Callbacks during later writes may have changed earlier positions.
            if (chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk) return;
            for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet())
                if (!chunk.getBlockState(entry.getKey()).equals(entry.getValue()) ||
                    chunk.getTileEntityMap().containsKey(entry.getKey())) return;
            if (chunk.getBlockState(chestPos).getBlock() != Blocks.AIR ||
                chunk.getBlockState(chestPos.up()).getBlock() != Blocks.AIR ||
                chunk.getTileEntityMap().containsKey(chestPos) ||
                chunk.getTileEntityMap().containsKey(chestPos.up())) return;
            complete = true;
        } finally {
            if (!complete) rollback(world, chunkProvider, chunk, chunkX, chunkZ, plan, original, attempted);
        }

        boolean placed = world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
        if (chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk) return;
        IBlockState chestState = chunk.getBlockState(chestPos);
        TileEntity tile = chunk.getTileEntityMap().get(chestPos);
        if (!placed || chestState.getBlock() != Blocks.CHEST || !(tile instanceof TileEntityChest)) {
            // If no tile exists, remove only our exact chest state; never destroy another mod's tile.
            if (tile == null && chestState.equals(Blocks.CHEST.getDefaultState()))
                world.setBlockState(chestPos, Blocks.AIR.getDefaultState(), 2);
            // Keep the platform if a chest remains: removing support could make it float.
            if (chunkProvider.getLoadedChunk(chunkX, chunkZ) == chunk &&
                chunk.getBlockState(chestPos).getBlock() != Blocks.CHEST)
                rollback(world, chunkProvider, chunk, chunkX, chunkZ, plan, original, attempted);
            return;
        }
        ((TileEntityChest) tile).setLootTable(new ResourceLocation("nexusrpg", "chests/" + id(style)), random.nextLong());
        tile.markDirty();
    }

    /** Best effort: never load chunks or overwrite a block/tile changed by another mod. */
    private static void rollback(World world, IChunkProvider provider, Chunk chunk, int chunkX, int chunkZ,
                                 Map<BlockPos, IBlockState> plan, Map<BlockPos, IBlockState> original,
                                 List<BlockPos> attempted) {
        for (int i = attempted.size() - 1; i >= 0; i--) {
            if (provider.getLoadedChunk(chunkX, chunkZ) != chunk) return;
            BlockPos pos = attempted.get(i);
            if (chunk.getBlockState(pos).equals(plan.get(pos)) &&
                !chunk.getTileEntityMap().containsKey(pos))
                world.setBlockState(pos, original.get(pos), 2);
        }
    }

    /** Inspect saved vanilla bounding boxes without loading any chunks. */
    private static boolean overlapsVanillaStructure(World world, int minX, int minZ, int maxX, int maxZ) {
        for (String name : new String[] {"Village", "Mineshaft", "Temple", "Stronghold", "Monument", "Mansion"}) {
            MapGenStructureData data = (MapGenStructureData) world.getPerWorldStorage()
                .getOrLoadData(MapGenStructureData.class, name);
            if (data == null) continue;
            NBTTagCompound features = data.getTagCompound();
            for (String key : features.getKeySet()) {
                if (!features.hasKey(key, 10)) return true;
                NBTTagCompound start = features.getCompoundTag(key);
                if (!start.hasKey("BB", 11)) return true;
                int[] bb = start.getIntArray("BB");
                if (bb.length != 6 || bb[0] > bb[3] || bb[2] > bb[5]) return true;
                if (bb[0] <= maxX && bb[3] >= minX && bb[2] <= maxZ && bb[5] >= minZ) return true;
            }
        }
        return false;
    }
    private static boolean isReplaceableSurface(Block b) {
        return b == Blocks.AIR || b == Blocks.SNOW_LAYER || b == Blocks.TALLGRASS ||
            b == Blocks.YELLOW_FLOWER || b == Blocks.RED_FLOWER || b == Blocks.DEADBUSH ||
            b == Blocks.BROWN_MUSHROOM || b == Blocks.RED_MUSHROOM;
    }
    private static boolean isGround(Block b) {
        return b == Blocks.GRASS || b == Blocks.DIRT || b == Blocks.STONE || b == Blocks.SAND ||
               b == Blocks.GRAVEL || b == Blocks.SANDSTONE || b == Blocks.RED_SANDSTONE || b == Blocks.MYCELIUM;
    }
    private static boolean isNatural(Block b) {
        return isGround(b) || isReplaceableSurface(b) || b == Blocks.BEDROCK || b == Blocks.WATER ||
            b == Blocks.FLOWING_WATER || b == Blocks.LAVA || b == Blocks.FLOWING_LAVA || b == Blocks.CLAY ||
            b == Blocks.COAL_ORE || b == Blocks.IRON_ORE || b == Blocks.GOLD_ORE || b == Blocks.REDSTONE_ORE ||
            b == Blocks.LIT_REDSTONE_ORE || b == Blocks.LAPIS_ORE || b == Blocks.DIAMOND_ORE || b == Blocks.EMERALD_ORE ||
            b == ModBlocks.MITHRIL_ORE || b == ModBlocks.ADAMANTITE_ORE || b == ModBlocks.DRACONITE_ORE;
    }
    private static int styleOf(Biome b) {
        if (b == ModBiomes.ENCHANTED_GROVE) return 0;
        if (b == ModBiomes.FROST_CROWN) return 1;
        if (b == ModBiomes.SUNKEN_EMPIRE) return 2;
        if (b == ModBiomes.WITCHING_MIRE) return 3;
        if (b == ModBiomes.CINDER_HEART) return 4;
        if (b == ModBiomes.ROSA_SANCTUM) return 5;
        if (b == ModBiomes.UMBRAL_REACH) return 6;
        if (b == ModBiomes.TITAN_CANOPY) return 7;
        return -1;
    }
    private static String id(int s) {
        return new String[] {"enchanted_grove", "frost_crown", "sunken_empire", "witching_mire",
            "cinder_heart", "rosa_sanctum", "umbral_reach", "titan_canopy"}[s];
    }
    private static Block floor(int s) {
        switch (s) {
            case 0: return Blocks.MOSSY_COBBLESTONE;
            case 1: return Blocks.STONEBRICK;
            case 2: return Blocks.SANDSTONE;
            case 3: return Blocks.PLANKS;
            case 4: return Blocks.NETHER_BRICK;
            case 5: return Blocks.QUARTZ_BLOCK;
            case 6: return Blocks.OBSIDIAN;
            default: return Blocks.LOG;
        }
    }
    private static void put(Map<BlockPos, IBlockState> p, int x, int y, int z, int dx, int dy, int dz, Block b) {
        p.put(new BlockPos(x + dx, y + dy, z + dz), b.getDefaultState());
    }
    private static void pillar(Map<BlockPos, IBlockState> p, int x, int y, int z, int dx, int dz, int height, Block b) {
        for (int dy = 1; dy <= height; dy++) put(p, x, y, z, dx, dy, dz, b);
    }
    private static void decorate(Map<BlockPos, IBlockState> p, int x, int y, int z, int s) {
        switch (s) {
        case 0: // anel rúnico musgoso
            for (int dx : new int[] {-3, 3}) for (int dz : new int[] {-3, 3}) pillar(p,x,y,z,dx,dz,3,Blocks.STONEBRICK);
            for (int dx : new int[] {-2, 2}) for (int dz : new int[] {-2, 2}) pillar(p,x,y,z,dx,dz,1,Blocks.MOSSY_COBBLESTONE);
            break;
        case 1: // quatro obeliscos de gelo
            for (int dx : new int[] {-2, 2}) for (int dz : new int[] {-2, 2}) {
                pillar(p,x,y,z,dx,dz,4,Blocks.PACKED_ICE);
                put(p,x,y,z,dx,5,dz,Blocks.SNOW);
            }
            break;
        case 2: // templo soterrado, duas paredes e um portal
            for (int dx = -3; dx <= 3; dx++) for (int dy = 1; dy <= 3; dy++)
                if (Math.abs(dx) == 3 || dy == 3) put(p,x,y,z,dx,dy,-3,Blocks.SANDSTONE);
            for (int dz = -2; dz <= 2; dz++) { pillar(p,x,y,z,-3,dz,2,Blocks.SANDSTONE); pillar(p,x,y,z,3,dz,2,Blocks.SANDSTONE); }
            break;
        case 3: // cabana aberta sobre passadiço
            for (int dx : new int[] {-3, 3}) for (int dz : new int[] {-3, 3}) pillar(p,x,y,z,dx,dz,4,Blocks.LOG);
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) put(p,x,y,z,dx,5,dz,Blocks.PLANKS);
            break;
        case 4: // forja em ruínas
            for (int dx = -3; dx <= 3; dx++) pillar(p,x,y,z,dx,-3,2,Blocks.NETHER_BRICK);
            pillar(p,x,y,z,-3,2,3,Blocks.NETHER_BRICK); pillar(p,x,y,z,3,2,3,Blocks.NETHER_BRICK);
            put(p,x,y,z,0,3,-3,Blocks.MAGMA);
            break;
        case 5: // santuário claro com quatro pilares rosados
            for (int dx : new int[] {-2, 2}) for (int dz : new int[] {-2, 2}) {
                pillar(p,x,y,z,dx,dz,3,Blocks.QUARTZ_BLOCK);
                p.put(new BlockPos(x+dx,y+4,z+dz), Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(6));
            }
            break;
        case 6: // portal umbral, centro livre para abrir o baú
            for (int dx : new int[] {-3, 3}) pillar(p,x,y,z,dx,-2,5,Blocks.OBSIDIAN);
            for (int dx = -3; dx <= 3; dx++) put(p,x,y,z,dx,5,-2,Blocks.OBSIDIAN);
            put(p,x,y,z,-2,1,2,Blocks.END_STONE); put(p,x,y,z,2,1,2,Blocks.END_STONE);
            break;
        default: // plataforma dos titãs com gigantescos postes
            for (int dx : new int[] {-3, 3}) for (int dz : new int[] {-3, 3}) pillar(p,x,y,z,dx,dz,6,Blocks.LOG);
            for (int dx = -2; dx <= 2; dx++) { put(p,x,y,z,dx,4,-3,Blocks.PLANKS); put(p,x,y,z,dx,4,3,Blocks.PLANKS); }
            break;
        }
    }
}
