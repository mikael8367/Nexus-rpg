package com.nexusrpg.magic;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
public final class MagicModels {
    private MagicModels() {}
    @SubscribeEvent
    public static void register(ModelRegistryEvent event) {
        for (Item item : new Item[] { MagicRegistry.ARCANE_STAFF, MagicRegistry.HEALING_STAFF,
                MagicRegistry.WARD_STAFF, MagicRegistry.SWIFT_STAFF, MagicRegistry.FLAMEWARD_STAFF,
                MagicRegistry.NIGHTVISION_STAFF, MagicRegistry.WATERBREATHING_STAFF }) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
