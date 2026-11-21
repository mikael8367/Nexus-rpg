package com.nexusrpg.energy;

import javax.annotation.Nullable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class TileCoalGenerator extends TileProducer {
    private int burnTicks;
    private boolean incompatibleVersion;
    // Archives are raw NBT, including wrong types; never treat them as live items.
    private static final String FUEL_ARCHIVE = "NexusRpgOriginalFuelInventory";
    private final NBTTagCompound fuelArchives = new NBTTagCompound();
    // Unknown roots and incompatible numeric values remain opaque, never active state.
    private final NBTTagCompound extraRoots = new NBTTagCompound();
    private NBTTagCompound futureFields;
    private NBTBase rawForgeData;
    private NBTBase rawForgeCaps;

    private static void preserve(NBTTagCompound target, String prefix, NBTBase value) {
        for (String key : target.getKeySet())
            if ((key.equals(prefix) || key.startsWith(prefix + "_")) && value.equals(target.getTag(key))) return;
        String key = prefix;
        int suffix = 1;
        while (target.hasKey(key)) key = prefix + "_" + suffix++;
        target.setTag(key, value.copy());
    }
    private static boolean ownedRoot(String key) {
        return key.equals("id") || key.equals("x") || key.equals("y") || key.equals("z")
            || key.equals("ForgeData") || key.equals("ForgeCaps") || key.equals("Energy")
            || key.equals("BurnTicks") || key.equals("FuelInventory") || key.equals("NexusRpgSchemaVersion") || key.startsWith(FUEL_ARCHIVE);
    }
    private static void archive(NBTTagCompound archives, NBTBase value) {
        // Repeated loads must not create unlimited copies of the same data.
        for (String key : archives.getKeySet())
            if (key.startsWith(FUEL_ARCHIVE) && value.equals(archives.getTag(key))) return;
        String key = FUEL_ARCHIVE;
        int suffix = 1;
        while (archives.hasKey(key)) key = FUEL_ARCHIVE + "_" + suffix++;
        archives.setTag(key, value.copy());
    }
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (incompatibleVersion && !stack.isEmpty()) throw new IllegalStateException("Cannot activate inventory with an unsupported schema");
            super.setStackInSlot(slot, stack);
        }
        @Override public void deserializeNBT(NBTTagCompound nbt) {
            if (incompatibleVersion) throw new IllegalStateException("Cannot deserialize inventory with an unsupported schema");
            super.deserializeNBT(nbt);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return !incompatibleVersion && isFuel(stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return !incompatibleVersion && isFuel(stack); }
    };
    public int getBurnTicks() { return burnTicks; }
    // Ordinary removal stays blocked: only forced removals reach the inert carrier.
    boolean hasOpaqueRecoveryData() {
        if (incompatibleVersion || rawForgeData != null || rawForgeCaps != null || !fuelArchives.hasNoTags() || !extraRoots.hasNoTags()) return true;
        NBTTagCompound saved = writeToNBT(new NBTTagCompound());
        return saved.hasKey("ForgeData") || saved.hasKey("ForgeCaps");
    }
    public boolean isFuel(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == Items.COAL && (stack.getMetadata() == 0 || stack.getMetadata() == 1); }
    @Override public void update() {
        if (incompatibleVersion || world == null || world.isRemote) return;
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
        if (futureFields != null) {
            for (String key : new String[]{"NexusRpgSchemaVersion", "Energy", "BurnTicks", "FuelInventory"})
                if (tag.hasKey(key) && (!futureFields.hasKey(key) || !tag.getTag(key).equals(futureFields.getTag(key))))
                    throw new IllegalStateException("Conflicting future schema output key: " + key);
        }
        for (String key : new String[]{"ForgeData", "ForgeCaps"}) {
            NBTBase raw = key.equals("ForgeData") ? rawForgeData : rawForgeCaps;
            if (raw != null && tag.hasKey(key) && !raw.equals(tag.getTag(key)))
                throw new IllegalStateException("Conflicting Forge output key: " + key);
        }
        super.writeToNBT(tag); tag.setInteger("BurnTicks", burnTicks); tag.setTag("FuelInventory", inventory.serializeNBT());
        if (futureFields != null) {
            for (String key : new String[]{"NexusRpgSchemaVersion", "Energy", "BurnTicks", "FuelInventory"}) {
                tag.removeTag(key);
                if (futureFields.hasKey(key)) tag.setTag(key, futureFields.getTag(key).copy());
            }
        }
        restoreForge(tag, "ForgeData", rawForgeData);
        restoreForge(tag, "ForgeCaps", rawForgeCaps);
        for (String key : fuelArchives.getKeySet()) {
            NBTBase value = fuelArchives.getTag(key);
            if (!tag.hasKey(key)) tag.setTag(key, value.copy());
            else if (!value.equals(tag.getTag(key))) archive(tag, value);
        }
        for (String key : extraRoots.getKeySet()) {
            NBTBase value = extraRoots.getTag(key);
            if (!tag.hasKey(key)) tag.setTag(key, value.copy());
            else if (!value.equals(tag.getTag(key))) {
                NBTTagCompound collision = new NBTTagCompound();
                collision.setString("Key", key); collision.setTag("Value", value.copy());
                preserve(tag, "NexusRpgPreservedRoot", collision);
            }
        }
        return tag;
    }
    private static void restoreForge(NBTTagCompound tag, String key, NBTBase raw) {
        if (raw == null) return;
        NBTBase live = tag.getTag(key);
        if (raw instanceof NBTTagCompound) {
            // ForgeData is live through TileEntity. Merge unregistered caps
            // without shadowing attached, actively serialized capabilities.
            if (key.equals("ForgeData")) return;
            if (live != null && !(live instanceof NBTTagCompound))
                throw new IllegalStateException("Cannot safely merge " + key);
            NBTTagCompound merged = live == null ? new NBTTagCompound() : ((NBTTagCompound) live).copy();
            NBTTagCompound original = (NBTTagCompound) raw;
            for (String name : original.getKeySet())
                if (!merged.hasKey(name)) merged.setTag(name, original.getTag(name).copy());
            tag.setTag(key, merged);
        } else {
            // One key cannot hold both malformed raw data and active state.
            if (live != null && (!(live instanceof NBTTagCompound) || !((NBTTagCompound) live).hasNoTags()))
                throw new IllegalStateException("Cannot safely preserve malformed " + key + " alongside active data");
            tag.setTag(key, raw.copy());
        }
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        rawForgeData = tag.hasKey("ForgeData") ? tag.getTag("ForgeData").copy() : null;
        rawForgeCaps = tag.hasKey("ForgeCaps") ? tag.getTag("ForgeCaps").copy() : null;
        NBTTagCompound safe = tag.copy();
        if (rawForgeData != null && !(rawForgeData instanceof NBTTagCompound)) safe.removeTag("ForgeData");
        if (rawForgeCaps != null && !(rawForgeCaps instanceof NBTTagCompound)) safe.removeTag("ForgeCaps");
        super.readFromNBT(safe); burnTicks = Math.max(0, Math.min(1600, tag.getInteger("BurnTicks")));
        incompatibleVersion = tag.hasKey("NexusRpgSchemaVersion");
        futureFields = incompatibleVersion ? new NBTTagCompound() : null;
        for (String key : new java.util.HashSet<String>(extraRoots.getKeySet())) extraRoots.removeTag(key);
        for (String key : tag.getKeySet())
            if (!ownedRoot(key)) extraRoots.setTag(key, tag.getTag(key).copy());
        if (!incompatibleVersion && tag.hasKey("Energy") && (!tag.hasKey("Energy", 3) || tag.getInteger("Energy") < 0 || tag.getInteger("Energy") > CAPACITY)) {
            preserve(extraRoots, "NexusRpgOriginalEnergy", tag.getTag("Energy"));
            energy = 0;
        }
        if (!incompatibleVersion && tag.hasKey("BurnTicks") && (!tag.hasKey("BurnTicks", 3) || tag.getInteger("BurnTicks") < 0 || tag.getInteger("BurnTicks") > 1600)) {
            preserve(extraRoots, "NexusRpgOriginalBurnTicks", tag.getTag("BurnTicks"));
            burnTicks = 0;
        }
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        for (String key : new java.util.HashSet<String>(fuelArchives.getKeySet())) fuelArchives.removeTag(key);
        for (String key : tag.getKeySet())
            if (key.startsWith(FUEL_ARCHIVE)) fuelArchives.setTag(key, tag.getTag(key).copy());
        // This implementation has no versioned schema. A version marker is
        // therefore incompatible, regardless of its type or numeric value.
        if (tag.hasKey("NexusRpgSchemaVersion")) {
            for (String key : new String[]{"NexusRpgSchemaVersion", "Energy", "BurnTicks", "FuelInventory"})
                if (tag.hasKey(key)) futureFields.setTag(key, tag.getTag(key).copy());
            energy = 0; burnTicks = 0;
            return;
        }
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
            ItemStack candidate = ItemStack.EMPTY;
            for (int i = 0; i < Math.min(items.tagCount(), 64); i++) {
                NBTTagCompound entry = items.getCompoundTagAt(i);
                if (!entry.hasKey("Slot", 3) || entry.getInteger("Slot") != 0 || loadedSlot) {
                    malformed = true; continue;
                }
                loadedSlot = true;
                ItemStack loaded = new ItemStack(entry);
                if (loaded.isEmpty()) { malformed = true; continue; }
                int limit = Math.min(64, loaded.getMaxStackSize());
                if (loaded.getCount() > limit) { malformed = true; continue; }
                // Valid legacy non-fuel items remain recoverable; malformed
                // inventories are opaque and never become live items.
                candidate = loaded;
            }
            // Compare before activating: divergent inventories are exclusively opaque.
            ItemStackHandler trial = new ItemStackHandler(1);
            if (!malformed) trial.setStackInSlot(0, candidate);
            if (malformed || !original.equals(trial.serializeNBT())) archive(fuelArchives, original);
            else inventory.setStackInSlot(0, candidate);
        } else if (tag.hasKey("FuelInventory")) {
            archive(fuelArchives, tag.getTag("FuelInventory"));
        }
    }
}
