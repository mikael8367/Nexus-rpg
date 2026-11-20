package com.nexusrpg;

import net.minecraftforge.fml.common.Mod;
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
        logger.info("Nexus RPG: preparando sistemas...");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        logger.info("Nexus RPG: inicializando...");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        logger.info("Nexus RPG: carregado!");
    }
}
