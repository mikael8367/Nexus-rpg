package com.nexusrpg.dragons.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Large quadrupedal dragon: dimensions are in model pixels (16 per block). */
@SideOnly(Side.CLIENT)
public final class DragonModel extends ModelBase {
    public enum Kind {
        FIRE("fire"), WATER("water"), WIND("wind"), EARTH("earth");
        final String textureName;
        Kind(String name) { textureName = name; }
    }

    private final ModelRenderer body, head, tail, tailTip, wingLeft, wingRight;
    private final ModelRenderer[] legs = new ModelRenderer[4];

    public DragonModel(Kind kind) {
        textureWidth = 256;
        textureHeight = 128;
        body = part(null, 0, 4, 0);
        box(body, 0, 0, -9, -8, -14, 18, 14, 29);
        // Armoured chest and throat.
        box(body, 96, 32, -7, 4, -13, 14, 3, 24);
        box(body, 96, 0, -6, -8, -23, 12, 10, 12);
        head = part(body, 0, -5, -23);
        box(head, 96, 0, -6, -6, -10, 12, 10, 12);
        box(head, 96, 24, -4, -2, -18, 8, 5, 9);
        // Eyes, brow, lower jaw, horns and cheek spines.
        box(head, 220, 70, -6.5F, -3, -7, 1, 2, 3);
        box(head, 220, 70, 5.5F, -3, -7, 1, 2, 3);
        box(head, 150, 0, -5, 3, -16, 10, 2, 8);
        for (int side : new int[] {-1, 1}) {
            ModelRenderer horn = part(head, side * 4, -5, 0);
            horn.rotateAngleX = -0.55F;
            horn.rotateAngleZ = side * 0.25F;
            box(horn, 192, 0, -1.5F, -9, -1.5F, 3, 10, 3);
            box(head, 192, 16, side < 0 ? -9 : 6, 0, -2, 3, 3, 5);
        }
        for (int i = 0; i < 4; i++) {
            int side = (i % 2 == 0) ? -1 : 1;
            int z = i < 2 ? -9 : 9;
            ModelRenderer leg = part(body, side * 7, 5, z);
            legs[i] = leg;
            box(leg, 150, 0, -3, 0, -3, 6, 12, 6);
            box(leg, 150, 20, -4, 10, -6, 8, 5, 10);
            for (int toe = 0; toe < 3; toe++)
                box(leg, 192, 0, -3 + toe * 2.5F, 13, -8, 2, 2, 3);
        }
        tail = part(body, 0, -2, 14);
        box(tail, 0, 45, -5, -4, 0, 10, 9, 17);
        tailTip = part(tail, 0, 0, 16);
        box(tailTip, 60, 45, -3, -3, 0, 6, 6, 17);
        box(tailTip, 192, 24, -4, -1, 15, 8, 3, 5);
        // Thin but nonzero-depth membranes make both wing faces render correctly.
        wingLeft = wing(body, 1);
        wingRight = wing(body, -1);
        // Back plates; element-specific silhouettes, not just a recolour.
        for (int z = -9; z <= 15; z += 6) {
            int height = kind == Kind.EARTH ? 7 : kind == Kind.WATER ? 6 : 5;
            box(body, 192, 40, -1.5F, -8 - height, z, 3, height, 4);
        }
        switch (kind) {
            case FIRE:
                // Flame-like jagged crest and tail spikes.
                box(head, 192, 40, -1, -11, -5, 2, 5, 5);
                for (int z = 2; z < 17; z += 5)
                    box(tail, 192, 40, -1, -8, z, 2, 4, 3);
                break;
            case WATER:
                // Gill fins and a broad fish-like tail fin.
                box(head, 192, 48, -9, -4, 0, 3, 6, 8);
                box(head, 192, 48, 6, -4, 0, 3, 6, 8);
                box(tailTip, 192, 48, -1, -9, 11, 2, 15, 9);
                break;
            case WIND:
                // High, narrow crest and extra swept-back head plumes.
                box(head, 192, 40, -1, -14, -1, 2, 8, 5);
                box(head, 192, 40, -8, -7, 1, 2, 3, 10);
                box(head, 192, 40, 6, -7, 1, 2, 3, 10);
                break;
            case EARTH:
                // Heavy stone armour and blunt horns.
                box(body, 96, 32, -10, -10, -9, 20, 3, 19);
                box(head, 192, 16, -7, -8, -5, 14, 3, 6);
                box(tailTip, 150, 20, -5, -5, 12, 10, 10, 8);
                break;
            default: throw new IllegalArgumentException("Unknown dragon kind");
        }
    }

    private ModelRenderer wing(ModelRenderer parent, int side) {
        ModelRenderer wing = part(parent, side * 8, -6, -7);
        float x = side > 0 ? 0 : -28;
        // A broad membrane with projecting, bony leading and trailing spars.
        box(wing, 0, 80, x, 0, -3, 28, 1, 25);
        box(wing, 120, 80, x, -1, -4, 28, 2, 3);
        box(wing, 120, 87, x, -1, 18, 28, 2, 3);
        float outer = side > 0 ? 24 : -28;
        box(wing, 120, 94, outer, -1, -3, 4, 2, 25);
        box(wing, 192, 0, side > 0 ? 27 : -30, -3, -4, 3, 4, 4);
        return wing;
    }

    private ModelRenderer part(ModelRenderer parent, float x, float y, float z) {
        ModelRenderer p = new ModelRenderer(this);
        p.setRotationPoint(x, y, z);
        if (parent != null) parent.addChild(p);
        return p;
    }

    private static void box(ModelRenderer p, int u, int v, float x, float y, float z, int w, int h, int d) {
        p.setTextureOffset(u, v).addBox(x, y, z, w, h, d);
    }

    @Override
    public void render(Entity entity, float swing, float amount, float age, float yaw, float pitch, float scale) {
        setRotationAngles(swing, amount, age, yaw, pitch, scale, entity);
        body.render(scale);
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0F;
    }

    // Use double for phase multiplication, so even extreme finite inputs do not overflow.
    private static float wave(float time, double speed, double offset) {
        return (float) Math.sin((double) time * speed + offset);
    }

    @Override
    public void setRotationAngles(float swing, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        swing = finiteOrZero(swing);
        age = finiteOrZero(age);
        head.rotateAngleY = MathHelper.clamp(finiteOrZero(yaw), -45F, 45F) * 0.017453292F;
        head.rotateAngleX = MathHelper.clamp(finiteOrZero(pitch), -30F, 30F) * 0.017453292F;
        float walk = MathHelper.clamp(finiteOrZero(amount), 0F, 1F);
        // A passenger can ride the dragon during flight; exclude the dragon riding
        // another entity, not a dragon carrying passengers. This is only a visual
        // airborne approximation: vanilla does not expose a generic flying flag.
        boolean airborne = entity != null && !entity.onGround && !entity.isInWater() && !entity.isRiding();
        float flapCycle = wave(age, 0.45, 0);
        body.rotateAngleX = airborne ? -0.12F + 0.035F * flapCycle : 0F;
        for (int i = 0; i < legs.length; i++) {
            if (airborne) {
                // Hold the feet tucked under the body rather than running in midair.
                legs[i].rotateAngleX = (i < 2 ? -0.55F : 0.45F) + 0.06F * flapCycle;
            } else {
                // Diagonal pairs move together, with no pose leaking between mobs.
                double phase = (i == 0 || i == 3) ? Math.PI / 2 : -Math.PI / 2;
                legs[i].rotateAngleX = wave(swing, 0.6662, phase) * 0.6F * walk;
            }
        }
        tail.rotateAngleY = wave(age, 0.09, 0) * 0.16F;
        tailTip.rotateAngleY = wave(age, 0.09, -0.7) * 0.22F;
        tail.rotateAngleX = airborne ? 0.10F + 0.04F * flapCycle : 0F;
        tailTip.rotateAngleX = airborne ? 0.06F + 0.04F * wave(age, 0.45, -0.5) : 0F;
        // Mirrored shoulder rotations alternate an upstroke and a downstroke.
        float flap = airborne ? 0.05F + 0.65F * flapCycle
                              : 0.12F + 0.08F * wave(age, 0.12, 0);
        wingLeft.rotateAngleZ = flap;
        wingRight.rotateAngleZ = -flap;
        float sweep = airborne ? 0.16F + 0.09F * wave(age, 0.45, -0.8) : 0.10F;
        wingLeft.rotateAngleY = sweep;
        wingRight.rotateAngleY = -sweep;
    }
}
