package com.nexusrpg;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        event.getModLog().info("Nexus RPG: preparando sistemas...");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        event.getModLog().info("Nexus RPG: inicializando...");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        event.getModLog().info("Nexus RPG: carregado!");
    }
}
