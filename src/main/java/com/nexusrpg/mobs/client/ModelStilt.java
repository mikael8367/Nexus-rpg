package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
/** Tall, long-necked ground bird with two wing-fins and a fan tail. */
@SideOnly(Side.CLIENT)
public final class ModelStilt extends ModelBase {
    private final ModelRenderer body, neck, head, tail;
    private final ModelRenderer[] legs = new ModelRenderer[2], wings = new ModelRenderer[2];
    public ModelStilt() {
        textureWidth=64; textureHeight=64;
        body=new ModelRenderer(this,0,0); body.addBox(-4,-4,-6,8,8,12); body.setRotationPoint(0,8,0);
        neck=new ModelRenderer(this,0,22); neck.addBox(-2,-10,-2,4,11,4); neck.setRotationPoint(0,6,-4);
        head=new ModelRenderer(this,20,22); head.addBox(-3,-4,-3,6,5,5); head.setRotationPoint(0,-4,-4);
        head.setTextureOffset(42,22).addBox(-1,-2,-8,2,2,5);
        head.setTextureOffset(42,30).addBox(-1,-7,0,2,3,2);
        tail=new ModelRenderer(this,0,42); tail.addBox(-4,-1,0,8,2,7); tail.setRotationPoint(0,6,5);
        for(int i=0;i<2;i++) { int side=i==0?-1:1;
            legs[i]=new ModelRenderer(this,32,40); legs[i].addBox(-1,0,-1,2,12,2); legs[i].setTextureOffset(40,40).addBox(-1,11,-3,2,1,4); legs[i].setRotationPoint(side*2,12,1);
            wings[i]=new ModelRenderer(this,0,52); wings[i].addBox(side<0?-5:0,0,-4,5,1,8); wings[i].setRotationPoint(side*4,6,0);
        }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e); body.render(scale); neck.render(scale); head.render(scale); tail.render(scale); for(ModelRenderer p:legs)p.render(scale); for(ModelRenderer p:wings)p.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-30,30)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-20,20)*0.017453292F;
        float step=MathHelper.cos(swing*0.6662F)*0.7F*amount; legs[0].rotateAngleX=step; legs[1].rotateAngleX=-step;
        wings[0].rotateAngleZ=0.1F+MathHelper.cos(age*0.1F)*0.05F; wings[1].rotateAngleZ=-wings[0].rotateAngleZ; tail.rotateAngleY=MathHelper.cos(age*0.1F)*0.1F;
    }
}
