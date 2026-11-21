package com.nexusrpg.magma.client;

import com.nexusrpg.magma.EntityColossoMagma;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderColossoMagma extends RenderLiving<EntityColossoMagma> {
    private static final ResourceLocation TEX = new ResourceLocation("nexusrpg", "textures/entity/colosso_magma.png");
    private static final ResourceLocation GLOW = new ResourceLocation("nexusrpg", "textures/entity/colosso_magma_glow.png");
    private static final ResourceLocation RAGE = new ResourceLocation("nexusrpg", "textures/entity/colosso_magma_glow_rage.png");
    public static void register() { RenderingRegistry.registerEntityRenderingHandler(EntityColossoMagma.class, RenderColossoMagma::new); }
    private RenderColossoMagma(RenderManager manager) {
        super(manager, new ModelColossoMagma(), 1.6F);
        addLayer(new ModelColossoMagma.GlowLayer(this, GLOW, RAGE));
    }
    @Override protected float getDeathMaxRotation(EntityColossoMagma entity) { return 0F; }
    @Override protected ResourceLocation getEntityTexture(EntityColossoMagma entity) { return TEX; }
}
