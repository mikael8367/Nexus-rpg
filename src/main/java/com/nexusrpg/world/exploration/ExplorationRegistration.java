package com.nexusrpg.world.exploration;

import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Forge discovers this subscriber without changing the existing mod entry point. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class ExplorationRegistration {
    private static boolean registered;
    private ExplorationRegistration() {}

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Block> event) {
        if (!registered) {
            registered = true;
            GameRegistry.registerWorldGenerator(new ExplorationGenerator(), 2);
        }
    }
}
