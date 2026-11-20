package com.nexusrpg.energy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.SlotItemHandler;

/** Only the real machine inventory is used; no GUI inventory is persisted separately. */
public final class MachineContainer extends Container {
    final TileEntity tile;
    final int machineSlots;
    final int inventoryTop;
    private int energy, extra;
    private int lastEnergy = -1, lastExtra = -1;

    MachineContainer(InventoryPlayer player, TileEntity tile) {
        this.tile = tile;
        machineSlots = tile instanceof TileDigitalMiner ? 27 : tile instanceof TileCoalGenerator ? 1 : 0;
        inventoryTop = machineSlots == 27 ? 114 : 78;
        if (tile instanceof TileCoalGenerator) {
            addSlotToContainer(new SlotItemHandler(((TileCoalGenerator) tile).inventory, 0, 80, 51) {
                @Override public void onSlotChanged() { super.onSlotChanged(); tile.markDirty(); }
            });
        } else if (tile instanceof TileDigitalMiner) {
            TileDigitalMiner miner = (TileDigitalMiner) tile;
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
                addSlotToContainer(new SlotItemHandler(miner.guiInventory(), row * 9 + col, 8 + col * 18, 48 + row * 18) {
                    @Override public boolean isItemValid(ItemStack stack) { return false; }
                    @Override public boolean canTakeStack(EntityPlayer p) { return !miner.isProcessing() && super.canTakeStack(p); }
                });
            }
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, inventoryTop + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col, 8 + col * 18, inventoryTop + 58));
    }
    @Override public boolean canInteractWith(EntityPlayer player) {
        if (tile.isInvalid() || tile.getWorld() == null || player.world != tile.getWorld()) return false;
        BlockPos p = tile.getPos();
        return tile.getWorld().isBlockLoaded(p) && tile.getWorld().getTileEntity(p) == tile
            && player.getDistanceSq(p.getX() + .5, p.getY() + .5, p.getZ() + .5) <= 64.0;
    }
    @Override public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        // -999 is valid only for outside pickup or the start/end of a drag.
        // The vanilla SWAP and drag-add paths index the list without a check.
        boolean outside = slotId == -999 && (clickType == ClickType.PICKUP
            || (clickType == ClickType.QUICK_CRAFT && getDragEvent(dragType) != 1));
        if ((slotId < 0 && !outside) || slotId >= inventorySlots.size()) {
            resetDrag();
            return ItemStack.EMPTY;
        }
        if (!canInteractWith(player) || (tile instanceof TileDigitalMiner && ((TileDigitalMiner) tile).isProcessing())) return ItemStack.EMPTY;
        return super.slotClick(slotId, dragType, clickType, player);
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (!canInteractWith(player) || index < 0 || index >= inventorySlots.size() ||
            (tile instanceof TileDigitalMiner && ((TileDigitalMiner) tile).isProcessing())) return ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack() || !slot.canTakeStack(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < machineSlots) {
            if (!mergeItemStack(stack, machineSlots, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else if (tile instanceof TileCoalGenerator && ((TileCoalGenerator) tile).isFuel(stack)) {
            if (!mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            int mainEnd = machineSlots + 27;
            if (index < mainEnd) {
                if (!mergeItemStack(stack, mainEnd, inventorySlots.size(), false)) return ItemStack.EMPTY;
            } else if (!mergeItemStack(stack, machineSlots, mainEnd, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        // mergeItemStack mutates a source ItemStack directly on partial transfers.
        if (index < machineSlots) tile.markDirty();
        slot.onTake(player, stack);
        return original;
    }
    private int readEnergy() { return tile instanceof TileProducer ? ((TileProducer) tile).guiEnergy() : ((TileDigitalMiner) tile).guiEnergy(); }
    private int readExtra() {
        if (tile instanceof TileCoalGenerator) return ((TileCoalGenerator) tile).getBurnTicks();
        if (tile instanceof TileSolarPanel) return ((TileSolarPanel) tile).complete() ? 1 : 0;
        return 0;
    }
    private void send(IContainerListener listener, int value, int state) {
        listener.sendWindowProperty(this, 0, value & 65535);
        listener.sendWindowProperty(this, 1, value >>> 16 & 65535);
        listener.sendWindowProperty(this, 2, state);
    }
    @Override public void addListener(IContainerListener listener) {
        super.addListener(listener);
        send(listener, readEnergy(), readExtra());
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int value = readEnergy(), state = readExtra();
        if (value != lastEnergy || state != lastExtra) {
            for (IContainerListener listener : listeners) send(listener, value, state);
            lastEnergy = value; lastExtra = state;
        }
    }
    @Override public void updateProgressBar(int id, int value) {
        if (id == 0) energy = (energy & 0xffff0000) | (value & 65535);
        else if (id == 1) energy = (energy & 65535) | ((value & 65535) << 16);
        else if (id == 2) extra = value & 65535;
    }
    int energy() { return energy; }
    int extra() { return extra; }
}
