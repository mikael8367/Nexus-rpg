package com.nexusrpg.init;

import com.nexusrpg.armor.GearArmor;
import com.nexusrpg.compass.ItemBiomeCompass;
import com.nexusrpg.weapons.GearAxe;
import com.nexusrpg.weapons.GearHoe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Registro das oito chaves originais e dos equipamentos RPG. */
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

    public static final Item MITHRIL_INGOT = item("mithril_ingot", CreativeTabs.MATERIALS);
    public static final Item ADAMANTITE_INGOT = item("adamantite_ingot", CreativeTabs.MATERIALS);
    public static final Item DRACONITE_INGOT = item("draconite_ingot", CreativeTabs.MATERIALS);

    public static final Item BIOME_COMPASS = named(new ItemBiomeCompass(), "biome_compass", CreativeTabs.TOOLS);

    private static final List<Item> ALL_ITEMS = buildItems();

    private ModItems() {}

    private static Item key(String id) {
        return new Item().setRegistryName("nexusrpg", id)
            .setUnlocalizedName("nexusrpg." + id).setCreativeTab(CreativeTabs.MATERIALS);
    }

    private static Item named(Item item, String id, CreativeTabs tab) {
        return item.setRegistryName("nexusrpg", id)
            .setUnlocalizedName("nexusrpg." + id).setCreativeTab(tab);
    }

    private static Item item(String id, CreativeTabs tab) {
        return named(new Item(), id, tab);
    }

    private static List<Item> buildItems() {
        List<Item> items = new ArrayList<>();
        Collections.addAll(items, KEY_GREEN, KEY_CYAN, KEY_YELLOW, KEY_PURPLE,
            KEY_RED, KEY_PINK, KEY_BLACK, KEY_ORANGE,
            MITHRIL_INGOT, ADAMANTITE_INGOT, DRACONITE_INGOT, BIOME_COMPASS);
        addTier(items, "mithril", MITHRIL_INGOT, 3, 1900, 9.0F, 3.5F, 18,
            36, new int[] {3, 6, 8, 3}, 2.0F);
        addTier(items, "adamantite", ADAMANTITE_INGOT, 4, 2500, 11.0F, 4.5F, 20,
            43, new int[] {4, 7, 9, 4}, 3.0F);
        addTier(items, "draconite", DRACONITE_INGOT, 5, 3200, 13.0F, 5.5F, 24,
            50, new int[] {4, 8, 10, 4}, 4.0F);
        return Collections.unmodifiableList(items);
    }

    private static void addTier(List<Item> items, String tier, Item ingot, int harvest,
            int uses, float efficiency, float damage, int enchantability,
            int armorDurability, int[] protection, float toughness) {
        Item.ToolMaterial tools = EnumHelper.addToolMaterial("NEXUSRPG_" + tier.toUpperCase(java.util.Locale.ROOT),
            harvest, uses, efficiency, damage, enchantability).setRepairItem(new ItemStack(ingot));
        ItemArmor.ArmorMaterial armor = EnumHelper.addArmorMaterial("NEXUSRPG_" + tier.toUpperCase(java.util.Locale.ROOT),
            "nexusrpg:" + tier, armorDurability, protection, enchantability,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON, toughness).setRepairItem(new ItemStack(ingot));
        items.add(named(new ItemSword(tools), tier + "_sword", CreativeTabs.COMBAT));
        items.add(named(new ItemPickaxe(tools) {}, tier + "_pickaxe", CreativeTabs.TOOLS));
        items.add(named(new GearAxe(tools), tier + "_axe", CreativeTabs.TOOLS));
        items.add(named(new ItemSpade(tools), tier + "_shovel", CreativeTabs.TOOLS));
        items.add(named(new GearHoe(tools, ingot), tier + "_hoe", CreativeTabs.TOOLS));
        items.add(named(new GearArmor(armor, EntityEquipmentSlot.HEAD, tier), tier + "_helmet", CreativeTabs.COMBAT));
        items.add(named(new GearArmor(armor, EntityEquipmentSlot.CHEST, tier), tier + "_chestplate", CreativeTabs.COMBAT));
        items.add(named(new GearArmor(armor, EntityEquipmentSlot.LEGS, tier), tier + "_leggings", CreativeTabs.COMBAT));
        items.add(named(new GearArmor(armor, EntityEquipmentSlot.FEET, tier), tier + "_boots", CreativeTabs.COMBAT));
    }

    public static List<Item> allItems() {
        return ALL_ITEMS;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : ALL_ITEMS) event.getRegistry().register(item);
    }
}
