package com.nexusrpg.init;

import com.nexusrpg.NexusRpg;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Aba comum para todos os itens e blocos registrados no namespace do mod. */
@Mod.EventBusSubscriber(modid = NexusRpg.MODID)
public final class NexusCreativeTab {
    public static final CreativeTabs TAB = new CreativeTabs("nexusrpg") {
        @Override
        public ItemStack getTabIconItem() {
            ItemStack icon = new ItemStack(ModBlocks.MITHRIL_ORE);
            return icon.isEmpty() ? new ItemStack(Items.COMPASS) : icon;
        }

        @Override
        public String getTranslatedTabLabel() {
            return NexusRpg.NAME;
        }

        @Override
        public void displayAllRelevantItems(NonNullList<ItemStack> items) {
            // Recheck the final registry in case another LOWEST-priority handler
            // registered an item after our registration event listener ran.
            for (Item item : Item.REGISTRY) {
                if (!belongsToMod(item.getRegistryName())) continue;
                assignItem(item);
                NonNullList<ItemStack> variants = NonNullList.create();
                item.getSubItems(this, variants);
                int previousSize = items.size();
                for (ItemStack variant : variants) {
                    if (!variant.isEmpty() && belongsToMod(variant.getItem().getRegistryName())) {
                        items.add(variant);
                    }
                }
                // Preserve a default for items which suppress all their variants.
                if (items.size() == previousSize) items.add(new ItemStack(item));
            }
        }
    };

    private NexusCreativeTab() {}

    /** Ensure the tab is constructed even if a registry event was not received. */
    public static void init() {}

    private static boolean belongsToMod(ResourceLocation name) {
        return name != null && NexusRpg.MODID.equals(name.getResourceDomain());
    }

    private static void assignItem(Item item) {
        // ItemBlock delegates its creative tab to its Block, not to Item.
        if (item instanceof ItemBlock) {
            Block block = ((ItemBlock) item).getBlock();
            if (belongsToMod(block.getRegistryName())) block.setCreativeTab(TAB);
        }
        item.setCreativeTab(TAB);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void assignBlocks(RegistryEvent.Register<Block> event) {
        for (Block block : event.getRegistry().getValuesCollection()) {
            if (belongsToMod(block.getRegistryName())) block.setCreativeTab(TAB);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void assignItems(RegistryEvent.Register<Item> event) {
        for (Item item : event.getRegistry().getValuesCollection()) {
            if (belongsToMod(item.getRegistryName())) assignItem(item);
        }
    }
}
