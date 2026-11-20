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
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isFuel(stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return isFuel(stack); }
    };
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
        super.writeToNBT(tag); tag.setInteger("BurnTicks", burnTicks); tag.setTag("FuelInventory", inventory.serializeNBT()); return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag); burnTicks = Math.max(0, Math.min(1600, tag.getInteger("BurnTicks")));
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        if (tag.hasKey("FuelInventory", 10)) {
            net.minecraft.nbt.NBTTagList items = tag.getCompoundTag("FuelInventory").getTagList("Items", 10);
            for (int i = 0; i < Math.min(items.tagCount(), 64); i++) {
                NBTTagCompound entry = items.getCompoundTagAt(i);
                if (entry.getInteger("Slot") != 0) continue;
                ItemStack loaded = new ItemStack(entry);
                if (isFuel(loaded)) {
                    loaded.setCount(Math.min(loaded.getCount(), Math.min(64, loaded.getMaxStackSize())));
                    inventory.setStackInSlot(0, loaded);
                }
                break;
            }
        }
    }
}
