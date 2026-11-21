package com.nexusrpg.supplies;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
public final class SupplyModels {
    private SupplyModels() {}
    @SubscribeEvent public static void register(ModelRegistryEvent event) {
        for (Item item : new Item[] {SupplyRegistry.BANDAGE, SupplyRegistry.ANTIDOTE, SupplyRegistry.ESCAPE})
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
