package com.nexusrpg;

import com.nexusrpg.init.ModBlocks;
import com.nexusrpg.compass.CompassNetwork;
import com.nexusrpg.init.ModItems;
import com.nexusrpg.init.NexusCreativeTab;
import com.nexusrpg.world.OreWorldGenerator;
import com.nexusrpg.world.structures.NexusStructureGenerator;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = NexusRpg.MODID,
    name = NexusRpg.NAME,
    version = NexusRpg.VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class NexusRpg {

    public static final String MODID = "nexusrpg";
    public static final String NAME = "Nexus RPG";
    public static final String VERSION = "0.1.0";

    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        NexusCreativeTab.init();
        CompassNetwork.init();
        com.nexusrpg.character.CharacterNetwork.init();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.nexusrpg.rest.RestEvents());
        GameRegistry.registerWorldGenerator(new OreWorldGenerator(), 0);
        com.nexusrpg.world.structures.WizardTower.setSpawnHandler((world, feet) -> {
            if (com.nexusrpg.mage.MageRegistration.spawnTowerMage(world, feet) == null) {
                logger.warn("Não foi possível criar o mago da torre em {}", feet);
            }
        });
        GameRegistry.registerWorldGenerator(new NexusStructureGenerator(), 1);
        GameRegistry.registerWorldGenerator(new com.nexusrpg.world.ent.EntClearingGenerator(), 3);
        GameRegistry.registerWorldGenerator(new com.nexusrpg.dragons.world.DragonNestGenerator(), 4);
        logger.info("Nexus RPG: preparando sistemas...");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.addSmelting(ModBlocks.MITHRIL_ORE, new ItemStack(ModItems.MITHRIL_INGOT), 0.7F);
        GameRegistry.addSmelting(ModBlocks.ADAMANTITE_ORE, new ItemStack(ModItems.ADAMANTITE_INGOT), 1.0F);
        GameRegistry.addSmelting(ModBlocks.DRACONITE_ORE, new ItemStack(ModItems.DRACONITE_INGOT), 1.5F);
        logger.info("Nexus RPG: inicializando...");
    }

    @Mod.EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new com.nexusrpg.quests.ZombieHunt.HuntCommand());
        event.registerServerCommand(new com.nexusrpg.quests.TrailHunts.TrailCommand());
    }

    @Mod.EventHandler
    public void serverStopping(net.minecraftforge.fml.common.event.FMLServerStoppingEvent event) {
        CompassNetwork.clearJobs();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Nexus RPG: carregado!");
    }
}
