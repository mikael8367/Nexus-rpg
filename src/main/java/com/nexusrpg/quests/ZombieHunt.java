package com.nexusrpg.quests;

import com.nexusrpg.NexusRpg;
import com.nexusrpg.character.CharacterEvents;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityZombie;
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

/** Server-only quest. The reward is a derived modifier, never an item grant. */
@Mod.EventBusSubscriber(modid=NexusRpg.MODID)
public final class ZombieHunt {
    private static final UUID BONUS=UUID.fromString("c0954865-1394-4f87-a073-5cda5a9ce774");
    private ZombieHunt() {}
    private static void say(EntityPlayer p,String s) { p.sendMessage(new TextComponentString("[Missao] "+s)); }
    private static void apply(EntityPlayer p) {
        IAttributeInstance health=p.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        updateBonus(health,HuntData.status(p.getEntityData())==2);
    }
    static void updateBonus(IAttributeInstance health, boolean earned) {
        if(health==null) return;
        AttributeModifier existing=health.getModifier(BONUS);
        if(earned && existing==null) health.applyModifier(new AttributeModifier(BONUS,"Nexus zombie hunt",2.0D,0).setSaved(false));
        else if(!earned && existing!=null) health.removeModifier(existing);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if(e.getEntityPlayer().world.isRemote) return;
        HuntData.cloneProgress(e.getOriginal().getEntityData(),e.getEntityPlayer().getEntityData());
    }
    @SubscribeEvent public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
        if(e.player.world.isRemote) return;
        apply(e.player); say(e.player,"Use /nexusmissao para conhecer a cacada e sua recompensa.");
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase==TickEvent.Phase.END && !e.player.world.isRemote && e.player.ticksExisted%20==0) apply(e.player);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=false) public static void death(LivingDeathEvent e) {
        if(e.getEntityLiving().world.isRemote || e.getEntityLiving().getClass()!=EntityZombie.class) return;
        Entity source=e.getSource().getTrueSource();
        if(!(source instanceof EntityPlayerMP)) return;
        EntityPlayerMP p=(EntityPlayerMP)source;
        if(p.world!=e.getEntityLiving().world || !HuntData.record(p.getEntityData(),e.getEntityLiving().getUniqueID())) return;
        int n=HuntData.count(p.getEntityData());
        if(n==HuntData.GOAL) { apply(p); say(p,"Cacada concluida! Recompensa permanente: +1 coracao de vida maxima."); }
        else say(p,"Zumbis derrotados: "+n+"/"+HuntData.GOAL+".");
    }
    public static final class HuntCommand extends CommandBase {
        @Override public String getName() { return "nexusmissao"; }
        @Override public String getUsage(ICommandSender sender) { return "/nexusmissao [iniciar|status]"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public void execute(MinecraftServer server,ICommandSender sender,String[] args) throws CommandException {
            EntityPlayerMP p=getCommandSenderAsPlayer(sender);
            if(args.length>1 || (args.length==1 && !args[0].equals("iniciar") && !args[0].equals("status"))) {
                say(p,"Use /nexusmissao iniciar ou /nexusmissao status."); return;
            }
            int status=HuntData.status(p.getEntityData());
            if(status<0) { say(p,"Dados da missao invalidos ou de outra versao; nada foi alterado. Fale com o administrador."); return; }
            if(args.length==1 && args[0].equals("iniciar") && status==0) {
                HuntData.start(p.getEntityData()); status=1; say(p,"Cacada iniciada!");
            }
            if(status==0) say(p,"Cacada unica: derrote 5 zumbis comuns. Recompensa: +1 coracao permanente. Use /nexusmissao iniciar.");
            else if(status==1) say(p,"Cacada ativa: "+HuntData.count(p.getEntityData())+"/5 zumbis comuns. Recompensa: +1 coracao permanente.");
            else say(p,"Cacada concluida: +1 coracao permanente desbloqueado.");
        }
    }
}
