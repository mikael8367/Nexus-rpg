package com.nexusrpg.achievements;

import com.nexusrpg.quests.HuntData;
import com.nexusrpg.quests.ZombieHunt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Conservative, transient evidence of a vanilla zombie death. */
@Mod.EventBusSubscriber(modid="nexusrpg")
public final class ZombieDeathEvidence {
    private static final PendingDeaths<EntityLivingBase, Candidate> PENDING = new PendingDeaths<EntityLivingBase, Candidate>();
    private static long ticks;
    private ZombieDeathEvidence() {}
    private static final class Candidate {
        final LivingDeathEvent death;
        final EntityPlayerMP player;
        Candidate(LivingDeathEvent death, EntityPlayerMP player) { this.death=death; this.player=player; }
    }
    // Storing a candidate has no persistent side effects, even if cancelled later.
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void death(LivingDeathEvent e) {
        EntityLivingBase victim=e.getEntityLiving();
        if (victim.world.isRemote || victim.getClass()!=EntityZombie.class) return;
        Entity source=e.getSource().getTrueSource();
        if (!(source instanceof EntityPlayerMP)) return;
        EntityPlayerMP p=(EntityPlayerMP)source;
        if (p.world!=victim.world || HuntData.status(p.getEntityData())!=1) return;
        PENDING.offer(victim,new Candidate(e,p),ticks);
    }
    // Vanilla Forge 1.12.2 posts drops after the death event, including empty or
    // cancelled drops and doMobLoot=false. This does not modify the drops.
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void drops(LivingDropsEvent e) {
        if (e.getEntityLiving().world.isRemote) return;
        Candidate c=PENDING.get(e.getEntityLiving());
        if (c!=null && !c.death.isCanceled() && c.death.getSource()==e.getSource()) PENDING.confirm(e.getEntityLiving());
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase!=TickEvent.Phase.END) return;
        ticks++;
        for (Candidate c:PENDING.drain(ticks)) {
            EntityLivingBase victim=c.death.getEntityLiving();
            EntityPlayerMP p=c.player;
            if (c.death.isCanceled() || victim.world.isRemote || !(victim.getHealth()<=0)
                || p.world!=victim.world || p.getServer()==null
                || p.getServer().getPlayerList().getPlayerByUUID(p.getUniqueID())!=p) continue;
            ZombieHunt.confirmedZombie(p,victim.getUniqueID());
        }
    }
}
