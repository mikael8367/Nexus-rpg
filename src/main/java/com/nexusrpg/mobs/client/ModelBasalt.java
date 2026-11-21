package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
/** Upright, broad-shouldered stone construct with a single central eye. */
public final class ModelBasalt extends ModelBase {
    private final ModelRenderer torso, head, leftArm, rightArm, leftLeg, rightLeg;
    public ModelBasalt() {
        textureWidth=64; textureHeight=64;
        torso=new ModelRenderer(this,0,0); torso.addBox(-6,-9,-3,12,14,6); torso.setRotationPoint(0,9,0);
        torso.setTextureOffset(0,0).addBox(-7,-10,-3,14,3,6);
        head=new ModelRenderer(this,0,24); head.addBox(-4,-6,-3,8,6,6); head.setRotationPoint(0,-1,0);
        head.setTextureOffset(48,48).addBox(-1,-4,-3.1F,2,2,1);
        leftArm=arm(7); rightArm=arm(-7); leftLeg=leg(3); rightLeg=leg(-3);
    }
    private ModelRenderer arm(int x) { ModelRenderer p=new ModelRenderer(this,32,24); p.addBox(-2,-1,-2,4,12,4); p.setRotationPoint(x,0,0); return p; }
    private ModelRenderer leg(int x) { ModelRenderer p=new ModelRenderer(this,0,40); p.addBox(-2,0,-2,4,10,4); p.setRotationPoint(x,14,0); return p; }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e); torso.render(scale); head.render(scale); leftArm.render(scale); rightArm.render(scale); leftLeg.render(scale); rightLeg.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-35,35)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-20,20)*0.017453292F;
        float step=MathHelper.cos(swing*0.6662F)*0.7F*amount;
        leftLeg.rotateAngleX=step; rightLeg.rotateAngleX=-step; leftArm.rotateAngleX=-step; rightArm.rotateAngleX=step;
    }
}
