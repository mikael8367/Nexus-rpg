package com.nexusrpg.magma;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class MagmaRegistration {
    private MagmaRegistration() { }
    @SubscribeEvent public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.<EntityColossoMagma>create()
                .entity(EntityColossoMagma.class)
                .id(new ResourceLocation("nexusrpg", "colosso_magma"), 2)
                .name("colosso_magma").tracker(80, 1, true).build());
    }
    @SideOnly(Side.CLIENT)
    @Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
    public static final class ClientRegistration {
        private ClientRegistration() { }
        @SubscribeEvent public static void registerRenderer(RegistryEvent.Register<EntityEntry> event) {
            com.nexusrpg.magma.client.RenderColossoMagma.register();
        }
    }
}
