package com.nexusrpg.supplies;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class SupplyRegistry {
    public static final Item BANDAGE = make("field_bandage", ItemFieldSupply.Kind.BANDAGE);
    public static final Item ANTIDOTE = make("field_antidote", ItemFieldSupply.Kind.ANTIDOTE);
    public static final Item ESCAPE = make("escape_powder", ItemFieldSupply.Kind.ESCAPE);
    private SupplyRegistry() {}
    private static Item make(String name, ItemFieldSupply.Kind kind) {
        return new ItemFieldSupply(kind).setRegistryName("nexusrpg", name)
            .setUnlocalizedName("nexusrpg." + name).setCreativeTab(CreativeTabs.MISC);
    }
    @SubscribeEvent public static void register(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(BANDAGE, ANTIDOTE, ESCAPE);
    }
}
