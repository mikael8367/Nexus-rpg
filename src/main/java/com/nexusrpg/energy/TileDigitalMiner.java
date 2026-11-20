package com.nexusrpg.energy;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

/**
 * 16x128x16 (x/z offsets -8..7, y 0..127), overworld only. No chunk is
 * loaded by this scanner. Every 10 ticks it examines up to 512 positions and
 * mines at most one ore for 200 FE. Unloaded positions are revisited on wrap.
 * Full output pauses at the same ore, without spending FE. Receives up to 1000 FE per call,
 * holds 100000 FE, never outputs FE. Output is extract-only on every face.
 * Right-click retrieves one stack (or the part that fits); breaking the miner
 * drops its contents. No Silk Touch or Fortune; Forge harvest drops apply.
 * The placing player is represented by a Forge FakePlayer for break events;
 * machines without a player owner do not mine. Protection plugins must support
 * Forge fake-player break events; uncooperative claim systems are not guaranteed.
 * Tile-bearing and unbreakable blocks are never mined.
 */
public class TileDigitalMiner extends TileEntity implements ITickable {
    private static final int SIZE = 16 * 128 * 16, COST = 200, CAPACITY = 100000;
    private int energy, cursor, cooldown;
    private UUID ownerId;
    private String ownerName = "";
    private boolean processing;
    private final ItemStackHandler inventory = new ItemStackHandler(27) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    private final IItemHandler output = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot).copy(); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return processing ? ItemStack.EMPTY : inventory.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
    };
    private final IEnergyStorage power = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = Math.min(Math.min(Math.max(0, amount), 1000), CAPACITY - energy);
            if (!simulate && received > 0) { energy += received; markDirty(); }
            return received;
        }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energy; }
        @Override public int getMaxEnergyStored() { return CAPACITY; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    public void setOwner(EntityPlayer player) {
        GameProfile profile = player.getGameProfile();
        ownerId = profile.getId(); ownerName = profile.getName(); markDirty();
    }

    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }
    @Nullable @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(power);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(output);
        return super.getCapability(capability, facing);
    }

    public void giveFirstStack(EntityPlayer player) {
        if (processing) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            if (!existing.isEmpty()) {
                ItemStack offered = existing.copy();
                int before = offered.getCount();
                player.inventory.addItemStackToInventory(offered);
                int accepted = before - offered.getCount();
                if (accepted > 0) inventory.extractItem(i, accepted, false);
                return;
            }
        }
    }
    public void dropInventory() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.extractItem(i, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) Block.spawnAsEntity(world, pos, stack);
        }
    }

    @Override public void update() {
        if (world == null || world.isRemote || !(world instanceof WorldServer)
                || world.provider.getDimension() != 0 || ownerId == null || processing) return;
        if (cooldown > 0) { cooldown--; return; }
        cooldown = 9;
        if (energy < COST) return;
        processing = true;
        try {
            for (int i = 0; i < 512; i++) {
                int index = cursor;
                BlockPos target = pos.add((index & 15) - 8, 0, ((index >> 4) & 15) - 8);
                target = new BlockPos(target.getX(), index >> 8, target.getZ());
                if (!world.isBlockLoaded(target, false) || target.equals(pos)) { advance(); continue; }
                IBlockState state = world.getBlockState(target);
                Block block = state.getBlock();
                if (!isOre(state) || block.hasTileEntity(state) || world.getTileEntity(target) != null
                        || state.getBlockHardness(world, target) < 0) { advance(); continue; }
                FakePlayer fake = FakePlayerFactory.get((WorldServer) world, new GameProfile(ownerId, ownerName));
                // ForgeHooks.onBlockBreakEvent sends packets through a real player's
                // connection; FakePlayer has no connection. Post the same event directly.
                fake.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (!world.isBlockModifiable(fake, target)) { advance(); continue; }
                BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, target, state, fake);
                boolean denied = MinecraftForge.EVENT_BUS.post(event);
                int xp = event.getExpToDrop();
                if (denied || !world.isBlockLoaded(target, false) || world.getBlockState(target) != state
                        || world.getTileEntity(target) != null || world.getTileEntity(pos) != this) { advance(); continue; }
                NonNullList<ItemStack> raw = NonNullList.create();
                block.getDrops(raw, world, target, state, 0);
                float chance = ForgeEventFactory.fireBlockHarvesting(raw, world, target, state, 0, 1.0F, false, fake);
                List<ItemStack> drops = new ArrayList<>();
                for (ItemStack stack : raw) if (!stack.isEmpty() && world.rand.nextFloat() < chance) drops.add(stack.copy());
                // Event listeners can alter the world, machine, inventory or FE.
                if (!world.isBlockLoaded(target, false) || world.getBlockState(target) != state
                        || world.getTileEntity(target) != null || world.getTileEntity(pos) != this || energy < COST) { advance(); continue; }
                if (!fits(drops)) return; // retry this ore, no energy consumed
                if (!world.setBlockToAir(target)) { advance(); continue; }
                energy -= COST; markDirty();
                if (world.getTileEntity(pos) == this) {
                    for (ItemStack stack : drops) {
                        ItemStack remainder = insert(inventory, stack);
                        if (!remainder.isEmpty()) Block.spawnAsEntity(world, target, remainder);
                    }
                } else {
                    // An unusual block callback removed the machine while mining.
                    for (ItemStack stack : drops) Block.spawnAsEntity(world, target, stack);
                }
                if (xp > 0) block.dropXpOnBlockBreak(world, target, xp);
                advance();
                return;
            }
        } finally { processing = false; }
    }
    private void advance() { cursor = (cursor + 1) % SIZE; markDirty(); }

    private boolean isOre(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockOre) return true;
        ResourceLocation name = block.getRegistryName();
        // Nexus RPG ores are ordinary Block instances, before OreDictionary registration.
        if (name != null && ("nexusrpg:mithril_ore".equals(name.toString())
                || "nexusrpg:adamantite_ore".equals(name.toString())
                || "nexusrpg:draconite_ore".equals(name.toString()))) return true;
        Item item = Item.getItemFromBlock(block);
        if (item == null) return false;
        ItemStack variant = new ItemStack(item, 1, block.getMetaFromState(state));
        if (variant.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(variant))
            if (OreDictionary.getOreName(id).startsWith("ore")) return true;
        return false;
    }
    private boolean fits(List<ItemStack> drops) {
        ItemStackHandler simulation = new ItemStackHandler(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) simulation.setStackInSlot(i, inventory.getStackInSlot(i).copy());
        for (ItemStack stack : drops) if (!insert(simulation, stack.copy()).isEmpty()) return false;
        return true;
    }
    private static ItemStack insert(ItemStackHandler handler, ItemStack stack) {
        ItemStack remainder = stack;
        for (int i = 0; i < handler.getSlots() && !remainder.isEmpty(); i++) remainder = handler.insertItem(i, remainder, false);
        return remainder;
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Energy", energy); nbt.setInteger("Cursor", cursor);
        nbt.setInteger("Cooldown", cooldown); nbt.setTag("Output", inventory.serializeNBT());
        if (ownerId != null) { nbt.setString("OwnerUUID", ownerId.toString()); nbt.setString("OwnerName", ownerName); }
        return nbt;
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        energy = Math.max(0, Math.min(CAPACITY, nbt.getInteger("Energy")));
        cursor = Math.floorMod(nbt.getInteger("Cursor"), SIZE);
        cooldown = Math.max(0, Math.min(9, nbt.getInteger("Cooldown")));
        ownerId = null; ownerName = nbt.getString("OwnerName");
        try { if (nbt.hasKey("OwnerUUID")) ownerId = UUID.fromString(nbt.getString("OwnerUUID")); }
        catch (IllegalArgumentException ignored) { ownerId = null; }
        if (nbt.hasKey("Output", 10)) inventory.deserializeNBT(nbt.getCompoundTag("Output"));
    }
}
