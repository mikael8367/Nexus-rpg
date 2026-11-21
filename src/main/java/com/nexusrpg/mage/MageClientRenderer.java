package com.nexusrpg.mage;

import net.minecraft.client.model.ModelWitch;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Only discovered on the physical client, before RenderManager is constructed. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = "nexusrpg", value = Side.CLIENT)
public final class MageClientRenderer {
    private MageClientRenderer() { }

    @SubscribeEvent
    public static void registerRenderer(RegistryEvent.Register<EntityEntry> event) {
        RenderingRegistry.registerEntityRenderingHandler(EntityTowerMage.class, MageRender::new);
    }

    private static final class MageRender extends RenderLiving<EntityTowerMage> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/witch.png");

        private MageRender(RenderManager manager) {
            super(manager, new ModelWitch(0.0F), 0.5F);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityTowerMage entity) {
            return TEXTURE;
        }
    }
}
