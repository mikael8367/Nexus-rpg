package com.nexusrpg.magma.client;

import com.nexusrpg.magma.IColossoMagmaAnimated;

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
 * Colosso de Magma, boss de lava (Forge 1.12.2).
 * Texturas: colosso_magma.png, colosso_magma_glow.png e colosso_magma_glow_rage.png (256x128).
 * Hitbox sugerida na entidade: setSize(2.2F, 4.1F) e isImmuneToFire = true.
 *
 * Sem nenhuma interface na entidade, o modelo usa:
 *  - swingProgress -> combo de socos (gancho direito, gancho esquerdo e finalização com as duas mãos)
 *  - vida abaixo de 50% -> Fase 2 (superaquecido: brilho amarelo-branco, olhos azulados, mais rápido)
 *  - deathTime -> o corpo esfria: o brilho apaga e ele desaba
 * Para sopro de lava, chuva de meteoros, investida e explosão de calor, implemente IColossoMagmaAnimated.
 * O modelo só move o corpo: chamas, meteoros e ondas de choque você cria na entidade (partículas ou entidades).
 */
@SideOnly(Side.CLIENT)
public class ModelColossoMagma extends ModelBase {

    private ModelRenderer root, legR, legL, torso, head, armR, armL, crest, jaw, vents, hornR, hornTipR, hornL, hornTipL;
    private final ModelRenderer[] drips = new ModelRenderer[4];

    public boolean enraged;
    public float glow = 1F;
    private int animState;
    private float animTime;

    public ModelColossoMagma() {
        this.textureWidth = 256;
        this.textureHeight = 128;
        root = part(null, 0F, 24F, 0F, 0F);
        legR = part(root, -6F, -22F, 0F, 0F);
        box(legR, 0, 0, -3.5F, 0F, -3.5F, 7, 22, 7);
        box(legR, 62, 63, -4F, 11F, -4F, 8, 2, 8);
        box(legR, 180, 29, -4.5F, 18F, -7F, 9, 4, 11);
        box(legR, 32, 75, -3.5F, 0F, -3.5F, 7, 2, 7);
        box(legR, 60, 75, -1F, 2F, -4.1F, 2, 8, 1);
        box(legR, 24, 85, 1.5F, 12F, -4.2F, 1, 6, 1);
        legL = part(root, 6F, -22F, 0F, 0F);
        box(legL, 28, 0, -3.5F, 0F, -3.5F, 7, 22, 7);
        box(legL, 94, 63, -4F, 11F, -4F, 8, 2, 8);
        box(legL, 0, 48, -4.5F, 18F, -7F, 9, 4, 11);
        box(legL, 66, 75, -3.5F, 0F, -3.5F, 7, 2, 7);
        box(legL, 94, 75, -1F, 2F, -4.1F, 2, 8, 1);
        box(legL, 28, 85, 1.5F, 12F, -4.2F, 1, 6, 1);
        torso = part(root, 0F, -22F, 0F, 0F);
        box(torso, 78, 48, -8F, -4F, -4.5F, 16, 4, 9);
        box(torso, 164, 48, -9F, -6F, -5F, 18, 2, 10);
        box(torso, 56, 0, -10F, -22F, -5.5F, 20, 16, 11);
        box(torso, 100, 75, -7F, -19F, -6.5F, 14, 8, 1);
        box(torso, 32, 85, -3F, -18F, -6.9F, 6, 6, 1);
        box(torso, 146, 85, -2F, -7F, -5.9F, 4, 4, 1);
        box(torso, 128, 48, -8.5F, -20F, -6.1F, 2, 12, 1);
        box(torso, 100, 29, -19F, -26F, -5F, 10, 6, 10);
        box(torso, 130, 75, -15.5F, -32F, -1.5F, 3, 6, 3);
        box(torso, 156, 85, -15.5F, -34F, -1.5F, 3, 2, 3);
        box(torso, 46, 85, -18.5F, -29.5F, -3F, 2, 5, 2);
        box(torso, 134, 48, -10.8F, -18F, -2.5F, 1, 8, 5);
        box(torso, 168, 85, -15F, -25F, -5.8F, 2, 4, 1);
        box(torso, 126, 63, -18F, -28F, -4F, 8, 2, 8);
        box(torso, 146, 48, 6.5F, -20F, -6.1F, 2, 12, 1);
        box(torso, 140, 29, 9F, -26F, -5F, 10, 6, 10);
        box(torso, 142, 75, 12.5F, -32F, -1.5F, 3, 6, 3);
        box(torso, 174, 85, 12.5F, -34F, -1.5F, 3, 2, 3);
        box(torso, 54, 85, 16.5F, -29.5F, -3F, 2, 5, 2);
        box(torso, 152, 48, 9.8F, -18F, -2.5F, 1, 8, 5);
        box(torso, 186, 85, 13F, -25F, -5.8F, 2, 4, 1);
        box(torso, 158, 63, 10F, -28F, -4F, 8, 2, 8);
        box(torso, 154, 75, -4F, -19F, 5.4F, 8, 8, 1);
        box(torso, 172, 75, -1.5F, -28F, 1.5F, 3, 6, 3);
        box(torso, 208, 75, -7.5F, -27F, 2.5F, 3, 5, 3);
        box(torso, 220, 75, 4.5F, -27F, 2.5F, 3, 5, 3);
        head = part(torso, 0F, -22F, 0F, 0F);
        box(head, 232, 75, -3F, 0F, -3F, 6, 2, 6);
        box(head, 40, 48, -5F, -9F, -4.5F, 10, 6, 9);
        box(head, 30, 93, -5F, -9.5F, -5.8F, 10, 2, 2);
        box(head, 70, 93, -3.5F, -7F, -5.1F, 2, 2, 1);
        box(head, 76, 93, 1.5F, -7F, -5.1F, 2, 2, 1);
        box(head, 82, 93, -3F, -4F, -5.1F, 6, 1, 1);
        armR = part(torso, -14F, -20F, 0F, 0F);
        box(armR, 206, 0, -3.5F, 0F, -3.5F, 7, 12, 7);
        box(armR, 214, 63, -4F, 8F, -4F, 8, 2, 8);
        box(armR, 28, 29, -4.5F, 12F, -4.5F, 9, 9, 9);
        box(armR, 106, 85, -3F, 12.5F, 4.5F, 2, 5, 2);
        box(armR, 114, 85, 1F, 12.5F, 4.5F, 2, 5, 2);
        box(armR, 118, 0, -5.5F, 21F, -5.5F, 11, 9, 11);
        box(armR, 240, 85, -5F, 24.5F, -7F, 3, 3, 2);
        box(armR, 0, 93, 2F, 24.5F, -7F, 3, 3, 2);
        armL = part(torso, 14F, -20F, 0F, 0F);
        box(armL, 0, 29, -3.5F, 0F, -3.5F, 7, 12, 7);
        box(armL, 0, 75, -4F, 8F, -4F, 8, 2, 8);
        box(armL, 64, 29, -4.5F, 12F, -4.5F, 9, 9, 9);
        box(armL, 122, 85, -3F, 12.5F, 4.5F, 2, 5, 2);
        box(armL, 130, 85, 1F, 12.5F, 4.5F, 2, 5, 2);
        box(armL, 162, 0, -5.5F, 21F, -5.5F, 11, 9, 11);
        box(armL, 10, 93, -5F, 24.5F, -7F, 3, 3, 2);
        box(armL, 20, 93, 2F, 24.5F, -7F, 3, 3, 2);
        crest = part(head, 0F, -9F, 0F, 0F);
        box(crest, 184, 75, -3F, -3F, -3F, 6, 3, 6);
        box(crest, 0, 85, -2F, -7F, -2F, 4, 4, 4);
        box(crest, 192, 85, -1F, -10F, -1F, 2, 3, 2);
        jaw = part(head, 0F, -3F, 3.5F, 0F);
        box(jaw, 0, 63, -5F, 0F, -8F, 10, 3, 9);
        box(jaw, 78, 85, -4F, -1F, -6.5F, 8, 1, 6);
        box(jaw, 200, 85, -4.5F, -3F, -8.5F, 2, 3, 2);
        box(jaw, 208, 85, 2.5F, -3F, -8.5F, 2, 3, 2);
        vents = part(torso, 0F, -20F, 7F, 0F);
        box(vents, 38, 63, -6.5F, -8F, -1.5F, 3, 8, 3);
        box(vents, 216, 85, -6.5F, -10F, -1.5F, 3, 2, 3);
        box(vents, 50, 63, 3.5F, -8F, -1.5F, 3, 8, 3);
        box(vents, 228, 85, 3.5F, -10F, -1.5F, 3, 2, 3);
        hornR = part(head, -4.5F, -9F, 0F, -0.5F);
        box(hornR, 190, 63, -1.5F, -7F, -1.5F, 3, 7, 3);
        hornTipR = part(hornR, 0F, -7F, 0F, -0.7F);
        box(hornTipR, 62, 85, -1F, -5F, -1F, 2, 5, 2);
        box(hornTipR, 54, 93, -1F, -7F, -1F, 2, 2, 2);
        hornL = part(head, 4.5F, -9F, 0F, 0.5F);
        box(hornL, 202, 63, -1.5F, -7F, -1.5F, 3, 7, 3);
        hornTipL = part(hornL, 0F, -7F, 0F, 0.7F);
        box(hornTipL, 70, 85, -1F, -5F, -1F, 2, 5, 2);
        box(hornTipL, 62, 93, -1F, -7F, -1F, 2, 2, 2);
        drips[0] = part(torso, -17F, -20F, -5.4F, 0F);
        box(drips[0], 138, 85, -0.5F, 0F, -0.5F, 1, 5, 1);
        drips[1] = part(torso, -11F, -20F, -5.4F, 0F);
        box(drips[1], 16, 85, -0.5F, 0F, -0.5F, 1, 7, 1);
        drips[2] = part(torso, 11F, -20F, -5.4F, 0F);
        box(drips[2], 142, 85, -0.5F, 0F, -0.5F, 1, 5, 1);
        drips[3] = part(torso, 17F, -20F, -5.4F, 0F);
        box(drips[3], 20, 85, -0.5F, 0F, -0.5F, 1, 7, 1);
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
        animState = IColossoMagmaAnimated.NONE;
        animTime = 0F;
        if (e instanceof IColossoMagmaAnimated) {
            IColossoMagmaAnimated a = (IColossoMagmaAnimated) e;
            animState = a.getAnimState();
            animTime = (a.getAnimTicks() + partialTickTime) / 20F;
            enraged = a.isEnraged();
        } else {
            enraged = e.getHealth() < e.getMaxHealth() * 0.5F;
            float sp = e.getSwingProgress(partialTickTime);
            if (sp > 0F) {
                animState = IColossoMagmaAnimated.PUNCH;
                animTime = sp * 1.9F;
            }
        }
        if (e.deathTime > 0) {
            animState = IColossoMagmaAnimated.DEATH;
            animTime = (e.deathTime + partialTickTime) / 20F;
        }
    }

    private static float ease(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    /** Preparação do gancho (0 a 1 a 0). */
    private static float hookW(float x) {
        return ease(x / 0.2F) * (1F - ease((x - 0.2F) / 0.1F));
    }

    /** Golpe do gancho (0 a 1 a 0). */
    private static float hookS(float x) {
        return ease((x - 0.2F) / 0.1F) * (1F - ease((x - 0.35F) / 0.15F));
    }

    private static void zero(ModelRenderer r) {
        r.rotateAngleX = 0F;
        r.rotateAngleY = 0F;
        r.rotateAngleZ = 0F;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        zero(root); zero(torso); zero(head); zero(crest); zero(jaw); zero(vents);
        zero(armR); zero(armL); zero(legR); zero(legL);
        root.setRotationPoint(0F, 24F, 0F);
        glow = 1F;

        float t = ageInTicks / 20F * (enraged ? 1.4F : 1F);
        float b = MathHelper.sin(t * 1.2F);

        // base: respiração pesada, chama da crista tremulando
        torso.rotateAngleX = b * 0.02F;
        head.rotateAngleY = MathHelper.sin(t * 0.5F) * 0.18F + MathHelper.clamp(netHeadYaw, -50F, 50F) * 0.0087F;
        head.rotateAngleX = b * 0.03F + MathHelper.clamp(headPitch, -30F, 30F) * 0.0087F;
        armR.rotateAngleZ = 0.05F + b * 0.015F;
        armL.rotateAngleZ = -armR.rotateAngleZ;
        armR.rotateAngleX = MathHelper.sin(t * 0.9F) * 0.04F;
        armL.rotateAngleX = -armR.rotateAngleX;
        crest.rotateAngleZ = MathHelper.sin(t * 7F) * 0.06F;
        crest.rotateAngleX = MathHelper.sin(t * 5.3F) * 0.05F;
        jaw.rotateAngleX = (b * 0.5F + 0.5F) * 0.05F;
        vents.rotateAngleX = MathHelper.sin(t * 9F) * 0.04F;

        // andar pesado
        float w = MathHelper.cos(limbSwing * 0.55F) * limbSwingAmount;
        legR.rotateAngleX = w * 0.8F;
        legL.rotateAngleX = -w * 0.8F;
        armR.rotateAngleX -= w * 0.55F;
        armL.rotateAngleX += w * 0.55F;
        torso.rotateAngleY = w * 0.1F;
        root.rotationPointY -= Math.abs(w) * 1.2F;

        // gotas de lava escorrendo dos ombros
        for (int i = 0; i < drips.length; i++) {
            drips[i].rotateAngleX = MathHelper.sin(t * 3F + i) * 0.15F;
        }

        float c = animTime;
        switch (animState) {
            case IColossoMagmaAnimated.PUNCH: {
                // combo: gancho direito, gancho esquerdo e finalização com as duas mãos
                float rw = hookW(c), rs = hookS(c), lw = hookW(c - 0.5F), ls = hookS(c - 0.5F);
                float up = ease((c - 1F) / 0.35F) * (1F - ease((c - 1.35F) / 0.1F));
                float dn = ease((c - 1.35F) / 0.1F) * (1F - ease((c - 1.6F) / 0.3F));
                armR.rotateAngleX = 0.8F * rw - 1.5F * rs - 2.6F * up - 0.4F * dn;
                armL.rotateAngleX = 0.8F * lw - 1.5F * ls - 2.6F * up - 0.4F * dn;
                armR.rotateAngleZ = 0.08F + 0.2F * up;
                armL.rotateAngleZ = -armR.rotateAngleZ;
                torso.rotateAngleY = 0.3F * rw - 0.45F * rs - 0.3F * lw + 0.45F * ls;
                torso.rotateAngleX = -0.25F * up + 0.3F * dn;
                head.rotateAngleX += -0.3F * up + 0.2F * dn;
                legR.rotateAngleX = -0.3F * dn;
                legL.rotateAngleX = -0.3F * dn;
                break;
            }
            case IColossoMagmaAnimated.BREATH: {
                // sopro de lava: inclina a cabeça para trás, abre a mandíbula e dispara
                float pre = ease(c / 0.8F) * (1F - ease((c - 0.8F) / 0.15F));
                float hold = ease((c - 0.8F) / 0.15F) * (1F - ease((c - 2.3F) / 0.5F));
                head.rotateAngleX += -0.5F * pre + 0.3F * hold;
                head.rotateAngleY += MathHelper.sin(ageInTicks * 0.7F) * 0.05F * hold;
                jaw.rotateAngleX = 0.7F * pre + 0.95F * hold + MathHelper.sin(ageInTicks * 0.8F) * 0.08F * hold;
                torso.rotateAngleX = -0.25F * pre + 0.15F * hold;
                armR.rotateAngleZ = 0.45F * (pre + hold);
                armL.rotateAngleZ = -armR.rotateAngleZ;
                armR.rotateAngleX = -0.2F * hold;
                armL.rotateAngleX = -0.2F * hold;
                root.rotationPointX = MathHelper.sin(ageInTicks) * 0.15F * hold;
                break;
            }
            case IColossoMagmaAnimated.METEOR: {
                // ergue os braços ao céu e arremessa; os meteoros caem logo depois (crie na entidade)
                float up = ease(c / 0.9F) * (1F - ease((c - 1F) / 0.15F));
                float th = ease((c - 1F) / 0.15F) * (1F - ease((c - 1.6F) / 0.9F));
                armR.rotateAngleX = -2.7F * up - 0.5F * th;
                armL.rotateAngleX = -2.7F * up - 0.5F * th;
                armR.rotateAngleZ = 0.35F * up;
                armL.rotateAngleZ = -armR.rotateAngleZ;
                head.rotateAngleX += -0.55F * up + 0.3F * th;
                torso.rotateAngleX = -0.3F * up + 0.35F * th;
                crest.rotateAngleZ += MathHelper.sin(ageInTicks * 1.5F) * 0.2F * up;
                root.rotationPointX = MathHelper.sin(ageInTicks * 2F) * 0.15F * up;
                legR.rotateAngleX = -0.2F * th;
                legL.rotateAngleX = -0.2F * th;
                break;
            }
            case IColossoMagmaAnimated.CHARGE: {
                // investida: agacha, joga os braços para trás e corre inclinado
                float pre = ease(c / 0.5F) * (1F - ease((c - 0.5F) / 0.2F));
                float run = ease((c - 0.5F) / 0.2F) * (1F - ease((c - 2.3F) / 0.3F));
                float sn = MathHelper.sin(t * 14F);
                torso.rotateAngleX = 0.5F * pre + 0.45F * run;
                torso.rotateAngleY = sn * 0.12F * run;
                head.rotateAngleX += -0.2F * pre - 0.35F * run;
                armR.rotateAngleX = pre + (1.1F + 0.25F * sn) * run;
                armL.rotateAngleX = pre + (1.1F - 0.25F * sn) * run;
                armR.rotateAngleZ = 0.25F * run;
                armL.rotateAngleZ = -0.25F * run;
                legR.rotateAngleX = -0.4F * pre + sn * run;
                legL.rotateAngleX = -0.4F * pre - sn * run;
                root.rotationPointY += 2F * pre - Math.abs(sn) * 2F * run;
                break;
            }
            case IColossoMagmaAnimated.NOVA: {
                // explosão de calor: treme concentrando energia e abre os braços de uma vez
                float cr = ease(c / 1.2F) * (1F - ease((c - 1.2F) / 0.12F));
                float bu = ease((c - 1.2F) / 0.12F) * (1F - ease((c - 1.6F) / 0.9F));
                root.rotationPointY += 4F * cr - 3F * bu;
                root.rotationPointX = MathHelper.sin(ageInTicks * 2.5F) * 0.5F * cr * cr;
                torso.rotateAngleX = 0.5F * cr - 0.3F * bu;
                head.rotateAngleX += 0.3F * cr - 0.5F * bu;
                armR.rotateAngleX = -0.9F * cr;
                armL.rotateAngleX = -0.9F * cr;
                armR.rotateAngleZ = 0.2F * cr + 2.2F * bu;
                armL.rotateAngleZ = -armR.rotateAngleZ;
                legR.rotateAngleX = -0.4F * cr;
                legL.rotateAngleX = -0.4F * cr;
                break;
            }
            case IColossoMagmaAnimated.DEATH: {
                // esfria e desaba: o brilho apaga e sobra só a rocha escura
                float k = ease(c / 1.0F);
                head.rotateAngleX += 0.7F * k;
                torso.rotateAngleX = 0.4F * k;
                armR.rotateAngleX = 0.35F * k;
                armL.rotateAngleX = 0.35F * k;
                armR.rotateAngleZ = 0.25F * k;
                armL.rotateAngleZ = -0.25F * k;
                legR.rotateAngleX = -0.35F * k;
                legL.rotateAngleX = -0.35F * k;
                root.rotationPointY += 2F * k;
                glow = 1F - k;
                break;
            }
            default:
                break;
        }
    }

    /** Camada de brilho (lava, olhos e núcleo) que ignora a iluminação do mundo. */
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
            if (e.isInvisible()) return;
            ModelColossoMagma m = (ModelColossoMagma) renderer.getMainModel();
            renderer.bindTexture(m.enraged ? rage : calm);
            float pulse = m.glow * (0.8F + 0.2F * MathHelper.sin(ageInTicks * (m.enraged ? 0.35F : 0.2F)));
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(!e.isInvisible());
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
            GlStateManager.color(pulse, pulse, pulse, 1F);
            m.render(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            int br = e.getBrightnessForRender();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, br % 65536, br / 65536);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
