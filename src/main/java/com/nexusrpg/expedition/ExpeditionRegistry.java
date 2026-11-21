package com.nexusrpg.expedition;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class ExpeditionRegistry {
    public static final Item QUENCH = make("quenching_salve", ItemExpeditionAid.Kind.QUENCH);
    public static final Item DIVE = make("divers_reserve", ItemExpeditionAid.Kind.DIVE);
    public static final Item SIGHT = make("lantern_dust", ItemExpeditionAid.Kind.SIGHT);
    private ExpeditionRegistry() {}
    private static Item make(String name, ItemExpeditionAid.Kind kind) {
        return new ItemExpeditionAid(kind).setRegistryName("nexusrpg", name)
            .setUnlocalizedName("nexusrpg." + name).setCreativeTab(CreativeTabs.MISC);
    }
    @SubscribeEvent public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(QUENCH, DIVE, SIGHT);
    }
}
