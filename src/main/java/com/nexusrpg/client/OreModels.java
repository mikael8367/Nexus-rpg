package com.nexusrpg.client;

import com.nexusrpg.NexusRpg;
import com.nexusrpg.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Registro dos modelos dos ItemBlocks, somente no cliente. */
@Mod.EventBusSubscriber(modid = NexusRpg.MODID, value = Side.CLIENT)
public final class OreModels {
    private OreModels() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        register(ModBlocks.MITHRIL_ORE);
        register(ModBlocks.ADAMANTITE_ORE);
        register(ModBlocks.DRACONITE_ORE);
    }

    private static void register(Block block) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }
}
