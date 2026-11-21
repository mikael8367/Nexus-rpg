package com.nexusrpg.rest;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Register ONE instance on MinecraftForge.EVENT_BUS during common preInit. */
public final class RestEvents {
    // Vanilla regeneration I restores one health point every 50 ticks.
    public static final int REST_TICKS = 200;

    @SubscribeEvent
    public void onWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        // Forge posts this event before vanilla clears the sleeping state and checks the bed.
        if (player.world.isRemote || !player.isEntityAlive() || !event.shouldSetSpawn()
                || event.wakeImmediately() || !player.isPlayerFullyAsleep()) {
            return;
        }
        BlockPos bed = player.bedLocation;
        if (bed == null || !player.world.isBlockLoaded(bed)) {
            return;
        }
        IBlockState state = player.world.getBlockState(bed);
        if (!state.getBlock().isBed(state, player.world, bed, player)) {
            return;
        }
        // Do not replace or extend regeneration from potions, beacons or other mods.
        if (player.isPotionActive(MobEffects.REGENERATION)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, REST_TICKS, 0, false, true));
        if (player.isPotionActive(MobEffects.REGENERATION)) {
            player.sendStatusMessage(new TextComponentString("Bom descanso: regeneração por 10 segundos."), false);
        }
    }
}
