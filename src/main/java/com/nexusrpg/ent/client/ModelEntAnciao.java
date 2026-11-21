package com.nexusrpg.ent.client;

import com.nexusrpg.ent.IEntAnciaoAnimated;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Ent Ancião, boss da floresta (Forge 1.12.2).
 * Texturas: ent_anciao.png, ent_anciao_glow.png e ent_anciao_glow_rage.png (256x128).
 * Hitbox sugerida na entidade: setSize(1.8F, 3.9F).
 *
 * Sem nenhuma interface na entidade, o modelo usa:
 *  - swingProgress  -> animação de esmagar com os punhos
 *  - vida abaixo de 50% -> Fase 2 (mais rápido, olhos e coração vermelhos)
 *  - deathTime -> pose de derrota
 * Para rugido e giro, implemente IEntAnciaoAnimated na entidade.
 */
@SideOnly(Side.CLIENT)
public class ModelEntAnciao extends ModelBase {

    private ModelRenderer root, legR, legL, torso, head, armR, armL, crown, antR, tineAR, tineBR, antL, tineAL, tineBL;
    private final ModelRenderer[] vines = new ModelRenderer[6];

    public boolean enraged;
    public float glow = 1F;
    private int animState;
    private float animTime;

    public ModelEntAnciao() {
        this.textureWidth = 256;
        this.textureHeight = 128;
        root = part(null, 0F, 24F, 0F, 0F);
        legR = part(root, -5F, -20F, 0F, 0F);
        box(legR, 50, 0, -3F, 0F, -3F, 6, 20, 6);
        box(legR, 64, 27, -4F, 17F, -6F, 8, 3, 10);
        box(legR, 188, 43, -3.5F, 0F, -3.5F, 7, 2, 7);
        legL = part(root, 5F, -20F, 0F, 0F);
        box(legL, 74, 0, -3F, 0F, -3F, 6, 20, 6);
        box(legL, 100, 27, -4F, 17F, -6F, 8, 3, 10);
        box(legL, 216, 43, -3.5F, 0F, -3.5F, 7, 2, 7);
        torso = part(root, 0F, -20F, 0F, 0F);
        box(torso, 0, 43, -7F, -2.5F, -4F, 14, 3, 8);
        box(torso, 0, 0, -8F, -18F, -4.5F, 16, 18, 9);
        box(torso, 136, 27, -8F, -18F, -5F, 16, 3, 10);
        box(torso, 0, 54, -4F, -14F, -5.1F, 8, 8, 1);
        box(torso, 64, 54, -2.5F, -12.5F, -5.6F, 5, 5, 1);
        box(torso, 188, 27, -6F, -16F, 4.5F, 12, 10, 3);
        box(torso, 26, 54, -4F, -6F, 4.5F, 8, 4, 3);
        box(torso, 44, 43, -14.5F, -21F, -3.5F, 7, 4, 7);
        box(torso, 212, 54, -13F, -23.5F, -2F, 2, 3, 2);
        box(torso, 76, 54, -14F, -25F, -3F, 4, 2, 4);
        box(torso, 240, 54, -9F, -22F, 2F, 2, 2, 2);
        box(torso, 72, 43, 7.5F, -21F, -3.5F, 7, 4, 7);
        box(torso, 220, 54, 11F, -23.5F, -2F, 2, 3, 2);
        box(torso, 92, 54, 10F, -25F, -3F, 4, 2, 4);
        box(torso, 248, 54, 7F, -22F, 2F, 2, 2, 2);
        head = part(torso, 0F, -18F, 0F, 0F);
        box(head, 32, 27, -4F, -8F, -4F, 8, 8, 8);
        box(head, 8, 63, -1.5F, -2.5F, -4.7F, 3, 2, 1);
        box(head, 28, 63, -2F, -1.5F, -4.9F, 4, 1, 1);
        box(head, 16, 63, -3F, -6F, -4.6F, 2, 2, 1);
        box(head, 22, 63, 1F, -6F, -4.6F, 2, 2, 1);
        armR = part(torso, -11F, -16F, 0F, 0F);
        box(armR, 98, 0, -2.5F, 0F, -2.5F, 5, 16, 5);
        box(armR, 194, 0, -4F, 15F, -4F, 8, 8, 8);
        box(armR, 116, 43, -4F, 14.5F, -4F, 8, 2, 8);
        box(armR, 108, 54, -3.5F, 22F, -3F, 2, 4, 2);
        box(armR, 116, 54, 1.5F, 22F, -3F, 2, 4, 2);
        armL = part(torso, 11F, -16F, 0F, 0F);
        box(armL, 118, 0, -2.5F, 0F, -2.5F, 5, 16, 5);
        box(armL, 0, 27, -4F, 15F, -4F, 8, 8, 8);
        box(armL, 148, 43, -4F, 14.5F, -4F, 8, 2, 8);
        box(armL, 124, 54, -3.5F, 22F, -3F, 2, 4, 2);
        box(armL, 132, 54, 1.5F, 22F, -3F, 2, 4, 2);
        crown = part(head, 0F, -8F, 0F, 0F);
        box(crown, 138, 0, -7F, -3F, -7F, 14, 3, 14);
        box(crown, 218, 27, -4.5F, -6F, -4.5F, 9, 3, 9);
        box(crown, 228, 54, 1.5F, -7.5F, -3.5F, 3, 2, 3);
        box(crown, 0, 63, 2F, -6.5F, -3F, 2, 2, 2);
        antR = part(head, -3.5F, -8F, 0F, -0.35F);
        box(antR, 100, 43, -1F, -9F, -1F, 2, 9, 2);
        tineAR = part(antR, 0F, -5F, 0F, -0.8F);
        box(tineAR, 48, 54, -1F, -5F, -1F, 2, 5, 2);
        box(tineAR, 140, 54, -1.5F, -6.5F, -1.5F, 3, 3, 3);
        tineBR = part(antR, 0F, -8.5F, 0F, 0.5F);
        box(tineBR, 152, 54, -1F, -4F, -1F, 2, 4, 2);
        box(tineBR, 160, 54, -1.5F, -6F, -1.5F, 3, 3, 3);
        antL = part(head, 3.5F, -8F, 0F, 0.35F);
        box(antL, 108, 43, -1F, -9F, -1F, 2, 9, 2);
        tineAL = part(antL, 0F, -5F, 0F, 0.8F);
        box(tineAL, 56, 54, -1F, -5F, -1F, 2, 5, 2);
        box(tineAL, 172, 54, -1.5F, -6.5F, -1.5F, 3, 3, 3);
        tineBL = part(antL, 0F, -8.5F, 0F, -0.5F);
        box(tineBL, 184, 54, -1F, -4F, -1F, 2, 4, 2);
        box(tineBL, 192, 54, -1.5F, -6F, -1.5F, 3, 3, 3);
        vines[0] = part(head, -4.7F, -7F, 3F, 0F);
        box(vines[0], 204, 54, -0.5F, 0F, -0.5F, 1, 5, 1);
        vines[1] = part(head, -4.7F, -7F, 0F, 0F);
        box(vines[1], 18, 54, -0.5F, 0F, -0.5F, 1, 7, 1);
        vines[2] = part(head, -4.7F, -7F, -3F, 0F);
        box(vines[2], 180, 43, -0.5F, 0F, -0.5F, 1, 9, 1);
        vines[3] = part(head, 4.7F, -7F, 3F, 0F);
        box(vines[3], 208, 54, -0.5F, 0F, -0.5F, 1, 5, 1);
        vines[4] = part(head, 4.7F, -7F, 0F, 0F);
        box(vines[4], 22, 54, -0.5F, 0F, -0.5F, 1, 7, 1);
        vines[5] = part(head, 4.7F, -7F, -3F, 0F);
        box(vines[5], 184, 43, -0.5F, 0F, -0.5F, 1, 9, 1);
    }

    private ModelRenderer part(ModelRenderer parent, float x, float y, float z, float rz) {
        ModelRenderer r = new ModelRenderer(this);
        r.setTextureSize(this.textureWidth, this.textureHeight);
        r.setRotationPoint(x, y, z);
        r.rotateAngleZ = rz;
        if (parent != null) parent.addChild(r);
        return r;
    }

    private static void box(ModelRenderer r, int u, int v, float x, float y, float z, int w, int h, int d) {
        r.setTextureOffset(u, v).addBox(x, y, z, w, h, d);
    }

    @Override
    public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entityIn);
        root.render(scale);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase e, float limbSwing, float limbSwingAmount, float partialTickTime) {
        animState = IEntAnciaoAnimated.NONE;
        animTime = 0F;
        if (e instanceof IEntAnciaoAnimated) {
            IEntAnciaoAnimated a = (IEntAnciaoAnimated) e;
            animState = a.getAnimState();
            animTime = (a.getAnimTicks() + partialTickTime) / 20F;
            enraged = a.isEnraged();
        } else {
            enraged = e.getHealth() < e.getMaxHealth() * 0.5F;
            float sp = e.getSwingProgress(partialTickTime);
            if (sp > 0F) {
                animState = IEntAnciaoAnimated.SLAM;
                animTime = sp * 2.6F;
            }
        }
        if (e.deathTime > 0) {
            animState = IEntAnciaoAnimated.DEATH;
            animTime = (e.deathTime + partialTickTime) / 20F;
        }
    }

    private static float ease(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    private static void zero(ModelRenderer r) {
        r.rotateAngleX = 0F;
        r.rotateAngleY = 0F;
        r.rotateAngleZ = 0F;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        zero(root); zero(torso); zero(head); zero(crown);
        zero(armR); zero(armL); zero(legR); zero(legL);
        root.setRotationPoint(0F, 24F, 0F);
        glow = 1F;

        float t = ageInTicks / 20F * (enraged ? 1.35F : 1F);
        float b = MathHelper.sin(t * 1.6F);

        // base: respiração e balanço
        torso.rotateAngleX = b * 0.015F;
        head.rotateAngleY = MathHelper.sin(t * 0.7F) * 0.2F + MathHelper.clamp(netHeadYaw, -50F, 50F) * 0.0087F;
        head.rotateAngleX = b * 0.03F + MathHelper.clamp(headPitch, -30F, 30F) * 0.0087F;
        armR.rotateAngleZ = 0.06F + b * 0.02F;
        armL.rotateAngleZ = -armR.rotateAngleZ;
        armR.rotateAngleX = MathHelper.sin(t * 1.1F) * 0.05F;
        armL.rotateAngleX = -armR.rotateAngleX;
        crown.rotateAngleZ = -MathHelper.sin(t * 1.3F) * 0.03F;

        // andar
        float w = MathHelper.cos(limbSwing * 0.6F) * limbSwingAmount;
        legR.rotateAngleX = w * 0.9F;
        legL.rotateAngleX = -w * 0.9F;
        armR.rotateAngleX -= w * 0.6F;
        armL.rotateAngleX += w * 0.6F;
        torso.rotateAngleY = w * 0.08F;
        root.rotationPointY -= Math.abs(w) * 0.8F;

        // cipós balançando
        for (int i = 0; i < vines.length; i++) {
            vines[i].rotateAngleZ = MathHelper.sin(t * 2F + i) * 0.1F;
        }

        float c = animTime;
        switch (animState) {
            case IEntAnciaoAnimated.SLAM: {
                float a, l, h;
                if (c < 1F) {
                    float k = ease(c);
                    a = -2.7F * k; l = -0.25F * k; h = -0.3F * k;
                } else if (c < 1.2F) {
                    float k = ease((c - 1F) / 0.2F);
                    a = -2.7F + 2.3F * k; l = -0.25F + 0.55F * k; h = -0.3F + 0.5F * k;
                    root.rotationPointY += 0.5F * k;
                } else {
                    float r = ease((c - 1.5F) / 1.1F);
                    a = -0.4F * (1F - r); l = 0.3F * (1F - r); h = 0.2F * (1F - r);
                }
                armR.rotateAngleX = a;
                armL.rotateAngleX = a;
                armR.rotateAngleZ = 0.08F;
                armL.rotateAngleZ = -0.08F;
                torso.rotateAngleX = l;
                head.rotateAngleX += h;
                legR.rotateAngleX = -l * 0.6F;
                legL.rotateAngleX = -l * 0.6F;
                break;
            }
            case IEntAnciaoAnimated.ROAR: {
                float k = ease(c / 0.6F) * (1F - ease((c - 2.2F) / 0.8F));
                head.rotateAngleX += -0.6F * k;
                torso.rotateAngleX = -0.2F * k;
                armR.rotateAngleZ = 1.1F * k;
                armL.rotateAngleZ = -1.1F * k;
                root.rotationPointX = MathHelper.sin(ageInTicks * 3F) * 0.2F * k;
                crown.rotateAngleZ += MathHelper.sin(ageInTicks * 1.5F) * 0.1F * k;
                break;
            }
            case IEntAnciaoAnimated.SPIN: {
                float k = ease(c / 0.4F) * (1F - ease((c - 3.2F) / 0.6F));
                armR.rotateAngleZ = 1.45F * k;
                armL.rotateAngleZ = -1.45F * k;
                torso.rotateAngleX = 0.1F * k;
                root.rotateAngleY = -6F * (float) Math.PI * ease(c / 3.4F);
                legR.rotateAngleX = MathHelper.sin(t * 10F) * 0.4F * k;
                legL.rotateAngleX = -legR.rotateAngleX;
                break;
            }
            case IEntAnciaoAnimated.DEATH: {
                float k = ease(c / 0.8F);
                head.rotateAngleX += 0.6F * k;
                torso.rotateAngleX = 0.5F * k;
                armR.rotateAngleX = 0.3F * k;
                armL.rotateAngleX = 0.3F * k;
                armR.rotateAngleZ = 0.4F * k;
                armL.rotateAngleZ = -0.4F * k;
                legR.rotateAngleX = -0.3F * k;
                legL.rotateAngleX = -0.3F * k;
                root.rotationPointY += 1.5F * k;
                glow = 1F - k;
                break;
            }
            default:
                break;
        }
    }

    /** Camada de brilho (olhos e coração) que ignora a iluminação do mundo. */
    @SideOnly(Side.CLIENT)
    public static class GlowLayer implements LayerRenderer<EntityLivingBase> {
        private final RenderLivingBase<?> renderer;
        private final ResourceLocation calm, rage;

        public GlowLayer(RenderLivingBase<?> renderer, ResourceLocation calm, ResourceLocation rage) {
            this.renderer = renderer;
            this.calm = calm;
            this.rage = rage;
        }

        @Override
        public void doRenderLayer(EntityLivingBase e, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            // Do not reveal an invisible entity through its emissive texture.
            if (e.isInvisible()) return;

            ModelEntAnciao m = (ModelEntAnciao) renderer.getMainModel();
            float pulse = m.glow * (0.75F + 0.25F * MathHelper.sin(ageInTicks * (m.enraged ? 0.3F : 0.15F)));

            // A layer runs inside the entity renderer: restore the exact incoming GL
            // state (including blend factors), not guessed vanilla defaults.
            boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean wasAlpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            // Black RGB texels contribute no light, but must not write depth: the
            // base texture has transparent pixels that the glow mask does not.
            boolean wasDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            FloatBuffer color = BufferUtils.createFloatBuffer(4);
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
            float lightX = OpenGlHelper.lastBrightnessX;
            float lightY = OpenGlHelper.lastBrightnessY;
            try {
                renderer.bindTexture(m.enraged ? rage : calm);
                GlStateManager.enableBlend();
                GlStateManager.disableAlpha();
                GlStateManager.depthMask(false);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
                GlStateManager.color(pulse, pulse, pulse, 1F);
                m.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            } finally {
                GlStateManager.depthMask(wasDepthMask);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
                GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
                GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
                if (wasBlend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
                if (wasAlpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
                GlStateManager.bindTexture(texture);
            }
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
