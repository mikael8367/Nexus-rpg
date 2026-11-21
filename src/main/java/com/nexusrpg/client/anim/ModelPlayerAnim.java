package com.nexusrpg.client.anim;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** ModelPlayer que aplica as animações depois da pose normal do jogo. */
@SideOnly(Side.CLIENT)
public class ModelPlayerAnim extends ModelPlayer {

    private final boolean slim;

    public ModelPlayerAnim(float size, boolean slim) {
        super(size, slim);
        this.slim = slim;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        PlayerPose.resetPoints(this, slim ? 2.5F : 2F);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
        PlayerAnim a = PlayerAnimManager.get(entityIn, ageInTicks);
        if (a == null) return;
        PlayerPose.apply(this, a, PlayerAnimManager.time(entityIn, ageInTicks));
        copyModelAngles(bipedHead, bipedHeadwear);
        copyModelAngles(bipedBody, bipedBodyWear);
        copyModelAngles(bipedLeftArm, bipedLeftArmwear);
        copyModelAngles(bipedRightArm, bipedRightArmwear);
        copyModelAngles(bipedLeftLeg, bipedLeftLegwear);
        copyModelAngles(bipedRightLeg, bipedRightLegwear);
    }

    /** Modelo das peças de armadura: segue a mesma animação do corpo. */
    @SideOnly(Side.CLIENT)
    public static class Armor extends ModelBiped {
        public Armor(float size) {
            super(size);
        }

        @Override
        public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                      float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
            PlayerPose.resetPoints(this, 2F);
            super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
            PlayerAnim a = PlayerAnimManager.get(entityIn, ageInTicks);
            if (a == null) return;
            PlayerPose.apply(this, a, PlayerAnimManager.time(entityIn, ageInTicks));
            copyModelAngles(bipedHead, bipedHeadwear);
        }
    }
}
