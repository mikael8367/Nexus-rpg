package com.nexusrpg.quests;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import com.nexusrpg.character.CharacterEvents;

/** Two separately versioned, finite hunts. Unknown data is never mutated. */
public final class TrailData {
    public static final int GOAL = 4;
    public enum Trail {
        SKELETON("NexusSkeletonTrail_v1"), SPIDER("NexusSpiderTrail_v1");
        public final String key;
        Trail(String key) { this.key = key; }
    }
    private TrailData() {}
    private static NBTTagCompound data(NBTTagCompound root, Trail trail) {
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(trail.key);
    }
    /** -1 unsupported, 0 absent, 1 active, 2 completed. */
    public static int status(NBTTagCompound root, Trail trail) {
        if (root == null || trail == null) return -1;
        String p = EntityPlayer.PERSISTED_NBT_TAG;
        if (root.hasKey(p) && !root.hasKey(p, 10)) return -1;
        NBTTagCompound persisted = root.getCompoundTag(p);
        if (!persisted.hasKey(trail.key)) return 0;
        if (!persisted.hasKey(trail.key, 10)) return -1;
        NBTTagCompound section = persisted.getCompoundTag(trail.key);
        // Fail closed on extension fields, including during clone conflicts.
        for (String name : section.getKeySet()) if (!name.equals("version") && !name.equals("kills")) return -1;
        if (!section.hasKey("version", 3) || section.getInteger("version") != 1 || !section.hasKey("kills", 9)) return -1;
        NBTTagList list = (NBTTagList)section.getTag("kills");
        if (list.tagCount() > GOAL || (list.getTagType() != 0 && list.getTagType() != 8)
                || (list.tagCount() > 0 && list.getTagType() != 8)) return -1;
        Set<UUID> seen = new HashSet<UUID>();
        for (int i = 0; i < list.tagCount(); i++) {
            String raw = list.getStringTagAt(i);
            try {
                UUID id = UUID.fromString(raw);
                if (!id.toString().equals(raw) || !seen.add(id)) return -1;
            } catch (IllegalArgumentException ex) { return -1; }
        }
        return list.tagCount() == GOAL ? 2 : 1;
    }
    public static int count(NBTTagCompound root, Trail trail) {
        return status(root, trail) > 0 ? ((NBTTagList)data(root, trail).getTag("kills")).tagCount() : 0;
    }
    public static boolean start(NBTTagCompound root, Trail trail) {
        if (status(root, trail) != 0) return false;
        String p = EntityPlayer.PERSISTED_NBT_TAG;
        if (!root.hasKey(p)) root.setTag(p, new NBTTagCompound());
        NBTTagCompound section = new NBTTagCompound();
        section.setInteger("version", 1); section.setTag("kills", new NBTTagList());
        root.getCompoundTag(p).setTag(trail.key, section);
        return true;
    }
    public static boolean record(NBTTagCompound root, Trail trail, UUID id) {
        if (id == null || status(root, trail) != 1) return false;
        NBTTagList list = (NBTTagList)data(root, trail).getTag("kills");
        String raw = id.toString();
        for (int i = 0; i < list.tagCount(); i++) if (raw.equals(list.getStringTagAt(i))) return false;
        list.appendTag(new NBTTagString(raw));
        return true;
    }
    public static boolean cloneProgress(NBTTagCompound source, NBTTagCompound destination, Trail trail) {
        int from = status(source, trail), to = status(destination, trail);
        if (to < 0 || (from < 0 && to > 0)) {
            CharacterEvents.carryCloneBackups(source, destination);
            String p = EntityPlayer.PERSISTED_NBT_TAG;
            NBTBase original = source.hasKey(p, 10) ? source.getCompoundTag(p).getTag(trail.key) : source.getTag(p);
            NBTBase target = destination.hasKey(p, 10) ? destination.getCompoundTag(p).getTag(trail.key) : destination.getTag(p);
            if (original != null && !original.equals(target)) CharacterEvents.backupClone(destination, trail.key, "unsupported_trail_clone", original, target);
            return false;
        }
        return CharacterEvents.cloneSection(source, destination, trail.key);
    }
}
