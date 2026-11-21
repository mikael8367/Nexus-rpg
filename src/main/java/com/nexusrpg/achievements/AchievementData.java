package com.nexusrpg.achievements;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/** Versioned, append-only progress. Unknown or corrupt data is never repaired. */
public final class AchievementData {
    public static final String KEY = "NexusAchievements_v1";
    public static final int SKELETON_GOAL = 8;
    private AchievementData() {}

    // -1 = unreadable/unsupported, 0 = absent, 1 = valid.
    public static int status(NBTTagCompound root) {
        String persistedKey = EntityPlayer.PERSISTED_NBT_TAG;
        if (root.hasKey(persistedKey) && !root.hasKey(persistedKey, 10)) return -1;
        NBTTagCompound persisted = root.getCompoundTag(persistedKey);
        if (!persisted.hasKey(KEY)) return 0;
        if (!persisted.hasKey(KEY, 10)) return -1;
        NBTTagCompound section = persisted.getCompoundTag(KEY);
        if (!section.hasKey("version", 3) || section.getInteger("version") != 1
                || !section.hasKey("dimensions", 3) || !section.hasKey("skeletons", 9)) return -1;
        int dimensions = section.getInteger("dimensions");
        if (dimensions < 0 || dimensions > 7) return -1;
        NBTTagList ids = (NBTTagList)section.getTag("skeletons");
        int type = ids.getTagType();
        if (ids.tagCount() > SKELETON_GOAL || (type != 0 && type != 8)
                || (ids.tagCount() > 0 && type != 8)) return -1;
        Set<UUID> seen = new HashSet<UUID>();
        for (int i = 0; i < ids.tagCount(); i++) {
            try {
                String raw = ids.getStringTagAt(i);
                UUID parsed = UUID.fromString(raw);
                if (!parsed.toString().equals(raw) || !seen.add(parsed)) return -1;
            } catch (IllegalArgumentException ex) { return -1; }
        }
        return 1;
    }
    /** Keep unsupported/malformed destination data active; retain source in a recovery snapshot. */
    public static boolean cloneProgress(NBTTagCompound source, NBTTagCompound destination) {
        int from = status(source), to = status(destination);
        boolean unknownDestinationFields = false;
        if (to == 1) {
            for (String name : section(destination).getKeySet())
                if (!name.equals("version") && !name.equals("dimensions") && !name.equals("skeletons")) unknownDestinationFields = true;
        }
        if (to < 0 || (from < 0 && to == 1) || unknownDestinationFields) {
            com.nexusrpg.character.CharacterEvents.carryCloneBackups(source, destination);
            String persistedKey = EntityPlayer.PERSISTED_NBT_TAG;
            net.minecraft.nbt.NBTBase original = source.hasKey(persistedKey, 10)
                    ? source.getCompoundTag(persistedKey).getTag(KEY) : source.getTag(persistedKey);
            net.minecraft.nbt.NBTBase target = destination.hasKey(persistedKey, 10)
                    ? destination.getCompoundTag(persistedKey).getTag(KEY) : destination.getTag(persistedKey);
            if (original != null && !original.equals(target))
                com.nexusrpg.character.CharacterEvents.backupClone(destination, KEY, "unsupported_achievement_clone", original, target);
            return false;
        }
        return com.nexusrpg.character.CharacterEvents.cloneSection(source, destination, KEY);
    }
    private static NBTTagCompound section(NBTTagCompound root) {
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(KEY);
    }
    private static NBTTagCompound createOrGet(NBTTagCompound root) {
        int state = status(root);
        if (state < 0) return null;
        if (state == 0) {
            String key = EntityPlayer.PERSISTED_NBT_TAG;
            if (!root.hasKey(key)) root.setTag(key, new NBTTagCompound());
            NBTTagCompound section = new NBTTagCompound();
            section.setInteger("version", 1);
            section.setInteger("dimensions", 0);
            section.setTag("skeletons", new NBTTagList());
            root.getCompoundTag(key).setTag(KEY, section);
        }
        return section(root);
    }
    public static int skeletons(NBTTagCompound root) {
        return status(root) == 1 ? ((NBTTagList)section(root).getTag("skeletons")).tagCount() : 0;
    }
    public static int dimensions(NBTTagCompound root) {
        return status(root) == 1 ? section(root).getInteger("dimensions") : 0;
    }
    public static boolean recordSkeleton(NBTTagCompound root, UUID id) {
        if (id == null || status(root) < 0 || skeletons(root) >= SKELETON_GOAL) return false;
        NBTTagCompound data = createOrGet(root);
        NBTTagList ids = (NBTTagList)data.getTag("skeletons");
        for (int i = 0; i < ids.tagCount(); i++) if (id.toString().equals(ids.getStringTagAt(i))) return false;
        ids.appendTag(new NBTTagString(id.toString()));
        return true;
    }
    public static boolean visit(NBTTagCompound root, int dimension) {
        int bit = dimension == 0 ? 1 : dimension == -1 ? 2 : dimension == 1 ? 4 : 0;
        if (bit == 0 || status(root) < 0 || (dimensions(root) & bit) != 0) return false;
        NBTTagCompound data = createOrGet(root);
        data.setInteger("dimensions", data.getInteger("dimensions") | bit);
        return true;
    }
}
