package com.nexusrpg.quests;

import com.nexusrpg.NexusRpg;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid=NexusRpg.MODID)
public final class TrailHunts {
    private static final UUID SPEED = UUID.fromString("b7add920-2b4f-4c63-b466-7f616a4c0f91");
    private static final UUID ARMOR = UUID.fromString("6167424f-d4b6-4ee1-9eab-c554b68c6c39");
    private TrailHunts() {}
    static void updateBonus(IAttributeInstance attribute, UUID id, String name, double amount, int operation, boolean earned) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(id);
        if (earned && current == null) attribute.applyModifier(new AttributeModifier(id, name, amount, operation).setSaved(false));
        else if (!earned && current != null) attribute.removeModifier(current);
    }
    private static void apply(EntityPlayer player) {
        updateBonus(player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED), SPEED, "Nexus skeleton trail", 0.02D, 2,
                TrailData.status(player.getEntityData(), TrailData.Trail.SKELETON) == 2);
        updateBonus(player.getEntityAttribute(SharedMonsterAttributes.ARMOR), ARMOR, "Nexus spider trail", 1D, 0,
                TrailData.status(player.getEntityData(), TrailData.Trail.SPIDER) == 2);
    }
    private static void say(EntityPlayer player, String message) { player.sendMessage(new TextComponentString("[Cacadas] " + message)); }
    private static String label(TrailData.Trail trail) { return trail == TrailData.Trail.SKELETON ? "esqueleto" : "aranha"; }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (event.getEntityPlayer().world.isRemote) return;
        for (TrailData.Trail trail : TrailData.Trail.values())
            TrailData.cloneProgress(event.getOriginal().getEntityData(), event.getEntityPlayer().getEntityData(), trail);
    }
    @SubscribeEvent public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player.world.isRemote) return;
        apply(event.player);
        say(event.player, "Use /nexuscacadas para conhecer duas missoes permanentes.");
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote && event.player.ticksExisted % 20 == 0) apply(event.player);
    }
    /** Called only after the server-side death has been confirmed. */
    public static void confirmedTrail(EntityPlayerMP player, TrailData.Trail trail, UUID victim) {
        if (player.isCreative() || player.isSpectator() || !player.isEntityAlive()) return;
        if (!TrailData.record(player.getEntityData(), trail, victim)) return;
        int n = TrailData.count(player.getEntityData(), trail);
        if (n == TrailData.GOAL) {
            apply(player);
            say(player, "Cacada de " + label(trail) + " concluida! Recompensa permanente: "
                    + (trail == TrailData.Trail.SKELETON ? "+2% velocidade de movimento." : "+1 armadura."));
        } else say(player, "Cacada de " + label(trail) + ": " + n + "/" + TrailData.GOAL + ".");
    }
    public static final class TrailCommand extends CommandBase {
        @Override public String getName() { return "nexuscacadas"; }
        @Override public String getUsage(ICommandSender sender) { return "/nexuscacadas [status|iniciar esqueleto|iniciar aranha]"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            if (args.length == 2 && args[0].equals("iniciar") && (args[1].equals("esqueleto") || args[1].equals("aranha"))) {
                TrailData.Trail trail = args[1].equals("esqueleto") ? TrailData.Trail.SKELETON : TrailData.Trail.SPIDER;
                if (TrailData.start(player.getEntityData(), trail)) say(player, "Cacada de " + label(trail) + " iniciada!");
            } else if (!(args.length == 0 || (args.length == 1 && args[0].equals("status")))) {
                say(player, "Use /nexuscacadas status ou /nexuscacadas iniciar esqueleto|aranha."); return;
            }
            for (TrailData.Trail trail : TrailData.Trail.values()) {
                int s = TrailData.status(player.getEntityData(), trail);
                String reward = trail == TrailData.Trail.SKELETON ? "+2% velocidade" : "+1 armadura";
                String status = s < 0 ? "dados invalidos ou de outra versao; procure o administrador"
                        : s == 0 ? "nao iniciada; use /nexuscacadas iniciar " + label(trail)
                        : s == 1 ? "ativa " + TrailData.count(player.getEntityData(), trail) + "/" + TrailData.GOAL
                        : "concluida";
                say(player, label(trail) + " comum: " + status + "; recompensa permanente " + reward + ".");
            }
        }
    }
}
