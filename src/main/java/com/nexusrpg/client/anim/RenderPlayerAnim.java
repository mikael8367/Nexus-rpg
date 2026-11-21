package com.nexusrpg.client.anim;

import java.util.List;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renderizador de jogador que usa o ModelPlayerAnim e anima também a armadura e a cabeça de bloco. */
@SideOnly(Side.CLIENT)
public class RenderPlayerAnim extends RenderPlayer {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public RenderPlayerAnim(RenderManager manager, boolean slim) {
        super(manager, slim);
        ModelPlayerAnim model = new ModelPlayerAnim(0F, slim);
        this.mainModel = model;
        List layers = this.layerRenderers;
        for (int i = 0; i < layers.size(); i++) {
            Object l = layers.get(i);
            if (l instanceof LayerBipedArmor) {
                layers.set(i, new ArmorLayer(this));
            } else if (l instanceof LayerCustomHead) {
                layers.set(i, new LayerCustomHead(model.bipedHead));
            }
        }
    }

    @Override
    protected void applyRotations(AbstractClientPlayer p, float age, float yaw, float partialTicks) {
        super.applyRotations(p, age, yaw, partialTicks);
        float[] fx = PlayerAnimManager.bodyFx(p, age);
        if (fx == null) return;
        GlStateManager.translate(0F, fx[1], 0F);
        if (fx[0] != 0F) {
            GlStateManager.translate(0F, 0.9F, 0F);
            GlStateManager.rotate(-fx[0], 1F, 0F, 0F); // negativo = para frente neste sistema
            GlStateManager.translate(0F, -0.9F, 0F);
        }
    }

    @SideOnly(Side.CLIENT)
    private static class ArmorLayer extends LayerBipedArmor {
        ArmorLayer(RenderLivingBase<?> renderer) {
            super(renderer);
        }

        @Override
        protected void initArmor() {
            this.modelLeggings = new ModelPlayerAnim.Armor(0.5F);
            this.modelArmor = new ModelPlayerAnim.Armor(1.0F);
        }
    }
}
