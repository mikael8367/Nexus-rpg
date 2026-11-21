package com.nexusrpg.ent.client;

import com.nexusrpg.ent.EntityEntAnciao;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Call register() once during client pre-initialization. */
@SideOnly(Side.CLIENT)
public final class EntClientRenderer extends RenderLiving<EntityEntAnciao> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("nexusrpg", "textures/entity/ent_anciao.png");
    private static final ResourceLocation GLOW = new ResourceLocation("nexusrpg", "textures/entity/ent_anciao_glow.png");
    private static final ResourceLocation GLOW_RAGE = new ResourceLocation("nexusrpg", "textures/entity/ent_anciao_glow_rage.png");

    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(EntityEntAnciao.class, EntClientRenderer::new);
    }

    private EntClientRenderer(RenderManager manager) {
        super(manager, new ModelEntAnciao(), 1.4F);
        addLayer(new ModelEntAnciao.GlowLayer(this, GLOW, GLOW_RAGE));
    }

    @Override
    protected float getDeathMaxRotation(EntityEntAnciao entity) {
        return 0.0F;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityEntAnciao entity) {
        return TEXTURE;
    }
}
