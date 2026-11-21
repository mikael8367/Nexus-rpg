package com.nexusrpg.achievements;

import com.nexusrpg.quests.TrailData;
import com.nexusrpg.quests.TrailHunts;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Server-side evidence for new trails; leaves existing XP and zombie hunt untouched. */
@Mod.EventBusSubscriber(modid="nexusrpg")
public final class CombatDeathEvents {
    private static final PendingDeaths<EntityLivingBase, Candidate> PENDING = new PendingDeaths<EntityLivingBase, Candidate>();
    // Remember credited entity instances without retaining dead entities indefinitely.
    private static final java.util.Map<EntityLivingBase, Boolean> CREDITED = new java.util.WeakHashMap<EntityLivingBase, Boolean>();
    private static long serverTicks;
    private CombatDeathEvents() {}

    private static final class Candidate {
        final LivingDeathEvent death;
        final EntityPlayerMP player;
        final TrailData.Trail trail;
        Candidate(LivingDeathEvent death, EntityPlayerMP player, TrailData.Trail trail) {
            this.death = death; this.player = player; this.trail = trail;
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void death(LivingDeathEvent e) {
        EntityLivingBase victim = e.getEntityLiving();
        if (victim.world.isRemote || victim instanceof EntityPlayer || victim instanceof EntityArmorStand || CREDITED.containsKey(victim)) return;
        Entity source = e.getSource().getTrueSource();
        if (!(source instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = (EntityPlayerMP)source;
        if (p.world != victim.world) return;
        TrailData.Trail trail = victim.getClass() == EntitySkeleton.class ? TrailData.Trail.SKELETON
                : victim.getClass() == EntitySpider.class ? TrailData.Trail.SPIDER : null;
        if (trail != null && (p.isCreative() || p.isSpectator() || !p.isEntityAlive() || TrailData.status(p.getEntityData(), trail) != 1)) trail = null;
        if (trail == null) return;
        PENDING.offer(victim, new Candidate(e, p, trail), serverTicks);
    }

    // Base EntityLivingBase.onDeath posts drops after the death event completes,
    // even if doMobLoot is false or drops are empty/cancelled. Do not alter drops.
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void drops(LivingDropsEvent e) {
        if (e.getEntityLiving().world.isRemote) return;
        Candidate c = PENDING.get(e.getEntityLiving());
        if (c != null && !c.death.isCanceled() && c.death.getSource() == e.getSource())
            PENDING.confirm(e.getEntityLiving());
    }

    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        serverTicks++;
        for (Candidate c : PENDING.drain(serverTicks)) {
            EntityLivingBase victim = c.death.getEntityLiving();
            EntityPlayerMP p = c.player;
            if (c.death.isCanceled() || !(victim.getHealth() <= 0) || victim.world.isRemote
                    || p.world != victim.world || p.getServer() == null
                    || p.getServer().getPlayerList().getPlayerByUUID(p.getUniqueID()) != p || CREDITED.containsKey(victim)) continue;
            CREDITED.put(victim, Boolean.TRUE);
            if (c.trail != null) TrailHunts.confirmedTrail(p, c.trail, victim.getUniqueID());
        }
    }
}
