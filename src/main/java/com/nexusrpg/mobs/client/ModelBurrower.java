package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
/** Six-legged armored beetle, with split mandibles and antennae. */
@SideOnly(Side.CLIENT)
public final class ModelBurrower extends ModelBase {
    private final ModelRenderer shell, head, leftJaw, rightJaw, leftAntenna, rightAntenna;
    private final ModelRenderer[] legs = new ModelRenderer[6];
    public ModelBurrower() {
        textureWidth=64; textureHeight=64;
        shell=new ModelRenderer(this,0,0); shell.addBox(-6,-4,-5,12,6,10); shell.setRotationPoint(0,18,1);
        shell.setTextureOffset(0,18).addBox(-5,-6,-4,10,2,8);
        head=new ModelRenderer(this,0,30); head.addBox(-4,-3,-4,8,5,4); head.setRotationPoint(0,19,-4);
        leftJaw=new ModelRenderer(this,26,30); leftJaw.addBox(-1,-1,-5,2,2,5); leftJaw.setRotationPoint(-3,20,-8);
        rightJaw=new ModelRenderer(this,26,30); rightJaw.addBox(-1,-1,-5,2,2,5); rightJaw.setRotationPoint(3,20,-8);
        leftAntenna=new ModelRenderer(this,42,30); leftAntenna.addBox(-1,-4,-1,2,4,2); leftAntenna.setRotationPoint(-3,16,-6);
        rightAntenna=new ModelRenderer(this,42,30); rightAntenna.addBox(-1,-4,-1,2,4,2); rightAntenna.setRotationPoint(3,16,-6);
        for(int i=0;i<6;i++) { int side=i%2==0?-1:1;
            legs[i]=new ModelRenderer(this,0,44); legs[i].addBox(side<0?-5:0,0,-1,5,2,2);
            legs[i].setTextureOffset(16,44).addBox(side<0?-5:3,1,-1,2,4,2);
            legs[i].setRotationPoint(side*5,19,-3+(i/2)*4);
        }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e);
        shell.render(scale); head.render(scale); leftJaw.render(scale); rightJaw.render(scale);
        leftAntenna.render(scale); rightAntenna.render(scale); for(ModelRenderer leg:legs) leg.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-20F,20F)*0.017453292F;
        head.rotateAngleX=MathHelper.clamp(pitch,-10F,10F)*0.017453292F;
        for(int i=0;i<6;i++) legs[i].rotateAngleY=MathHelper.cos(swing*0.6662F+(((i+i/2)%2)*3.1415927F))*0.45F*amount;
        leftJaw.rotateAngleY=0.15F+MathHelper.cos(age*0.1F)*0.06F; rightJaw.rotateAngleY=-leftJaw.rotateAngleY;
        leftAntenna.rotateAngleZ=0.2F+MathHelper.cos(age*0.08F)*0.05F; rightAntenna.rotateAngleZ=-leftAntenna.rotateAngleZ;
    }
}
