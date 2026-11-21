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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
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
 * Chunks loaded from disk are never eligible, even if population was incomplete.
 */
public final class NexusStructureGenerator implements IWorldGenerator {
    // Candidate rate, before biome, terrain and safety checks; not a guarantee.
    private static final int CANDIDATE_CHANCE = 4;
    // Towers get three tickets; every other design keeps one. Selection only
    // happens after all terrain and protection checks, never by forcing a site.
    private static final int TOWER_TICKETS = 3;
    // Reject thin cave roofs and thin floating shelves under the whole footprint.
    private static final int FOUNDATION_DEPTH = 5;
    // A live loaded chunk holds its own identity; weak keys do not retain unloaded chunks/worlds.
    private final Set<Chunk> loadedFromDisk = Collections.synchronizedSet(
        Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>()));

    public NexusStructureGenerator() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (!event.getWorld().isRemote) {
            // Never retrofit a saved chunk, including partially populated ones.
            loadedFromDisk.add(event.getChunk());
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.isRemote || world.provider.getDimension() != 0 || random.nextInt(CANDIDATE_CHANCE) != 0) return;
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
        Map<BlockPos, IBlockState> foundation = new LinkedHashMap<BlockPos, IBlockState>();
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            int x = cx + dx, z = cz + dz;
            if (chunk.getBiome(new BlockPos(x, 0, z), world.getBiomeProvider()) != biome) return;
            int h = chunk.getHeightValue(x & 15, z & 15);
            if (h < FOUNDATION_DEPTH || h + 12 >= world.getHeight()) return;
            Block surface = chunk.getBlockState(x & 15, h - 1, z & 15).getBlock();
            if (!isGround(surface)) return;
            heights[dx + 4][dz + 4] = h;
            if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3) {
                min = Math.min(min, h); max = Math.max(max, h);
                for (int depth = 1; depth <= FOUNDATION_DEPTH; depth++) {
                    BlockPos base = new BlockPos(x, h - depth, z);
                    IBlockState state = chunk.getBlockState(base);
                    if (!isFoundation(state.getBlock()) || state.getBlock().hasTileEntity(state) ||
                        chunk.getTileEntityMap().containsKey(base)) return;
                    foundation.put(base, state);
                }
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
        // One ticket per original/nova design, three for a supported tower.
        int[] first = {0, 7, 14, 20, 26, 32, 38, 44};
        int[] count = {7, 7, 6, 6, 6, 6, 6, 6};
        boolean eligibleTower = WizardTower.supports(style);
        int choice = random.nextInt(count[style] + 1 + (eligibleTower ? TOWER_TICKETS : 0));
        boolean wizardTower = eligibleTower && choice >= count[style] + 1;
        if (choice == 0) {
            decorate(plan, cx, max, cz, style);
        } else {
            int design = first[style] + choice - 1;
            Map<BlockPos, IBlockState> decoration = new LinkedHashMap<BlockPos, IBlockState>();
            try {
                if (wizardTower) WizardTower.build(decoration, cx, max, cz, style);
                else if (design <= 16) NovaStructuresA.build(decoration, cx, max, cz, design, style);
                else if (design <= 33) NovaStructuresB.build(decoration, cx, max, cz, design, style);
                else NovaStructuresC.build(decoration, cx, max, cz, design, style);
            } catch (IllegalArgumentException invalidPlan) {
                return;
            }
            if (!validNewDecoration(decoration, cx, max, cz)) return;
            plan.putAll(decoration);
        }
        // Original designs also need a reachable chest; the surrounding ring may
        // be too low even when the platform's own foundation is flat and safe.
        if (!hasChestApproach(chunk, plan, cx, max, cz)) return;
        BlockPos chestPos = new BlockPos(cx, max + 1, cz);
        if (plan.containsKey(chestPos) || plan.containsKey(chestPos.up())) return;
        // Cover the full footprint and all possible planned heights, including slopes.
        // The loaded-entity list avoids querying or loading neighbouring chunks.
        AxisAlignedBB occupiedVolume = new AxisAlignedBB(cx - 3, min, cz - 3,
                                                        cx + 4, max + 11, cz + 4);
        if (hasBlockingEntity(world, occupiedVolume)) return;
        // Preserve plants, but allow snow layers: vanilla populates them before Forge
        // generators in snowy biomes. Snapshot exact states for conditional rollback.
        Map<BlockPos, IBlockState> original = new LinkedHashMap<BlockPos, IBlockState>();
        for (BlockPos pos : plan.keySet()) {
            IBlockState state = chunk.getBlockState(pos);
            if ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ ||
                (state.getBlock() != Blocks.AIR && state.getBlock() != Blocks.SNOW_LAYER) ||
                state.getBlock().hasTileEntity(state) ||
                chunk.getTileEntityMap().containsKey(pos)) return;
            original.put(pos, state);
        }
        if (chunk.getBlockState(chestPos).getBlock() != Blocks.AIR ||
            chunk.getBlockState(chestPos.up()).getBlock() != Blocks.AIR ||
            chunk.getTileEntityMap().containsKey(chestPos) ||
            chunk.getTileEntityMap().containsKey(chestPos.up())) return;

        List<BlockPos> attempted = new ArrayList<BlockPos>();
        boolean complete = false;
        boolean attemptedChest = false;
        try {
            // Build first, chest last: a rejected floor cannot leave a floating chest.
            for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet()) {
                BlockPos pos = entry.getKey();
                if (chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    hasBlockingEntity(world, occupiedVolume) ||
                    !chunk.getBlockState(pos).equals(original.get(pos)) ||
                    chunk.getTileEntityMap().containsKey(pos)) return;
                // Include failed writes: a mod may change a block while returning false.
                attempted.add(pos);
                if (!world.setBlockState(pos, entry.getValue(), 2) ||
                    chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk ||
                    !chunk.getBlockState(pos).equals(entry.getValue()) ||
                    chunk.getTileEntityMap().containsKey(pos)) return;
            }
            // Callbacks during later writes may have changed earlier positions or spawned entities.
            if (chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk) return;
            if (hasBlockingEntity(world, occupiedVolume)) return;
            if (!foundationUnchanged(chunk, foundation)) return;
            for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet())
                if (!chunk.getBlockState(entry.getKey()).equals(entry.getValue()) ||
                    chunk.getTileEntityMap().containsKey(entry.getKey())) return;
            if (chunk.getBlockState(chestPos).getBlock() != Blocks.AIR ||
                chunk.getBlockState(chestPos.up()).getBlock() != Blocks.AIR ||
                chunk.getTileEntityMap().containsKey(chestPos) ||
                chunk.getTileEntityMap().containsKey(chestPos.up())) return;
            // Chest creation and loot assignment are part of the same guarded operation.
            // A callback during chest placement can also invalidate earlier writes.
            attemptedChest = true;
            boolean placed = world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), 2);
            if (chunkProvider.getLoadedChunk(chunkX, chunkZ) != chunk) return;
            if (hasBlockingEntity(world, occupiedVolume)) return;
            IBlockState chestState = chunk.getBlockState(chestPos);
            TileEntity tile = chunk.getTileEntityMap().get(chestPos);
            if (!placed || chestState.getBlock() != Blocks.CHEST || !(tile instanceof TileEntityChest) ||
                chunk.getBlockState(chestPos.up()).getBlock() != Blocks.AIR ||
                chunk.getTileEntityMap().containsKey(chestPos.up())) return;
            if (!foundationUnchanged(chunk, foundation)) return;
            for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet())
                if (!chunk.getBlockState(entry.getKey()).equals(entry.getValue()) ||
                    chunk.getTileEntityMap().containsKey(entry.getKey())) return;
            ((TileEntityChest) tile).setLootTable(new ResourceLocation("nexusrpg", "chests/" + id(style)), random.nextLong());
            tile.markDirty();
            complete = true;
        } finally {
            if (!complete && chunkProvider.getLoadedChunk(chunkX, chunkZ) == chunk) {
                // A chest with a tile (or any third-party block/tile) may own data: never
                // remove it or its support. An exact tile-less chest has no inventory.
                IBlockState state = chunk.getBlockState(chestPos);
                TileEntity tile = chunk.getTileEntityMap().get(chestPos);
                if (attemptedChest && tile == null && state.equals(Blocks.CHEST.getDefaultState()))
                    world.setBlockState(chestPos, Blocks.AIR.getDefaultState(), 2);
                if (chunkProvider.getLoadedChunk(chunkX, chunkZ) == chunk &&
                    chunk.getBlockState(chestPos).getBlock() == Blocks.AIR &&
                    !chunk.getTileEntityMap().containsKey(chestPos))
                    rollback(world, chunkProvider, chunk, chunkX, chunkZ, plan, original, attempted);
            }
        }
        // Notify only after a successful complete tower, without loading chunks.
        if (complete && wizardTower && chunkProvider.getLoadedChunk(chunkX, chunkZ) == chunk) {
            BlockPos feet = WizardTower.spawnFeet(cx, max, cz);
            BlockPos support = new BlockPos(feet.getX(), feet.getY() - 1, feet.getZ());
            if (chunk.getBlockState(support).getBlock() == Blocks.PLANKS &&
                chunk.getBlockState(feet).getBlock() == Blocks.AIR &&
                chunk.getBlockState(feet.up()).getBlock() == Blocks.AIR &&
                !chunk.getTileEntityMap().containsKey(support) &&
                !chunk.getTileEntityMap().containsKey(feet) &&
                !chunk.getTileEntityMap().containsKey(feet.up()))
                WizardTower.afterGenerated(world, feet);
        }
    }

    /** Inspect already loaded entity boxes without querying or loading chunks. */
    private static boolean hasBlockingEntity(World world, AxisAlignedBB volume) {
        for (Entity entity : world.loadedEntityList) {
            if (!entity.isDead && (entity instanceof EntityLivingBase || entity.canBeCollidedWith()) &&
                entity.getEntityBoundingBox().intersects(volume)) return true;
        }
        return false;
    }

    private static boolean foundationUnchanged(Chunk chunk, Map<BlockPos, IBlockState> foundation) {
        for (Map.Entry<BlockPos, IBlockState> entry : foundation.entrySet())
            if (!chunk.getBlockState(entry.getKey()).equals(entry.getValue()) ||
                chunk.getTileEntityMap().containsKey(entry.getKey())) return false;
        return true;
    }

    /** Reject malformed, unsafe or out-of-envelope additions before any world write. */
    private static boolean validNewDecoration(Map<BlockPos, IBlockState> plan, int x, int y, int z) {
        if (plan.isEmpty()) return false;
        for (Map.Entry<BlockPos, IBlockState> entry : plan.entrySet()) {
            BlockPos pos = entry.getKey();
            IBlockState state = entry.getValue();
            if (pos == null || state == null || Math.abs((long) pos.getX() - x) > 3 ||
                Math.abs((long) pos.getZ() - z) > 3 || (long) pos.getY() - y < 1 || (long) pos.getY() - y > 10 ||
                (pos.getX() == x && pos.getZ() == z && (long) pos.getY() - y <= 2) ||
                state.getBlock().hasTileEntity(state) || !safeDecorationBlock(state.getBlock())) return false;
        }
        return true;
    }

    // Explicitly allow only inert, non-falling blocks used by these designs.
    private static boolean safeDecorationBlock(Block b) {
        return b == Blocks.STONEBRICK || b == Blocks.MOSSY_COBBLESTONE || b == Blocks.PLANKS ||
            b == Blocks.COBBLESTONE || b == Blocks.SANDSTONE || b == Blocks.BRICK_BLOCK ||
            b == Blocks.PRISMARINE || b == Blocks.LOG || b == Blocks.NETHER_BRICK ||
            b == Blocks.OBSIDIAN || b == Blocks.QUARTZ_BLOCK || b == Blocks.PURPUR_BLOCK ||
            b == Blocks.GLOWSTONE || b == Blocks.NETHERRACK || b == Blocks.END_BRICKS ||
            b == Blocks.END_STONE || b == Blocks.RED_NETHER_BRICK || b == Blocks.IRON_BARS ||
            b == Blocks.GLASS_PANE;
    }

    /** Two-block-high walk from a clear, reachable edge to a side of the chest. */
    private static boolean hasChestApproach(Chunk chunk, Map<BlockPos, IBlockState> plan,
                                            int x, int y, int z) {
        boolean[][] seen = new boolean[7][7];
        List<int[]> queue = new ArrayList<int[]>();
        for (int d = -3; d <= 3; d++) {
            addApproachStart(chunk, plan, x, y, z, -3, d, -4, d, seen, queue);
            addApproachStart(chunk, plan, x, y, z, 3, d, 4, d, seen, queue);
            addApproachStart(chunk, plan, x, y, z, d, -3, d, -4, seen, queue);
            addApproachStart(chunk, plan, x, y, z, d, 3, d, 4, seen, queue);
        }
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        for (int i = 0; i < queue.size(); i++) {
            int[] p = queue.get(i);
            if (Math.abs(p[0]) + Math.abs(p[1]) == 1) return true;
            for (int j = 0; j < 4; j++) {
                int a = p[0] + dx[j], b = p[1] + dz[j];
                if (a < -3 || a > 3 || b < -3 || b > 3 || (a == 0 && b == 0) ||
                    seen[a + 3][b + 3] || !walkable(chunk, plan, x + a, y, z + b)) continue;
                seen[a + 3][b + 3] = true;
                queue.add(new int[] {a, b});
            }
        }
        return false;
    }
    private static void addApproachStart(Chunk chunk, Map<BlockPos, IBlockState> plan, int x, int y, int z,
                                         int a, int b, int outsideX, int outsideZ,
                                         boolean[][] seen, List<int[]> queue) {
        BlockPos ground = new BlockPos(x + outsideX, y - 1, z + outsideZ);
        // Outside the raised platform, the surface must be at most one step down.
        if (!seen[a + 3][b + 3] && isGround(chunk.getBlockState(ground).getBlock()) &&
            walkable(chunk, plan, x + outsideX, y, z + outsideZ) &&
            walkable(chunk, plan, x + a, y, z + b)) {
            seen[a + 3][b + 3] = true;
            queue.add(new int[] {a, b});
        }
    }
    private static boolean walkable(Chunk chunk, Map<BlockPos, IBlockState> plan, int x, int y, int z) {
        for (int h = 1; h <= 2; h++) {
            BlockPos p = new BlockPos(x, y + h, z);
            IBlockState state = plan.containsKey(p) ? plan.get(p) : chunk.getBlockState(p);
            Block block = state.getBlock();
            // These vanilla plants have no collision and can be walked through.
            // Do not generalize to all replaceable blocks: deep snow can block the way.
            if (block != Blocks.AIR && block != Blocks.TALLGRASS && block != Blocks.YELLOW_FLOWER &&
                block != Blocks.RED_FLOWER && block != Blocks.DEADBUSH &&
                block != Blocks.BROWN_MUSHROOM && block != Blocks.RED_MUSHROOM) return false;
        }
        return true;
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
                if (bb.length != 6 || bb[0] > bb[3] || bb[1] > bb[4] || bb[2] > bb[5]) return true;
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
    private static boolean isFoundation(Block b) {
        return isNatural(b) && !isReplaceableSurface(b) && b != Blocks.WATER &&
            b != Blocks.FLOWING_WATER && b != Blocks.LAVA && b != Blocks.FLOWING_LAVA;
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
