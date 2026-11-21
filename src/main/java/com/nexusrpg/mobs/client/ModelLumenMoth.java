package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
public final class ModelLumenMoth extends ModelBase {
    private final ModelRenderer body, head, antennaA, antennaB;
    private final ModelRenderer[] legs = new ModelRenderer[6], wings = new ModelRenderer[2];
    public ModelLumenMoth() {
        textureWidth=64; textureHeight=64;
        body=box(0,0,-3,-3,-5,6,6,10,0,14,0);
        head=box(0,20,-3,-3,-3,6,6,6,0,14,-5);
        antennaA=box(30,20,-1,-5,-1,2,5,2,-2,11,-6);
        antennaB=box(30,20,-1,-5,-1,2,5,2,2,11,-6);
        for(int i=0;i<6;i++) { int side=i%2==0?-1:1; legs[i]=box(40,20,-1,0,-1,2,7,2,side*3,17,-3+(i/2)*3); }
        wings[0]=box(0,34,-10,-1,-5,10,1,10,-3,12,0);
        wings[1]=box(0,46,0,-1,-5,10,1,10,3,12,0);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-30,30)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-20,20)*0.017453292F;
        for(int i=0;i<6;i++) legs[i].rotateAngleX=MathHelper.cos(swing*0.6662F+(i%2==0?0:3.14159F)+(i/2)*0.5F)*0.55F*amount;
        float flap=0.18F+0.12F*MathHelper.cos(age*0.22F); wings[0].rotateAngleZ=flap; wings[1].rotateAngleZ=-flap;
        antennaA.rotateAngleX=0.1F*MathHelper.cos(age*0.08F); antennaB.rotateAngleX=-antennaA.rotateAngleX;
    }

    private ModelRenderer box(int u,int v,float x,float y,float z,int w,int h,int d,float px,float py,float pz) {
        ModelRenderer part=new ModelRenderer(this,u,v); part.addBox(x,y,z,w,h,d); part.setRotationPoint(px,py,pz); return part;
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e);
        for(ModelRenderer part : boxList) part.render(scale);
    }
}
