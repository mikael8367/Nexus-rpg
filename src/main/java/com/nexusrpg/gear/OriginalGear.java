package com.nexusrpg.gear;

import com.nexusrpg.NexusRpg;
import com.nexusrpg.armor.GearArmor;
import com.nexusrpg.weapons.GearAxe;
import com.nexusrpg.weapons.GearHoe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Materiais artesanais independentes; não registra geradores de mundo. */
@Mod.EventBusSubscriber(modid = NexusRpg.MODID)
public final class OriginalGear {
    public static final Block AURONITE_ORE = ore("auronite_ore", 3.0F, 2);
    public static final Block UMBRILITE_ORE = ore("umbrilite_ore", 4.5F, 3);
    public static final Item AURONITE_INGOT = named(new Item(), "auronite_ingot", CreativeTabs.MATERIALS);
    public static final Item UMBRILITE_INGOT = named(new Item(), "umbrilite_ingot", CreativeTabs.MATERIALS);
    private static final List<Item> ITEMS = buildItems();

    private OriginalGear() {}
    private static Block ore(String id, float hardness, int harvest) {
        Block block = new Block(Material.ROCK).setRegistryName(NexusRpg.MODID, id)
            .setUnlocalizedName(NexusRpg.MODID + "." + id).setHardness(hardness)
            .setResistance(hardness * 3).setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        block.setHarvestLevel("pickaxe", harvest);
        return block;
    }
    private static Item named(Item item, String id, CreativeTabs tab) {
        return item.setRegistryName(NexusRpg.MODID, id)
            .setUnlocalizedName(NexusRpg.MODID + "." + id).setCreativeTab(tab);
    }
    private static List<Item> buildItems() {
        List<Item> items = new ArrayList<>();
        items.add(AURONITE_INGOT);
        items.add(UMBRILITE_INGOT);
        addTier(items, "auronite", AURONITE_INGOT, 2, 700, 7.0F, 2.5F, 14,
                20, new int[] {2, 5, 6, 2}, 0.0F);
        addTier(items, "umbrilite", UMBRILITE_INGOT, 3, 1300, 8.0F, 3.0F, 16,
                28, new int[] {3, 5, 7, 3}, 1.0F);
        return Collections.unmodifiableList(items);
    }
    private static void addTier(List<Item> items, String tier, Item ingot, int harvest,
            int uses, float speed, float damage, int enchant, int durability, int[] defense, float toughness) {
        String enumName = "NEXUSRPG_" + tier.toUpperCase(java.util.Locale.ROOT);
        Item.ToolMaterial tools = EnumHelper.addToolMaterial(enumName, harvest, uses, speed, damage, enchant)
            .setRepairItem(new ItemStack(ingot));
        ItemArmor.ArmorMaterial armor = EnumHelper.addArmorMaterial(enumName, NexusRpg.MODID + ":" + tier,
            durability, defense, enchant, SoundEvents.ITEM_ARMOR_EQUIP_IRON, toughness)
            .setRepairItem(new ItemStack(ingot));
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
    public static List<Item> items() { return ITEMS; }
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(AURONITE_ORE, UMBRILITE_ORE);
    }
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : ITEMS) event.getRegistry().register(item);
        for (Block block : new Block[] {AURONITE_ORE, UMBRILITE_ORE}) {
            ItemBlock item = new ItemBlock(block);
            item.setRegistryName(block.getRegistryName());
            event.getRegistry().register(item);
        }
    }
    /** Called during FML initialization after block and item registration. */
    public static void registerSmelting() {
        GameRegistry.addSmelting(AURONITE_ORE, new ItemStack(AURONITE_INGOT), 0.5F);
        GameRegistry.addSmelting(UMBRILITE_ORE, new ItemStack(UMBRILITE_INGOT), 0.8F);
    }
}
