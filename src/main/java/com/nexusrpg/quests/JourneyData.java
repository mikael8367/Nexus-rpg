package com.nexusrpg.quests;

import com.nexusrpg.character.CharacterEvents;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** Independent, additive, bounded milestones; unsupported data is never repaired. */
public final class JourneyData {
    public enum Journey {
        EXPLORER("NexusExplorerJourney_v1"), CRAFTER("NexusCrafterJourney_v1");
        public final String key;
        Journey(String key) { this.key = key; }
    }
    public static final int COMPLETE = 7;
    private JourneyData() {}
    /** -1 means unsupported; an absent section is valid and has no progress. */
    public static int mask(NBTTagCompound root, Journey journey) {
        if (root == null || journey == null) return -1;
        String p = EntityPlayer.PERSISTED_NBT_TAG;
        if (root.hasKey(p) && !root.hasKey(p, 10)) return -1;
        NBTTagCompound persisted = root.getCompoundTag(p);
        if (!persisted.hasKey(journey.key)) return 0;
        if (!persisted.hasKey(journey.key, 10)) return -1;
        NBTTagCompound data = persisted.getCompoundTag(journey.key);
        for (String key : data.getKeySet()) if (!key.equals("version") && !key.equals("mask")) return -1;
        if (!data.hasKey("version", 3) || data.getInteger("version") != 1 || !data.hasKey("mask", 3)) return -1;
        int mask = data.getInteger("mask");
        return mask >= 0 && (mask & ~COMPLETE) == 0 ? mask : -1;
    }
    /** A single new milestone; duplicates or unsupported sections are unchanged. */
    public static boolean record(NBTTagCompound root, Journey journey, int bit) {
        if (bit != 1 && bit != 2 && bit != 4) return false;
        int old = mask(root, journey);
        if (old < 0 || (old & bit) != 0) return false;
        String p = EntityPlayer.PERSISTED_NBT_TAG;
        if (!root.hasKey(p)) root.setTag(p, new NBTTagCompound());
        NBTTagCompound persisted = root.getCompoundTag(p);
        if (!persisted.hasKey(journey.key)) {
            NBTTagCompound section = new NBTTagCompound();
            section.setInteger("version", 1);
            section.setInteger("mask", old | bit);
            persisted.setTag(journey.key, section);
        } else persisted.getCompoundTag(journey.key).setInteger("mask", old | bit);
        return true;
    }
    public static boolean cloneProgress(NBTTagCompound source, NBTTagCompound destination, Journey journey) {
        int from = mask(source, journey), to = mask(destination, journey);
        String persistedKey = EntityPlayer.PERSISTED_NBT_TAG;
        boolean destinationHasSection = destination.hasKey(persistedKey, 10)
                && destination.getCompoundTag(persistedKey).hasKey(journey.key);
        if (to < 0 || (from < 0 && destinationHasSection)) {
            CharacterEvents.carryCloneBackups(source, destination);
            String p = EntityPlayer.PERSISTED_NBT_TAG;
            NBTBase original = source.hasKey(p, 10) ? source.getCompoundTag(p).getTag(journey.key) : source.getTag(p);
            NBTBase target = destination.hasKey(p, 10) ? destination.getCompoundTag(p).getTag(journey.key) : destination.getTag(p);
            if (original != null && !original.equals(target))
                CharacterEvents.backupClone(destination, journey.key, "unsupported_journey_clone", original, target);
            return false;
        }
        return CharacterEvents.cloneSection(source, destination, journey.key);
    }
}
