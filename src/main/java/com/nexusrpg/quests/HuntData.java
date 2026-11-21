package com.nexusrpg.quests;

import com.nexusrpg.character.CharacterEvents;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/** One permanent, non-repeatable hunt. Never repairs or overwrites malformed NBT. */
public final class HuntData {
    public static final String KEY="NexusZombieHunt_v1";
    public static final int GOAL=5;
    private HuntData() {}
    // -1 damaged/unsupported, 0 not started, 1 active, 2 completed.
    public static int status(NBTTagCompound root) {
        if (root.hasKey(EntityPlayer.PERSISTED_NBT_TAG) && !root.hasKey(EntityPlayer.PERSISTED_NBT_TAG,10)) return -1;
        NBTTagCompound persisted=root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(KEY)) return 0;
        if (!persisted.hasKey(KEY,10)) return -1;
        NBTTagCompound quest=persisted.getCompoundTag(KEY);
        // Preserve extensions from unsupported data instead of modifying them.
        for (String name : quest.getKeySet()) if (!name.equals("version") && !name.equals("kills")) return -1;
        if (!quest.hasKey("version",3) || quest.getInteger("version")!=1 || !quest.hasKey("kills",9)) return -1;
        NBTTagList kills=quest.getTagList("kills",8);
        // getTagList silently returns an empty list for a wrong element type.
        NBTTagList actual=(NBTTagList)quest.getTag("kills");
        if (actual.tagCount()>0 && actual.getTagType()!=8) return -1;
        if (kills.tagCount()>GOAL) return -1;
        Set<UUID> seen=new HashSet<UUID>();
        for(int i=0;i<kills.tagCount();i++) {
            try { if (!seen.add(UUID.fromString(kills.getStringTagAt(i)))) return -1; }
            catch (IllegalArgumentException ex) { return -1; }
        }
        return kills.tagCount()==GOAL ? 2 : 1;
    }
    /** Preserve unsupported clone destinations, with a backup of conflicting originals. */
    public static boolean cloneProgress(NBTTagCompound source, NBTTagCompound destination) {
        int from=status(source), to=status(destination);
        if (to<0 || (from<0 && to>0)) {
            CharacterEvents.carryCloneBackups(source,destination);
            String p=EntityPlayer.PERSISTED_NBT_TAG;
            net.minecraft.nbt.NBTBase original=source.hasKey(p,10)?source.getCompoundTag(p).getTag(KEY):source.getTag(p);
            net.minecraft.nbt.NBTBase target=destination.hasKey(p,10)?destination.getCompoundTag(p).getTag(KEY):destination.getTag(p);
            if (original!=null && !original.equals(target)) CharacterEvents.backupClone(destination,KEY,"unsupported_hunt_clone",original,target);
            return false;
        }
        return CharacterEvents.cloneSection(source,destination,KEY);
    }
    public static int count(NBTTagCompound root) {
        int s=status(root); if(s<=0) return 0;
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(KEY).getTagList("kills",8).tagCount();
    }
    public static boolean start(NBTTagCompound root) {
        if(status(root)!=0) return false;
        if(!root.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) root.setTag(EntityPlayer.PERSISTED_NBT_TAG,new NBTTagCompound());
        NBTTagCompound q=new NBTTagCompound(); q.setInteger("version",1); q.setTag("kills",new NBTTagList());
        root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).setTag(KEY,q); return true;
    }
    public static boolean record(NBTTagCompound root, UUID id) {
        if(id==null || status(root)!=1) return false;
        NBTTagList kills=root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(KEY).getTagList("kills",8);
        for(int i=0;i<kills.tagCount();i++) if(id.equals(UUID.fromString(kills.getStringTagAt(i)))) return false;
        // A newly-created empty NBTTagList has type 0 and getTagList returns it unchanged.
        kills.appendTag(new NBTTagString(id.toString()));
        return true;
    }
}
