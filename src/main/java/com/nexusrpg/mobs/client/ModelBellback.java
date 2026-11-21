package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
public final class ModelBellback extends ModelBase {
    private final ModelRenderer mantle, shell, face, crest;
    private final ModelRenderer[] legs = new ModelRenderer[4], feelers = new ModelRenderer[2];
    public ModelBellback() {
        textureWidth=64; textureHeight=64;
        mantle=box(0,0,-5,-3,-6,10,6,12,0,18,0);
        shell=box(0,20,-5,-7,-5,10,7,10,0,15,1);
        face=box(40,0,-3,-2,-4,6,4,4,0,18,-6);
        crest=box(40,10,-1,-4,-1,2,4,2,0,8,1);
        for(int i=0;i<4;i++) { int side=i%2==0?-1:1; legs[i]=box(40,20,-2,0,-2,4,3,4,side*4,21,i<2?-4:4); }
        for(int i=0;i<2;i++) { int side=i==0?-1:1; feelers[i]=box(42,31,-1,-5,-1,2,5,2,side*2,16,-8); }
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        face.rotateAngleY=MathHelper.clamp(yaw,-20,20)*0.017453292F;
        for(int i=0;i<4;i++) legs[i].rotateAngleX=MathHelper.cos(swing*0.6662F+((i==0||i==3)?0:3.14159F))*0.4F*amount;
        feelers[0].rotateAngleX=0.15F*MathHelper.cos(age*0.1F); feelers[1].rotateAngleX=-feelers[0].rotateAngleX;
        crest.rotateAngleZ=0.08F*MathHelper.cos(age*0.08F);
    }

    private ModelRenderer box(int u,int v,float x,float y,float z,int w,int h,int d,float px,float py,float pz) {
        ModelRenderer part=new ModelRenderer(this,u,v); part.addBox(x,y,z,w,h,d); part.setRotationPoint(px,py,pz); return part;
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e);
        for(ModelRenderer part : boxList) part.render(scale);
    }
}
