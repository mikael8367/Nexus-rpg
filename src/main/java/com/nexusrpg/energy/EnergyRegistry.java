package com.nexusrpg.energy;

import com.nexusrpg.NexusRpg;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = NexusRpg.MODID)
public final class EnergyRegistry {
    public static final Block SOLAR_PANEL = new BlockPowerMachine("solar_panel_large", true);
    public static final Block COAL_GENERATOR = new BlockPowerMachine("coal_generator", false);
    public static final Block CABLE = new BlockEnergyCable();
    public static final Block MINER = new BlockDigitalMiner().setRegistryName(NexusRpg.MODID, "digital_miner");
    private EnergyRegistry() {}

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(SOLAR_PANEL, COAL_GENERATOR, CABLE, MINER);
        GameRegistry.registerTileEntity(TileSolarPanel.class, new ResourceLocation(NexusRpg.MODID, "solar_panel_large"));
        GameRegistry.registerTileEntity(TileCoalGenerator.class, new ResourceLocation(NexusRpg.MODID, "coal_generator"));
        GameRegistry.registerTileEntity(TileEnergyCable.class, new ResourceLocation(NexusRpg.MODID, "energy_cable"));
        GameRegistry.registerTileEntity(TileDigitalMiner.class, new ResourceLocation(NexusRpg.MODID, "digital_miner"));
    }
    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> event) {
        for (Block block : new Block[]{SOLAR_PANEL, COAL_GENERATOR, CABLE, MINER}) {
            event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        }
    }
}
