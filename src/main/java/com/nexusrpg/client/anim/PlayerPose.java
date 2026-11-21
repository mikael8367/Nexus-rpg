package com.nexusrpg.client.anim;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

/**
 * Poses das animações, no espaço do ModelBiped (y para baixo, frente em -z).
 * Convenções: braço/perna direitos giram para fora com rotateAngleZ positivo; rotateAngleX negativo levanta para frente.
 * Cada parte é {rotX, rotY, rotZ, pontoX, pontoY, pontoZ}.
 */
public final class PlayerPose {
    private static final int HEAD = 0, BODY = 1, ARM_R = 2, ARM_L = 3, LEG_R = 4, LEG_L = 5;
    private static final float HIP = 12F;

    private PlayerPose() {}

    public static float ease(float x) {
        x = MathHelper.clamp(x, 0F, 1F);
        return x * x * (3F - 2F * x);
    }

    /** Reverte pontos móveis antes de reutilizar o modelo para outro jogador. */
    public static void resetPoints(ModelBiped m, float armY) {
        m.bipedHead.setRotationPoint(0F, 0F, 0F);
        m.bipedBody.setRotationPoint(0F, 0F, 0F);
        m.bipedRightArm.setRotationPoint(-5F, armY, 0F);
        m.bipedLeftArm.setRotationPoint(5F, armY, 0F);
        m.bipedRightLeg.setRotationPoint(-1.9F, 12F, 0.1F);
        m.bipedLeftLeg.setRotationPoint(1.9F, 12F, 0.1F);
    }

    /** Mistura a pose da animação com a pose normal do jogo (andar, agachar etc.). */
    public static void apply(ModelBiped m, PlayerAnim anim, float t) {
        float armY = m.bipedRightArm.rotationPointY;
        float[][] p = {
            {0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0},
            {0, 0, 0, -5F, armY, 0}, {0, 0, 0, 5F, armY, 0},
            {0, 0, 0, -1.9F, 12F, 0.1F}, {0, 0, 0, 1.9F, 12F, 0.1F}
        };
        float dur = anim.seconds();
        float w = dur > 0F ? ease(t / 0.08F) * (1F - ease((t - dur + 0.12F) / 0.12F)) : ease(t / 0.12F);
        switch (anim) {
            case SLASH: slash(p, t); break;
            case OVERHEAD: overhead(p, t); break;
            case THRUST: thrust(p, t); break;
            case ROLL: roll(p, t); break;
            case GUARD: guard(p, t); break;
            case DRINK: drink(p, t); break;
            case STUN: stun(p, t); break;
            case VICTORY: victory(p, t); break;
            default: break;
        }
        ModelRenderer[] r = {m.bipedHead, m.bipedBody, m.bipedRightArm, m.bipedLeftArm, m.bipedRightLeg, m.bipedLeftLeg};
        for (int i = 0; i < 6; i++) {
            ModelRenderer o = r[i];
            float[] q = p[i];
            o.rotateAngleX += (q[0] - o.rotateAngleX) * w;
            o.rotateAngleY += (q[1] - o.rotateAngleY) * w;
            o.rotateAngleZ += (q[2] - o.rotateAngleZ) * w;
            o.rotationPointX += (q[3] - o.rotationPointX) * w;
            o.rotationPointY += (q[4] - o.rotationPointY) * w;
            o.rotationPointZ += (q[5] - o.rotationPointZ) * w;
        }
    }

    /** Inclina (lean, eixo x) e gira (twist, eixo y) cabeça, corpo e braços em volta do quadril. */
    private static void torso(float[][] p, float lean, float twist) {
        float cl = MathHelper.cos(lean), sl = MathHelper.sin(lean), ct = MathHelper.cos(twist), st = MathHelper.sin(twist);
        for (int i = HEAD; i <= ARM_L; i++) {
            float x = p[i][3], y = p[i][4] - HIP, z = p[i][5];
            float y1 = y * cl - z * sl, z1 = y * sl + z * cl;
            p[i][3] = x * ct + z1 * st;
            p[i][4] = y1 + HIP;
            p[i][5] = -x * st + z1 * ct;
            p[i][0] += lean;
            p[i][1] += twist;
        }
    }

    private static void slash(float[][] p, float t) {
        float k = ease(t / 0.25F), s = ease((t - 0.25F) / 0.15F);
        float twist = 0.4F * k - 0.9F * s;
        p[ARM_R][0] = -1.5F + 0.15F * s;
        p[ARM_R][1] = k - 1.9F * s;
        p[ARM_L][0] = 0.3F * k;
        p[ARM_L][2] = -0.4F * k;
        p[LEG_R][0] = 0.3F * k;
        p[LEG_L][0] = -0.4F * k;
        p[HEAD][1] = -0.6F * twist;
        torso(p, 0.1F * s, twist);
    }

    private static void overhead(float[][] p, float t) {
        float k = ease(t / 0.45F), s = ease((t - 0.45F) / 0.13F);
        float lean = -0.3F * k + 0.7F * s;
        float arms = -2.8F * k + 2.2F * s;
        p[ARM_R][0] = arms;
        p[ARM_L][0] = arms;
        p[ARM_R][2] = -0.15F * k;
        p[ARM_L][2] = 0.15F * k;
        p[LEG_R][0] = 0.3F * (k - 0.5F * s);
        p[LEG_L][0] = -0.3F * (k - 0.5F * s);
        p[HEAD][0] = -0.6F * lean;
        torso(p, lean, 0F);
    }

    private static void thrust(float[][] p, float t) {
        float k = ease(t / 0.2F), s = ease((t - 0.2F) / 0.12F);
        float twist = 0.45F * k - 1.0F * s;
        p[ARM_R][0] = -0.7F * k - 0.87F * s;
        p[ARM_R][1] = 0.4F * k - 0.5F * s;
        p[ARM_L][0] = 0.5F * k;
        p[ARM_L][2] = -0.7F * k;
        p[LEG_R][0] = 0.7F * k;
        p[LEG_L][0] = -0.8F * k;
        p[HEAD][1] = -0.5F * twist;
        torso(p, 0.25F * s, twist);
    }

    private static void roll(float[][] p, float t) {
        float k = ease(t / 0.2F) * (1F - ease((t - 0.5F) / 0.2F));
        p[ARM_R][0] = -1.4F * k;
        p[ARM_L][0] = -1.4F * k;
        p[ARM_R][2] = -0.5F * k;
        p[ARM_L][2] = 0.5F * k;
        p[LEG_R][0] = -1.3F * k;
        p[LEG_L][0] = -1.3F * k;
        p[HEAD][0] = 0.2F * k;
        torso(p, 0.5F * k, 0F);
    }

    private static void guard(float[][] p, float t) {
        float br = MathHelper.sin(t * 3F) * 0.03F;
        p[ARM_R][0] = -1.35F + br;
        p[ARM_L][0] = -1.35F + br;
        p[ARM_R][2] = -0.6F;
        p[ARM_L][2] = 0.6F;
        p[LEG_R][0] = 0.3F;
        p[LEG_L][0] = -0.35F;
        p[HEAD][0] = 0.1F;
        torso(p, -0.1F, 0F);
    }

    private static void drink(float[][] p, float t) {
        float k = ease(t / 0.4F) * (1F - ease((t - 1.1F) / 0.4F));
        float tilt = ease((t - 0.4F) / 0.2F) * (1F - ease((t - 1F) / 0.2F));
        p[ARM_R][0] = -2.55F * k + MathHelper.sin(t * 14F) * 0.02F * tilt;
        p[ARM_R][2] = -0.35F * k;
        p[HEAD][0] = -0.4F * tilt;
        torso(p, -0.12F * tilt, 0F);
    }

    private static void stun(float[][] p, float t) {
        float o = MathHelper.sin(t * 7F), c = MathHelper.cos(t * 9F);
        p[HEAD][0] = 0.35F + 0.12F * c;
        p[HEAD][2] = 0.3F * o;
        p[ARM_R][0] = 0.15F + 0.2F * o;
        p[ARM_L][0] = 0.15F - 0.2F * o;
        p[ARM_R][2] = 0.15F;
        p[ARM_L][2] = -0.15F;
        p[LEG_R][0] = 0.2F * o;
        p[LEG_L][0] = -0.2F * o;
        torso(p, 0.3F + 0.05F * o, 0.25F * MathHelper.sin(t * 3.5F));
    }

    private static void victory(float[][] p, float t) {
        float pump = MathHelper.sin(t * 12F) * 0.15F;
        p[ARM_R][0] = -0.15F;
        p[ARM_L][0] = -0.15F;
        p[ARM_R][2] = 2.5F + pump;
        p[ARM_L][2] = -(2.5F + pump);
        p[LEG_R][2] = 0.15F;
        p[LEG_L][2] = -0.15F;
        p[HEAD][0] = -0.3F;
        torso(p, -0.12F, 0F);
    }
}
