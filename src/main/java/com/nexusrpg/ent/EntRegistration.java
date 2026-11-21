package com.nexusrpg.ent;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Registered once through Forge's registry event; deliberately has no natural spawn entry. */
@Mod.EventBusSubscriber(modid = "nexusrpg")
public final class EntRegistration {
    private EntRegistration() { }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.<EntityEntAnciao>create()
                .entity(EntityEntAnciao.class)
                .id(new ResourceLocation("nexusrpg", "ent_anciao"), 1)
                .name("ent_anciao")
                .tracker(80, 1, true)
                .build());
    }

    /** Class is discovered only on the physical client, before RenderManager is constructed. */
    @SideOnly(Side.CLIENT)
    @Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
    public static final class ClientRegistration {
        private ClientRegistration() { }

        @SubscribeEvent
        public static void registerRenderer(RegistryEvent.Register<EntityEntry> event) {
            com.nexusrpg.ent.client.EntClientRenderer.register();
        }
    }
}
