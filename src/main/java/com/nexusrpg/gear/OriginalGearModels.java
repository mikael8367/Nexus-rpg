package com.nexusrpg.gear;

import com.nexusrpg.NexusRpg;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NexusRpg.MODID, value = Side.CLIENT)
public final class OriginalGearModels {
    private OriginalGearModels() {}
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : OriginalGear.items()) register(item);
        for (Block block : new Block[] {OriginalGear.AURONITE_ORE, OriginalGear.UMBRILITE_ORE}) {
            register(Item.getItemFromBlock(block));
        }
    }
    private static void register(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
