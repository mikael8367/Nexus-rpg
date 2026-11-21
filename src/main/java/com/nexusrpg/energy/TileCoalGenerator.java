package com.nexusrpg.energy;

import javax.annotation.Nullable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class TileCoalGenerator extends TileProducer {
    private int burnTicks;
    // Keep anomalous legacy inventory data as an archival backup, not live items.
    @Nullable private NBTTagCompound preservedFuelInventory;
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isFuel(stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return isFuel(stack); }
    };
    public int getBurnTicks() { return burnTicks; }
    public boolean isFuel(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == Items.COAL && (stack.getMetadata() == 0 || stack.getMetadata() == 1); }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (energy <= CAPACITY - 40) {
            if (burnTicks == 0 && isFuel(inventory.getStackInSlot(0))) {
                inventory.extractItem(0, 1, false);
                burnTicks = 1600;
            }
            if (burnTicks > 0) { burnTicks--; addEnergy(40); }
        }
        emit(256);
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }
    @Override @Nullable public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        return super.getCapability(capability, facing);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag); tag.setInteger("BurnTicks", burnTicks); tag.setTag("FuelInventory", inventory.serializeNBT());
        if (preservedFuelInventory != null) tag.setTag("NexusRpgOriginalFuelInventory", preservedFuelInventory.copy());
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); burnTicks = Math.max(0, Math.min(1600, tag.getInteger("BurnTicks")));
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        preservedFuelInventory = tag.hasKey("NexusRpgOriginalFuelInventory", 10)
            ? tag.getCompoundTag("NexusRpgOriginalFuelInventory").copy() : null;
        if (tag.hasKey("FuelInventory", 10)) {
            NBTTagCompound original = tag.getCompoundTag("FuelInventory");
            net.minecraft.nbt.NBTTagList items = original.getTagList("Items", 10);
            boolean malformed = !original.hasKey("Size", 3) || original.getInteger("Size") != 1
                || (original.hasKey("Items") && !original.hasKey("Items", 9)) || items.tagCount() > 1;
            if (original.hasKey("Items", 9)) {
                net.minecraft.nbt.NBTTagList raw = (net.minecraft.nbt.NBTTagList) original.getTag("Items");
                if (raw.tagCount() > 0 && raw.getTagType() != 10) malformed = true;
            }
            boolean loadedSlot = false;
            for (int i = 0; i < Math.min(items.tagCount(), 64); i++) {
                NBTTagCompound entry = items.getCompoundTagAt(i);
                if (!entry.hasKey("Slot", 3) || entry.getInteger("Slot") != 0 || loadedSlot) {
                    malformed = true; continue;
                }
                loadedSlot = true;
                ItemStack loaded = new ItemStack(entry);
                if (loaded.isEmpty()) { malformed = true; continue; }
                int limit = Math.min(64, loaded.getMaxStackSize());
                if (loaded.getCount() > limit) { malformed = true; loaded.setCount(limit); }
                // Old saves can contain non-fuel items. Keep them extractable; new
                // insertions remain restricted by the normal item handler.
                inventory.setStackInSlot(0, loaded);
            }
            if (malformed && preservedFuelInventory == null) preservedFuelInventory = original.copy();
        }
    }
}
