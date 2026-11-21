package com.nexusrpg.achievements;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Automatically registered on the Forge event bus; gameplay runs on the server only. */
@Mod.EventBusSubscriber(modid="nexusrpg")
public final class AchievementEvents {
    static final UUID HUNTER_BONUS = UUID.fromString("ea619e67-e722-445c-bf37-65946107691b");
    static final UUID EXPLORER_BONUS = UUID.fromString("b623f04d-5e0c-4c06-b486-e89fa5763904");
    private static final PendingDeaths<EntityLivingBase, LivingDeathEvent> PENDING = new PendingDeaths<EntityLivingBase, LivingDeathEvent>();
    private static long serverTicks;
    private AchievementEvents() {}
    private static void say(EntityPlayer p, String text) { p.sendMessage(new TextComponentString("[Conquistas] " + text)); }
    static void bonus(IAttributeInstance attribute, UUID id, String name, boolean earned) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (earned && old == null) attribute.applyModifier(new AttributeModifier(id, name, 1.0D, 0).setSaved(false));
        else if (!earned && old != null) attribute.removeModifier(old);
    }
    private static void apply(EntityPlayer p) {
        int state = AchievementData.status(p.getEntityData());
        bonus(p.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE), HUNTER_BONUS, "Nexus skeleton hunter", state == 1 && AchievementData.skeletons(p.getEntityData()) == AchievementData.SKELETON_GOAL);
        bonus(p.getEntityAttribute(SharedMonsterAttributes.ARMOR), EXPLORER_BONUS, "Nexus three realms", state == 1 && AchievementData.dimensions(p.getEntityData()) == 7);
    }
    private static boolean eligible(EntityPlayer p) {
        return p instanceof EntityPlayerMP && !p.world.isRemote && !p.capabilities.isCreativeMode && !p.isSpectator();
    }
    private static void visit(EntityPlayer p) {
        if (!eligible(p)) return;
        if (AchievementData.visit(p.getEntityData(), p.dimension)) {
            int mask = AchievementData.dimensions(p.getEntityData());
            int count = Integer.bitCount(mask);
            if (mask == 7) {
                apply(p);
                say(p, "Tres Reinos concluida! Recompensa permanente: +1 armadura.");
            } else say(p, "Tres Reinos: " + count + "/3 dimensoes (Mundo, Nether e End). Recompensa: +1 armadura.");
        }
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone e) {
        if (!e.getEntityPlayer().world.isRemote)
            AchievementData.cloneProgress(e.getOriginal().getEntityData(), e.getEntityPlayer().getEntityData());
    }
    @SubscribeEvent public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
        EntityPlayer p = e.player;
        if (!(p instanceof EntityPlayerMP) || p.world.isRemote) return;
        apply(p);
        if (AchievementData.status(p.getEntityData()) < 0) {
            say(p, "Dados de conquistas invalidos ou de outra versao; nada foi alterado. Avise o administrador.");
            return;
        }
        say(p, "Cacador de Esqueletos: " + AchievementData.skeletons(p.getEntityData()) + "/8 (+1 dano). Tres Reinos: " + Integer.bitCount(AchievementData.dimensions(p.getEntityData())) + "/3 (+1 armadura). Progresso no sobrevivencia/aventura.");
        visit(p);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof EntityPlayerMP) || e.player.world.isRemote || e.player.ticksExisted % 20 != 0) return;
        visit(e.player);
        apply(e.player);
    }
    @SubscribeEvent public static void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent e) {
        visit(e.player);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=false)
    public static void death(LivingDeathEvent e) {
        if (e.getEntityLiving().world.isRemote || e.getEntityLiving().getClass() != EntitySkeleton.class) return;
        Entity source = e.getSource().getTrueSource();
        if (!(source instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = (EntityPlayerMP)source;
        if (!eligible(p) || p.world != e.getEntityLiving().world) return;
        PENDING.offer(e.getEntityLiving(), e, serverTicks);
    }
    // Vanilla 1.12.2 calls this only after the death event has returned uncancelled
    // and EntityLivingBase.dead is set. Canceled drops still mean a real death.
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void drops(LivingDropsEvent e) {
        if (e.getEntityLiving().world.isRemote) return;
        LivingDeathEvent candidate = PENDING.get(e.getEntityLiving());
        if (candidate != null && !candidate.isCanceled() && candidate.getSource() == e.getSource())
            PENDING.confirm(e.getEntityLiving());
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        serverTicks++;
        for (LivingDeathEvent death : PENDING.drain(serverTicks)) {
            EntityLivingBase victim = death.getEntityLiving();
            Entity source = death.getSource().getTrueSource();
            if (death.isCanceled() || victim.getHealth() > 0 || !(source instanceof EntityPlayerMP)) continue;
            EntityPlayerMP p = (EntityPlayerMP)source;
            if (!eligible(p) || p.getServer() == null || p.getServer().getPlayerList().getPlayerByUUID(p.getUniqueID()) != p
                    || !AchievementData.recordSkeleton(p.getEntityData(), victim.getUniqueID())) continue;
            announceSkeleton(p);
        }
    }
    private static void announceSkeleton(EntityPlayerMP p) {
        int count = AchievementData.skeletons(p.getEntityData());
        if (count == AchievementData.SKELETON_GOAL) {
            apply(p);
            say(p, "Cacador de Esqueletos concluida! Recompensa permanente: +1 dano de ataque.");
        } else say(p, "Cacador de Esqueletos: " + count + "/8. Recompensa: +1 dano de ataque.");
    }
}
