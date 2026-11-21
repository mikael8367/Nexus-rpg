package com.nexusrpg.quests;

import com.nexusrpg.NexusRpg;
import java.util.UUID;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Passive, server-authoritative journeys; no block or inventory modification. */
@Mod.EventBusSubscriber(modid=NexusRpg.MODID)
public final class JourneyEvents {
    static final UUID EXPLORER_SPEED = UUID.fromString("a9c9f076-35af-44f4-9767-4b17d8833bc5");
    static final UUID CRAFTER_ARMOR = UUID.fromString("185d2901-1650-45ca-8b37-95d0fd632573");
    private JourneyEvents() {}
    static boolean eligible(EntityPlayer p) {
        return p instanceof EntityPlayerMP && !(p instanceof net.minecraftforge.common.util.FakePlayer)
                && !p.world.isRemote && p.isEntityAlive() && !p.isCreative() && !p.isSpectator();
    }
    static void bonus(IAttributeInstance attribute, UUID id, String name, double amount, int operation, boolean earned) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (earned && old == null) attribute.applyModifier(new AttributeModifier(id, name, amount, operation).setSaved(false));
        else if (!earned && old != null) attribute.removeModifier(old);
    }
    static void apply(EntityPlayer player) {
        bonus(player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED), EXPLORER_SPEED, "Nexus explorer journey", 0.02D, 2,
                JourneyData.mask(player.getEntityData(), JourneyData.Journey.EXPLORER) == JourneyData.COMPLETE);
        bonus(player.getEntityAttribute(SharedMonsterAttributes.ARMOR), CRAFTER_ARMOR, "Nexus crafter journey", 1D, 0,
                JourneyData.mask(player.getEntityData(), JourneyData.Journey.CRAFTER) == JourneyData.COMPLETE);
    }
    private static void say(EntityPlayer p, String message) { p.sendMessage(new TextComponentString("[Jornadas] " + message)); }
    static int craftBit(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.getItem() == Items.IRON_PICKAXE) return 1;
        if (stack.getItem() == Items.SHIELD) return 2;
        if (stack.getItem() == Items.COMPASS) return 4;
        return 0;
    }
    static int locationBits(int dimension, int y, boolean water, boolean ocean) {
        if (dimension != 0) return 0;
        return (y <= 24 && y >= 0 ? 1 : 0) | (y >= 110 && y < 256 ? 2 : 0) | (water && ocean ? 4 : 0);
    }
    private static String description(JourneyData.Journey j) {
        return j == JourneyData.Journey.EXPLORER
                ? "Explorador: visite no Mundo normal Y<=24, Y>=110 e nade em oceano. Recompensa +2% velocidade."
                : "Artifice: fabrique picareta de ferro, escudo e bussola. Recompensa +1 armadura.";
    }
    static String remaining(JourneyData.Journey j, int mask) {
        StringBuilder result = new StringBuilder();
        String[] labels = j == JourneyData.Journey.EXPLORER
                ? new String[] {"Y<=24", "Y>=110", "nadar em oceano"}
                : new String[] {"picareta de ferro", "escudo", "bussola"};
        for (int i = 0; i < 3; i++) if ((mask & (1 << i)) == 0) {
            if (result.length() > 0) result.append(", ");
            result.append(labels[i]);
        }
        return result.toString();
    }
    private static void progress(EntityPlayer p, JourneyData.Journey journey, int bit) {
        if (!JourneyData.record(p.getEntityData(), journey, bit)) return;
        int mask = JourneyData.mask(p.getEntityData(), journey);
        say(p, (journey == JourneyData.Journey.EXPLORER ? "Explorador" : "Artifice") + ": " + Integer.bitCount(mask) + "/3. "
                + (mask == JourneyData.COMPLETE ? "Concluida! Recompensa permanente aplicada." : "Falta: " + remaining(journey, mask) + "."));
        if (mask == JourneyData.COMPLETE) apply(p);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) {
        if (e.getEntityPlayer().world.isRemote) return;
        for (JourneyData.Journey j : JourneyData.Journey.values())
            JourneyData.cloneProgress(e.getOriginal().getEntityData(), e.getEntityPlayer().getEntityData(), j);
    }
    @SubscribeEvent public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
        EntityPlayer p = e.player;
        if (!(p instanceof EntityPlayerMP) || p.world.isRemote) return;
        apply(p);
        for (JourneyData.Journey j : JourneyData.Journey.values()) {
            int m = JourneyData.mask(p.getEntityData(), j);
            if (m < 0) say(p, description(j) + " Dados invalidos ou de outra versao; avise o administrador.");
            else say(p, description(j) + " Progresso " + Integer.bitCount(m) + "/3" + (m == 7 ? " (concluida)." : ". Falta: " + remaining(j, m) + "."));
        }
        say(p, "As jornadas sao automaticas em sobrevivencia ou aventura; nao precisam de comando.");
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof EntityPlayerMP) || e.player.world.isRemote || e.player.ticksExisted % 20 != 0) return;
        EntityPlayer p = e.player;
        apply(p);
        if (!eligible(p) || p.dimension != 0) return;
        BlockPos pos = p.getPosition();
        if (!p.world.isBlockLoaded(pos)) return;
        boolean water = p.isInWater();
        boolean ocean = water && BiomeDictionary.hasType(p.world.getBiome(pos), BiomeDictionary.Type.OCEAN);
        int bits = locationBits(p.dimension, pos.getY(), water, ocean);
        for (int bit = 1; bit <= 4; bit <<= 1) if ((bits & bit) != 0) progress(p, JourneyData.Journey.EXPLORER, bit);
    }
    /** FML fires this non-cancelable event when crafting output is taken. */
    @SubscribeEvent public static void crafted(net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent e) {
        if (!eligible(e.player)) return;
        int bit = craftBit(e.crafting);
        if (bit != 0) progress(e.player, JourneyData.Journey.CRAFTER, bit);
    }
}
