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

    // All attack timing comes from the common interface, in ticks plus partial ticks.
    // Window endpoints return smoothly to zero, so shared model instances do not
    // retain a previous entity's pose or require client-side animation counters.
    private static float rise(float tick, float from, float to) {
        return ease((tick - from) / (to - from));
    }

    private static float window(float tick, float inStart, float inEnd, float outStart, float outEnd) {
        return rise(tick, inStart, inEnd) * (1F - rise(tick, outStart, outEnd));
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        zero(root); zero(torso); zero(head); zero(crown);
        zero(armR); zero(armL); zero(legR); zero(legL);
        zero(antR); zero(antL); zero(tineAR); zero(tineBR); zero(tineAL); zero(tineBL);
        antR.rotateAngleZ = -0.35F; antL.rotateAngleZ = 0.35F;
        tineAR.rotateAngleZ = -0.8F; tineBR.rotateAngleZ = 0.5F;
        tineAL.rotateAngleZ = 0.8F; tineBL.rotateAngleZ = -0.5F;
        root.setRotationPoint(0F, 24F, 0F);
        glow = 1F;

        float tick = Math.max(0F, animTime * 20F);
        float wind = 0F, impact = 0F, roar = 0F, spin = 0F, fall = 0F;
        if (animState == IEntAnciaoAnimated.SLAM) {
            // Windup finishes at 18; fists descend through the impact at tick 22.
            wind = window(tick, 0F, 18F, 18F, 22F);
            impact = window(tick, 18F, 22F, 28F, 52F);
        } else if (animState == IEntAnciaoAnimated.ROAR) {
            roar = window(tick, 0F, 12F, 44F, 60F);
        } else if (animState == IEntAnciaoAnimated.SPIN) {
            spin = window(tick, 0F, 10F, 68F, 80F);
        } else if (animState == IEntAnciaoAnimated.DEATH) {
            fall = rise(tick, 0F, 16F);
        }
        float action = MathHelper.clamp(wind + impact + roar + spin + fall, 0F, 1F);
        float walk = MathHelper.clamp(limbSwingAmount, 0F, 1F) * (1F - action);
        float step = MathHelper.cos(limbSwing * 0.6F) * walk;
        // Do not change phase on becoming enraged: doing so jumps the entire pose.
        float breath = MathHelper.sin(ageInTicks * 0.08F);
        float sway = MathHelper.sin(ageInTicks * 0.055F);
        float idle = 1F - 0.75F * action;
        float tension = enraged ? 1.35F : 1F;

        torso.rotateAngleX = breath * 0.018F * tension * idle;
        torso.rotateAngleY = step * 0.09F;
        torso.rotateAngleZ = step * 0.035F;
        head.rotateAngleY = (sway * 0.10F + MathHelper.clamp(netHeadYaw, -50F, 50F) * 0.0087F) * (1F - 0.8F * spin);
        head.rotateAngleX = breath * 0.024F * idle + MathHelper.clamp(headPitch, -30F, 30F) * 0.0087F * (1F - 0.8F * action);
        head.rotateAngleZ = -step * 0.025F;
        armR.rotateAngleZ = 0.06F + breath * 0.02F * idle;
        armL.rotateAngleZ = -0.06F - breath * 0.02F * idle;
        armR.rotateAngleX = MathHelper.sin(ageInTicks * 0.055F) * 0.05F * idle - step * 0.55F;
        armL.rotateAngleX = -MathHelper.sin(ageInTicks * 0.055F + 0.2F) * 0.05F * idle + step * 0.55F;
        legR.rotateAngleX = step * 0.75F;
        legL.rotateAngleX = -step * 0.75F;
        root.rotationPointY -= Math.abs(step) * 0.5F;
        crown.rotateAngleZ = -sway * 0.025F;

        // The heavy trunk leans back before both fists fall, then settles.
        if (animState == IEntAnciaoAnimated.SLAM) {
            float shudder = MathHelper.sin((tick - 22F) * 0.8F) * window(tick, 22F, 24F, 24F, 38F);
            torso.rotateAngleX += -0.27F * wind + 0.42F * impact + 0.035F * shudder;
            torso.rotateAngleY += 0.04F * wind;
            head.rotateAngleX += -0.24F * wind + 0.24F * impact - 0.06F * shudder;
            armR.rotateAngleX += -2.65F * wind - 1.32F * impact;
            armL.rotateAngleX += -2.60F * wind - 1.37F * impact;
            armR.rotateAngleZ += 0.18F * wind + 0.05F * impact;
            armL.rotateAngleZ -= 0.15F * wind + 0.05F * impact;
            legR.rotateAngleX += 0.14F * wind - 0.20F * impact;
            legL.rotateAngleX += 0.11F * wind - 0.18F * impact;
            root.rotationPointY += 1.3F * impact + 0.14F * shudder;
            crown.rotateAngleX += 0.07F * impact - 0.09F * wind;
        } else if (animState == IEntAnciaoAnimated.ROAR) {
            float rumble = MathHelper.sin(tick * 0.72F) * roar;
            torso.rotateAngleX -= 0.23F * roar;
            head.rotateAngleX -= 0.55F * roar;
            head.rotateAngleZ += 0.025F * rumble;
            armR.rotateAngleX -= 0.32F * roar;
            armL.rotateAngleX -= 0.29F * roar;
            armR.rotateAngleZ += 0.95F * roar;
            armL.rotateAngleZ -= 0.95F * roar;
            legR.rotateAngleX += 0.10F * roar;
            legL.rotateAngleX += 0.10F * roar;
            root.rotationPointY += 0.35F * roar;
            crown.rotateAngleZ += 0.035F * rumble;
        } else if (animState == IEntAnciaoAnimated.SPIN) {
            // Exactly three revolutions, ending on the original heading at tick 72.
            float turn = rise(tick, 8F, 72F) * (float) (Math.PI * 6.0);
            float stride = MathHelper.sin(turn * 2F) * spin;
            root.rotateAngleY = -turn;
            torso.rotateAngleX += 0.13F * spin;
            torso.rotateAngleZ += 0.045F * stride;
            head.rotateAngleX += 0.10F * spin;
            armR.rotateAngleZ += 1.35F * spin;
            armL.rotateAngleZ -= 1.35F * spin;
            armR.rotateAngleX -= 0.18F * spin;
            armL.rotateAngleX -= 0.18F * spin;
            legR.rotateAngleX += 0.30F * stride;
            legL.rotateAngleX -= 0.30F * stride;
            root.rotationPointY -= Math.abs(stride) * 0.30F;
            crown.rotateAngleZ -= 0.05F * stride;
        } else if (animState == IEntAnciaoAnimated.DEATH) {
            head.rotateAngleX += 0.6F * fall;
            torso.rotateAngleX += 0.5F * fall;
            armR.rotateAngleX += 0.3F * fall;
            armL.rotateAngleX += 0.3F * fall;
            armR.rotateAngleZ += 0.4F * fall;
            armL.rotateAngleZ -= 0.4F * fall;
            legR.rotateAngleX -= 0.3F * fall;
            legL.rotateAngleX -= 0.3F * fall;
            root.rotationPointY += 1.5F * fall;
            glow = 1F - fall;
        }

        // Secondary motion trails the movement, but returns to the original
        // attachment angles on every invocation (including the glow pass).
        antR.rotateAngleX = antL.rotateAngleX = -0.025F * breath + 0.04F * impact;
        antR.rotateAngleZ += 0.025F * sway;
        antL.rotateAngleZ += 0.025F * sway;
        for (int i = 0; i < vines.length; i++) {
            float lag = ageInTicks * 0.10F + i * 0.75F;
            vines[i].rotateAngleX = 0.035F * MathHelper.sin(lag - 0.8F) + 0.12F * wind - 0.15F * impact + 0.10F * spin;
            vines[i].rotateAngleY = 0F;
            vines[i].rotateAngleZ = 0.07F * MathHelper.sin(lag) + (i < 3 ? -1F : 1F) * 0.12F * spin;
        }
    }

    /** Camada de brilho (olhos e coração) que ignora a iluminação do mundo. */
    @SideOnly(Side.CLIENT)
    public static class GlowLayer implements LayerRenderer<EntityLivingBase> {
        private final RenderLivingBase<?> renderer;
        private final ResourceLocation calm, rage;
        private final FloatBuffer savedColor = BufferUtils.createFloatBuffer(4);

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
            FloatBuffer color = savedColor;
            color.clear();
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
