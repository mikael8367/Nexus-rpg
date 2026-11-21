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
        // Do not replace a destination profile with an absent or malformed source.
        net.minecraft.nbt.NBTTagCompound data=e.getOriginal().getEntityData();
        net.minecraft.nbt.NBTTagCompound destination=e.getEntityPlayer().getEntityData();
        String persistedKey=EntityPlayer.PERSISTED_NBT_TAG;
        if(data.hasKey(persistedKey,10)) {
            net.minecraft.nbt.NBTTagCompound persisted=data.getCompoundTag(persistedKey);
            if(persisted.hasKey(CharacterProfile.KEY,10)) {
                CharacterProfile.replace(e.getEntityPlayer(),persisted.getCompoundTag(CharacterProfile.KEY));
            } else if(persisted.hasKey(CharacterProfile.KEY)) {
                // Preserve malformed raw profile data where the destination has none.
                if(!destination.hasKey(persistedKey)) destination.setTag(persistedKey,new net.minecraft.nbt.NBTTagCompound());
                if(destination.hasKey(persistedKey,10)) {
                    net.minecraft.nbt.NBTTagCompound destPersisted=destination.getCompoundTag(persistedKey);
                    if(!destPersisted.hasKey(CharacterProfile.KEY)) destPersisted.setTag(CharacterProfile.KEY,persisted.getTag(CharacterProfile.KEY).copy());
                }
            }
        } else if(data.hasKey(persistedKey) && !destination.hasKey(persistedKey)) {
            destination.setTag(persistedKey,data.getTag(persistedKey).copy());
        }
        CharacterProfile.apply(e.getEntityPlayer());
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
