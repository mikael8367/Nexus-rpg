package com.nexusrpg.energy;

import com.nexusrpg.NexusRpg;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.Explosion;
import net.minecraftforge.common.ForgeHooks;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemStackHandler;

public final class BlockPowerMachine extends Block {
    // Not an ItemBlock: the payload is never automatically applied to a tile.
    public static final Item COAL_RECOVERY = new Item().setMaxStackSize(1)
        .setRegistryName(NexusRpg.MODID, "coal_recovery")
        .setUnlocalizedName(NexusRpg.MODID + ".coal_recovery");
    private final boolean solar;
    private static final ThreadLocal<Boolean> SOLAR_CLEANUP = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> SOLAR_CREATIVE = ThreadLocal.withInitial(() -> false);
    public BlockPowerMachine(String id, boolean solar) {
        super(Material.IRON);
        this.solar = solar;
        setRegistryName(NexusRpg.MODID, id);
        setUnlocalizedName(NexusRpg.MODID + "." + id);
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(3.0F);
        setHarvestLevel("pickaxe", 1);
    }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) {
        return solar ? new TileSolarPanel() : new TileCoalGenerator();
    }
    private boolean room(World world, BlockPos center, boolean centerAlreadyPlaced) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos p = center.add(x, 0, z);
            // A single chunk ensures that dismantling never leaves unloaded parts behind.
            if ((p.getX() >> 4) != (center.getX() >> 4) || (p.getZ() >> 4) != (center.getZ() >> 4)
                    || !world.isBlockLoaded(p) || !world.getWorldBorder().contains(p)) return false;
            if (centerAlreadyPlaced && x == 0 && z == 0) continue;
            // Do not replace even liquids, plants, snow, or other replaceable blocks.
            if (world.getBlockState(p).getBlock() != Blocks.AIR || world.getTileEntity(p) != null) return false;
        }
        return true;
    }
    @Override public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && (!solar || room(world, pos, false));
    }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!solar || world.isRemote) return;
        EntityPlayer player = placer instanceof EntityPlayer ? (EntityPlayer) placer : null;
        boolean ok = player != null && room(world, pos, true) && world.getTileEntity(pos) instanceof TileSolarPanel;
        if (ok) for (int x = -1; x <= 1 && ok; x++) for (int z = -1; z <= 1; z++) {
            BlockPos p = pos.add(x, 0, z);
            if (!player.canPlayerEdit(p, EnumFacing.UP, stack)) { ok = false; break; }
        }
        UUID id = UUID.randomUUID();
        List<BlockPos> placed = new ArrayList<>();
        if (ok) {
            ((TileSolarPanel) world.getTileEntity(pos)).configure(pos, id);
            for (int x = -1; x <= 1 && ok; x++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos p = pos.add(x, 0, z);
                if (world.getBlockState(p).getBlock() != Blocks.AIR || world.getTileEntity(p) != null) { ok = false; break; }
                BlockSnapshot before = BlockSnapshot.getBlockSnapshot(world, p);
                if (!world.setBlockState(p, state, 2)) { ok = false; break; }
                placed.add(p);
                TileEntity tile = world.getTileEntity(p);
                if (!(tile instanceof TileSolarPanel)) { ok = false; break; }
                ((TileSolarPanel) tile).configure(pos, id);
                // Check each claimed position in addition to Forge's final multi-place event.
                EnumHand hand = player.getHeldItemOffhand() == stack ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
                if (ForgeEventFactory.onPlayerBlockPlace(player, before, EnumFacing.UP, hand).isCanceled()) { ok = false; break; }
            }
        }
        if (!ok) {
            SOLAR_CLEANUP.set(true);
            try {
                for (BlockPos p : placed) {
                    TileEntity tile = world.getTileEntity(p);
                    if (world.getBlockState(p).getBlock() == this && tile instanceof TileSolarPanel
                            && ((TileSolarPanel) tile).belongsTo(pos, id)) world.setBlockToAir(p);
                }
                if (world.getBlockState(pos).getBlock() == this) world.setBlockToAir(pos);
            } finally { SOLAR_CLEANUP.set(false); }
            // ItemBlock shrinks the same stack after this callback. Compensating here also
            // works with Forge's snapshot rollback, which restores the original count.
            if (player != null) stack.grow(1);
        }
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float x, float y, float z) {
        if (hand != EnumHand.MAIN_HAND) return false;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileCoalGenerator) && !(tile instanceof TileSolarPanel)) return false;
        if (!world.isRemote) {
            // Before the GUI existed vanilla retried the interaction with the
            // off hand when the main hand was not fuel; preserve that shortcut.
            EnumHand fuelHand = hand;
            if (tile instanceof TileCoalGenerator && !((TileCoalGenerator) tile).isFuel(player.getHeldItem(hand))
                    && ((TileCoalGenerator) tile).isFuel(player.getHeldItemOffhand())) fuelHand = EnumHand.OFF_HAND;
            ItemStack held = player.getHeldItem(fuelHand);
            if (tile instanceof TileCoalGenerator && ((TileCoalGenerator) tile).isFuel(held)) {
                player.setHeldItem(fuelHand, ((TileCoalGenerator) tile).inventory.insertItem(0, held, false));
            } else if (tile instanceof TileSolarPanel) {
                TileSolarPanel part = (TileSolarPanel) tile;
                BlockPos origin = part.origin();
                if (origin != null && part.structure() != null && world.isBlockLoaded(origin)) {
                    TileEntity controller = world.getTileEntity(origin);
                    if (controller instanceof TileSolarPanel && ((TileSolarPanel) controller).belongsTo(origin, part.structure()))
                        MachineGuiHandler.open(player, world, origin);
                }
            } else MachineGuiHandler.open(player, world, pos);
        }
        return true;
    }
    @Override public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (!solar) {
            if (world.captureBlockSnapshots || world.restoringBlockSnapshots) return false;
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileCoalGenerator && ((TileCoalGenerator) tile).hasOpaqueRecoveryData()) return false;
            return super.removedByPlayer(state, world, pos, player, willHarvest);
        }
        if (!world.isRemote) {
            if (!(player instanceof EntityPlayerMP)) return false;
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileSolarPanel) {
                TileSolarPanel panel = (TileSolarPanel) tile;
                BlockPos origin = panel.origin();
                UUID id = panel.structure();
                if (origin != null && id != null) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    BlockPos p = origin.add(x, 0, z);
                    if (p.equals(pos)) continue;
                    if (!world.isBlockLoaded(p)) return false;
                    TileEntity other = world.getTileEntity(p);
                    if (world.getBlockState(p).getBlock() == this && other instanceof TileSolarPanel
                            && ((TileSolarPanel) other).belongsTo(origin, id)
                            && ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, p) < 0) return false;
                }
            }
        }
        SOLAR_CREATIVE.set(player.capabilities.isCreativeMode);
        try { return super.removedByPlayer(state, world, pos, player, willHarvest); }
        finally { SOLAR_CREATIVE.set(false); }
    }
    @Override public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        // Do not bypass explosion protections on the other eight cells.
        if (!solar) {
            if (world.captureBlockSnapshots || world.restoringBlockSnapshots) return;
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileCoalGenerator && ((TileCoalGenerator) tile).hasOpaqueRecoveryData()) return;
            super.onBlockExploded(world, pos, explosion);
        }
    }
    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (!solar) {
            if (world instanceof World && (((World) world).captureBlockSnapshots || ((World) world).restoringBlockSnapshots)) return;
            // Explosion.doExplosionB asks for drops before onBlockExploded.
            // If destruction is refused, emitting a block here duplicates it.
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileCoalGenerator && ((TileCoalGenerator) tile).hasOpaqueRecoveryData()) return;
            super.getDrops(drops, world, pos, state, fortune);
        }
    }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && !world.captureBlockSnapshots && !world.restoringBlockSnapshots && tile instanceof TileCoalGenerator) {
            TileCoalGenerator coal = (TileCoalGenerator) tile;
            if (coal.hasOpaqueRecoveryData()) {
                // No BlockEntityTag, automatic restoration, or separate active inventory
                // drop: an opaque archive may already contain that same inventory.
                ItemStack recovery = new ItemStack(COAL_RECOVERY);
                NBTTagCompound payload = new NBTTagCompound();
                payload.setTag("NexusRpgCoalRecovery", coal.writeToNBT(new NBTTagCompound()));
                recovery.setTagCompound(payload);
                spawnAsEntity(world, pos, recovery);
            } else {
                ItemStackHandler inv = coal.inventory;
                ItemStack stack = inv.extractItem(0, 64, false);
                if (!stack.isEmpty()) spawnAsEntity(world, pos, stack);
            }
        }
        if (solar && !world.isRemote && !world.restoringBlockSnapshots && !SOLAR_CLEANUP.get()) {
            TileSolarPanel solarTile = tile instanceof TileSolarPanel ? (TileSolarPanel) tile : null;
            BlockPos origin = solarTile == null ? null : solarTile.origin();
            UUID id = solarTile == null ? null : solarTile.structure();
            SOLAR_CLEANUP.set(true);
            try {
                if (origin != null && id != null) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                    BlockPos p = origin.add(x, 0, z);
                    if (p.equals(pos) || !world.isBlockLoaded(p)) continue;
                    TileEntity other = world.getTileEntity(p);
                    if (world.getBlockState(p).getBlock() == this && other instanceof TileSolarPanel
                            && ((TileSolarPanel) other).belongsTo(origin, id)) world.setBlockToAir(p);
                }
            } finally { SOLAR_CLEANUP.set(false); }
            if (!SOLAR_CREATIVE.get()) spawnAsEntity(world, pos, new ItemStack(this));
        }
        super.breakBlock(world, pos, state);
    }
}
