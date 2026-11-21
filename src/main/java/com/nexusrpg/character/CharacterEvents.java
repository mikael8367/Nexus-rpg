package com.nexusrpg.character;

import com.nexusrpg.NexusRpg;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid=NexusRpg.MODID)
public final class CharacterEvents {
    private CharacterEvents() {}
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone e) {
        if(e.getEntityPlayer().world.isRemote) return;
        cloneSection(e.getOriginal().getEntityData(), e.getEntityPlayer().getEntityData(), CharacterProfile.KEY);
        CharacterProfile.apply(e.getEntityPlayer());
    }

    // Recovery documentation: a player save's ForgeData contains append-only
    // NexusRpgCloneBackup_v1_<number> compounds: version, section, reason, original,
    // destination. For malformed_persisted the snapshots are whole PlayerPersisted
    // tags; for malformed_destination_persisted original is the section and
    // destination is the whole malformed PlayerPersisted. Other reasons contain
    // whole section snapshots. Legacy death_combo_reset has only original. Types, unknown
    // nested data and pre-existing backup keys are retained; do not merge trees or
    // points automatically. To recover, make a separate full world backup, inspect
    // the player's snapshots, then choose the appropriate complete section by hand.
    // There is no in-game recovery UI. Both handlers carry these entries forward,
    // including when PlayerPersisted is malformed; ForgeData is saved by Forge.
    private static final String BACKUP_PREFIX="NexusRpgCloneBackup_v1_";
    private static String freeBackupKey(net.minecraft.nbt.NBTTagCompound root) {
        for (long i=0; i<Long.MAX_VALUE; i++) {
            String key=BACKUP_PREFIX+i;
            if (!root.hasKey(key)) return key;
        }
        throw new IllegalStateException("No free Nexus clone backup key");
    }
    public static void carryCloneBackups(net.minecraft.nbt.NBTTagCompound oldRoot, net.minecraft.nbt.NBTTagCompound root) {
        for (String key : oldRoot.getKeySet()) {
            if (!key.startsWith(BACKUP_PREFIX)) continue;
            net.minecraft.nbt.NBTBase value=oldRoot.getTag(key);
            if (!root.hasKey(key)) root.setTag(key,value.copy());
            else if (!root.getTag(key).equals(value)) {
                boolean found=false;
                for (String existing : root.getKeySet())
                    if (existing.startsWith(BACKUP_PREFIX) && root.getTag(existing).equals(value)) { found=true; break; }
                if (!found) root.setTag(freeBackupKey(root),value.copy());
            }
        }
    }
    public static void backupClone(net.minecraft.nbt.NBTTagCompound root, String section, String reason,
            net.minecraft.nbt.NBTBase original, net.minecraft.nbt.NBTBase destination) {
        net.minecraft.nbt.NBTTagCompound record=new net.minecraft.nbt.NBTTagCompound();
        record.setInteger("version",1); record.setString("section",section); record.setString("reason",reason);
        if (original!=null) record.setTag("original",original.copy());
        if (destination!=null) record.setTag("destination",destination.copy());
        // Replayed clone events must not keep appending the same recovery record.
        // Leave all pre-existing keys, even malformed or duplicate ones, untouched.
        for (String key : root.getKeySet())
            if (key.startsWith(BACKUP_PREFIX) && record.equals(root.getTag(key))) return;
        root.setTag(freeBackupKey(root),record);
    }
    /** Copy one complete section; true only if a compound from the original became active.
     * Conflicting snapshots are never merged: the original is authoritative on clone,
     * except when either side is malformed, when the destination stays untouched. */
    public static boolean cloneSection(net.minecraft.nbt.NBTTagCompound oldRoot, net.minecraft.nbt.NBTTagCompound root, String section) {
        carryCloneBackups(oldRoot,root);
        String key=EntityPlayer.PERSISTED_NBT_TAG;
        if (!oldRoot.hasKey(key)) return false;
        if (!oldRoot.hasKey(key,10)) {
            if (!root.hasKey(key)) root.setTag(key,oldRoot.getTag(key).copy());
            else if (!oldRoot.getTag(key).equals(root.getTag(key)))
                backupClone(root,section,"malformed_persisted",oldRoot.getTag(key),root.getTag(key));
            return false;
        }
        net.minecraft.nbt.NBTTagCompound oldPersisted=oldRoot.getCompoundTag(key);
        if (!oldPersisted.hasKey(section)) return false;
        if (root.hasKey(key) && !root.hasKey(key,10)) {
            backupClone(root,section,"malformed_destination_persisted",oldPersisted.getTag(section),root.getTag(key));
            return false;
        }
        if (!root.hasKey(key)) root.setTag(key,new net.minecraft.nbt.NBTTagCompound());
        net.minecraft.nbt.NBTTagCompound persisted=root.getCompoundTag(key);
        net.minecraft.nbt.NBTBase original=oldPersisted.getTag(section), destination=persisted.getTag(section);
        if (destination==null) {
            persisted.setTag(section,original.copy());
            return original.getId()==10;
        }
        if (original.equals(destination)) return original.getId()==10;
        if (original.getId()!=10 || destination.getId()!=10) {
            backupClone(root,section,"malformed_section",original,destination);
            return false;
        }
        net.minecraft.nbt.NBTTagCompound source=(net.minecraft.nbt.NBTTagCompound)original;
        net.minecraft.nbt.NBTTagCompound target=(net.minecraft.nbt.NBTTagCompound)destination;
        if (source.getKeySet().isEmpty() && !target.getKeySet().isEmpty()) return false;
        if (!target.getKeySet().isEmpty()) backupClone(root,section,"conflicting_snapshots",source,target);
        persisted.setTag(section,source.copy());
        return true;
    }
    static boolean hasProtectedProfile(net.minecraft.nbt.NBTTagCompound destination) {
        String key=EntityPlayer.PERSISTED_NBT_TAG;
        if(!destination.hasKey(key)) return false;
        if(!destination.hasKey(key,10)) return true;
        net.minecraft.nbt.NBTTagCompound persisted=destination.getCompoundTag(key);
        if(!persisted.hasKey(CharacterProfile.KEY)) return false;
        return !persisted.hasKey(CharacterProfile.KEY,10)
            || !persisted.getCompoundTag(CharacterProfile.KEY).getKeySet().isEmpty();
    }
    @SubscribeEvent public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) { ready(e.player); }
    @SubscribeEvent public static void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent e) { ready(e.player); }
    @SubscribeEvent public static void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent e) { ready(e.player); }
    private static void ready(EntityPlayer p) { if(!p.world.isRemote) {CharacterProfile.apply(p);CharacterProfile.sync(p);} }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase==TickEvent.Phase.END && !e.player.world.isRemote && e.player.ticksExisted%20==0) CharacterProfile.regenerate(e.player);
    }
    @SubscribeEvent(priority=EventPriority.LOW,receiveCanceled=false) public static void hurt(LivingHurtEvent e) {
        if(e.getEntityLiving().world.isRemote || e.getAmount()<=0 || Float.isNaN(e.getAmount()) || Float.isInfinite(e.getAmount())) return;
        Entity source=e.getSource().getTrueSource(); double damage=e.getAmount();
        if(source instanceof EntityPlayer && source!=e.getEntityLiving()) damage *= 1D+Math.min(2D,CharacterProfile.stat((EntityPlayer)source,"strength")*0.025D);
        if(e.getEntityLiving() instanceof EntityPlayer && !e.getSource().isDamageAbsolute() && !e.getSource().canHarmInCreative()) {
            int defense=CharacterProfile.stat((EntityPlayer)e.getEntityLiving(),"defense"); damage *= 1D-Math.min(0.6D,defense*0.01D);
        }
        e.setAmount((float)Math.min(Float.MAX_VALUE,damage));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=false) public static void death(LivingDeathEvent e) {
        EntityLivingBase victim=e.getEntityLiving();
        // Armor stands are living entities in vanilla, but are craftable objects, not mobs.
        if(victim.world.isRemote || victim instanceof EntityPlayer || victim instanceof net.minecraft.entity.item.EntityArmorStand) return;
        Entity killer=e.getSource().getTrueSource(); if(!(killer instanceof EntityPlayerMP)) return;
        int xp=(int)Math.max(5,Math.min(100,victim.getMaxHealth()*3)); CharacterProfile.addXp((EntityPlayer)killer,xp);
    }
}
