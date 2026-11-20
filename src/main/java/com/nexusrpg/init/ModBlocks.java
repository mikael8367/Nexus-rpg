package com.nexusrpg.init;

import com.nexusrpg.NexusRpg;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Blocos de minério e seus itens correspondentes. */
@Mod.EventBusSubscriber(modid = NexusRpg.MODID)
public final class ModBlocks {
    public static final Block MITHRIL_ORE = ore("mithril_ore", 3.0F, 2);
    public static final Block ADAMANTITE_ORE = ore("adamantite_ore", 4.5F, 3);
    public static final Block DRACONITE_ORE = ore("draconite_ore", 6.0F, 4);

    private ModBlocks() {}

    private static Block ore(String id, float hardness, int harvestLevel) {
        Block block = new Block(Material.ROCK)
            .setRegistryName(NexusRpg.MODID, id)
            .setUnlocalizedName(NexusRpg.MODID + "." + id)
            .setHardness(hardness)
            .setResistance(hardness * 3.0F)
            .setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        block.setHarvestLevel("pickaxe", harvestLevel);
        return block;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(MITHRIL_ORE, ADAMANTITE_ORE, DRACONITE_ORE);
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(itemBlock(MITHRIL_ORE), itemBlock(ADAMANTITE_ORE),
            itemBlock(DRACONITE_ORE));
    }

    private static ItemBlock itemBlock(Block block) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        return item;
    }
}
