package com.nexusrpg.init;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Oito chaves coloridas, uma para a masmorra de cada bioma. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class ModItems {
    public static final Item KEY_GREEN = key("key_green");
    public static final Item KEY_CYAN = key("key_cyan");
    public static final Item KEY_YELLOW = key("key_yellow");
    public static final Item KEY_PURPLE = key("key_purple");
    public static final Item KEY_RED = key("key_red");
    public static final Item KEY_PINK = key("key_pink");
    public static final Item KEY_BLACK = key("key_black");
    public static final Item KEY_ORANGE = key("key_orange");

    private ModItems() {}

    private static Item key(String id) {
        return new Item().setRegistryName("nexusrpg", id)
            .setUnlocalizedName("nexusrpg." + id).setCreativeTab(CreativeTabs.MATERIALS);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(KEY_GREEN, KEY_CYAN, KEY_YELLOW, KEY_PURPLE,
            KEY_RED, KEY_PINK, KEY_BLACK, KEY_ORANGE);
    }
}
