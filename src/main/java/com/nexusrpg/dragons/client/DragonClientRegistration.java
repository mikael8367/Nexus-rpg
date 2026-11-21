package com.nexusrpg.dragons.client;

import com.nexusrpg.dragons.EntityEarthDragon;
import com.nexusrpg.dragons.EntityFireDragon;
import com.nexusrpg.dragons.EntityWaterDragon;
import com.nexusrpg.dragons.EntityWindDragon;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Discovered by Forge only on the physical client, at the same registry phase as EntRegistration. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
public final class DragonClientRegistration {
    private DragonClientRegistration() { }

    @SubscribeEvent
    public static void registerRenderers(RegistryEvent.Register<EntityEntry> event) {
        RenderingRegistry.registerEntityRenderingHandler(EntityFireDragon.class,
                manager -> new DragonRenderer<EntityFireDragon>(manager, DragonModel.Kind.FIRE));
        RenderingRegistry.registerEntityRenderingHandler(EntityWaterDragon.class,
                manager -> new DragonRenderer<EntityWaterDragon>(manager, DragonModel.Kind.WATER));
        RenderingRegistry.registerEntityRenderingHandler(EntityWindDragon.class,
                manager -> new DragonRenderer<EntityWindDragon>(manager, DragonModel.Kind.WIND));
        RenderingRegistry.registerEntityRenderingHandler(EntityEarthDragon.class,
                manager -> new DragonRenderer<EntityEarthDragon>(manager, DragonModel.Kind.EARTH));
    }
}
