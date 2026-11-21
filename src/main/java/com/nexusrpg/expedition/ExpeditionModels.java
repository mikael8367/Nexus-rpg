package com.nexusrpg.expedition;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
public final class ExpeditionModels {
    private ExpeditionModels() {}
    @SubscribeEvent public static void register(ModelRegistryEvent event) {
        for (Item item : new Item[] {ExpeditionRegistry.QUENCH, ExpeditionRegistry.DIVE, ExpeditionRegistry.SIGHT})
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
