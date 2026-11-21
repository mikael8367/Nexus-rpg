package com.nexusrpg.mage;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** An original robed guardian: independent limbs, a hood and a stepped crown. */
@SideOnly(Side.CLIENT)
public final class ModelTowerMage extends ModelBase {
    private final ModelRenderer head, body, rightArm, leftArm, rightLeg, leftLeg;

    public ModelTowerMage() {
        textureWidth = 64;
        textureHeight = 64;
        head = new ModelRenderer(this, 0, 0);
        head.addBox(-4, -8, -4, 8, 8, 8);
        // Hood/crown follows the head rather than remaining fixed when looking around.
        ModelRenderer brim = new ModelRenderer(this, 0, 32);
        brim.addBox(-5, -9, -5, 10, 2, 10);
        head.addChild(brim);
        ModelRenderer crown = new ModelRenderer(this, 0, 44);
        crown.addBox(-3, -12, -3, 6, 4, 6);
        head.addChild(crown);
        body = new ModelRenderer(this, 32, 0);
        body.addBox(-4, 0, -2, 8, 12, 4);
        rightArm = new ModelRenderer(this, 16, 16);
        rightArm.addBox(-3, -2, -2, 4, 12, 4);
        rightArm.setRotationPoint(-5, 2, 0);
        leftArm = new ModelRenderer(this, 16, 16);
        leftArm.mirror = true;
        leftArm.addBox(-1, -2, -2, 4, 12, 4);
        leftArm.setRotationPoint(5, 2, 0);
        rightLeg = new ModelRenderer(this, 0, 16);
        rightLeg.addBox(-2, 0, -2, 4, 12, 4);
        rightLeg.setRotationPoint(-2, 12, 0);
        leftLeg = new ModelRenderer(this, 0, 16);
        leftLeg.mirror = true;
        leftLeg.addBox(-2, 0, -2, 4, 12, 4);
        leftLeg.setRotationPoint(2, 12, 0);
    }

    @Override
    public void render(Entity entity, float swing, float amount, float age, float yaw, float pitch, float scale) {
        setRotationAngles(swing, amount, age, yaw, pitch, scale, entity);
        head.render(scale);
        body.render(scale);
        rightArm.render(scale);
        leftArm.render(scale);
        rightLeg.render(scale);
        leftLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float swing, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        head.rotateAngleY = MathHelper.clamp(yaw, -60.0F, 60.0F) * 0.017453292F;
        head.rotateAngleX = MathHelper.clamp(pitch, -45.0F, 45.0F) * 0.017453292F;
        float step = MathHelper.cos(swing * 0.6662F) * 1.0F * amount;
        rightLeg.rotateAngleX = step;
        leftLeg.rotateAngleX = -step;
        float idle = MathHelper.cos(age * 0.09F) * 0.04F;
        rightArm.rotateAngleX = -step * 0.65F + idle;
        leftArm.rotateAngleX = step * 0.65F - idle;
        rightArm.rotateAngleZ = 0.05F + idle;
        leftArm.rotateAngleZ = -0.05F - idle;
        // The server sends a short visual-only event after a successful spell.
        if (entity instanceof EntityTowerMage) {
            int ticks = ((EntityTowerMage) entity).getCastAnimationTicks();
            if (ticks > 0) {
                float cast = MathHelper.sin((12 - ticks) * (float) Math.PI / 12.0F);
                rightArm.rotateAngleX = rightArm.rotateAngleX * (1.0F - cast) - 1.8F * cast;
                leftArm.rotateAngleX = leftArm.rotateAngleX * (1.0F - cast) - 1.35F * cast;
                rightArm.rotateAngleZ += 0.25F * cast;
                leftArm.rotateAngleZ -= 0.25F * cast;
            }
        }
    }
}
