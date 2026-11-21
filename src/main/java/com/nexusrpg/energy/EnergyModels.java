package com.nexusrpg.energy;

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
public final class EnergyModels {
    private EnergyModels() {}
    @SubscribeEvent public static void models(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(BlockPowerMachine.COAL_RECOVERY, 0, new ModelResourceLocation(BlockPowerMachine.COAL_RECOVERY.getRegistryName(), "inventory"));
        for (Block block : new Block[]{EnergyRegistry.SOLAR_PANEL, EnergyRegistry.COAL_GENERATOR, EnergyRegistry.CABLE, EnergyRegistry.MINER}) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
        }
    }
}
