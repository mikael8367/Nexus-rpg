package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
public final class ModelDuneMole extends ModelBase {
    private final ModelRenderer body, snout, plate, tail;
    private final ModelRenderer[] legs = new ModelRenderer[4], claws = new ModelRenderer[2];
    public ModelDuneMole() {
        textureWidth=64; textureHeight=64;
        body=box(0,0,-5,-4,-6,10,8,12,0,16,0);
        snout=box(0,22,-3,-2,-5,6,5,6,0,17,-6);
        plate=box(26,22,-4,-2,-5,8,2,10,0,12,0);
        tail=box(0,36,-1,-1,0,2,2,6,0,17,6);
        for(int i=0;i<4;i++) { int side=i%2==0?-1:1; legs[i]=box(32,38,-2,0,-2,4,5,4,side*4,19,i<2?-4:4); }
        for(int i=0;i<2;i++) { int side=i==0?-1:1; claws[i]=box(0,46,-2,-1,-3,4,2,4,side*5,22,-6); }
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        snout.rotateAngleY=MathHelper.clamp(yaw,-20,20)*0.017453292F; snout.rotateAngleX=MathHelper.clamp(pitch,-15,15)*0.017453292F;
        for(int i=0;i<4;i++) legs[i].rotateAngleX=MathHelper.cos(swing*0.6662F+((i==0||i==3)?0:3.14159F))*0.6F*amount;
        claws[0].rotateAngleX=legs[0].rotateAngleX; claws[1].rotateAngleX=legs[1].rotateAngleX; tail.rotateAngleY=MathHelper.cos(age*0.09F)*0.15F;
    }

    private ModelRenderer box(int u,int v,float x,float y,float z,int w,int h,int d,float px,float py,float pz) {
        ModelRenderer part=new ModelRenderer(this,u,v); part.addBox(x,y,z,w,h,d); part.setRotationPoint(px,py,pz); return part;
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e);
        for(ModelRenderer part : boxList) part.render(scale);
    }
}
