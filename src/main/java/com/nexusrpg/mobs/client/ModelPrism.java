package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
/** Low four-legged creature with layered shell and three crystalline plates. */
@SideOnly(Side.CLIENT)
public final class ModelPrism extends ModelBase {
    private final ModelRenderer shell, head, tail;
    private final ModelRenderer[] legs = new ModelRenderer[4];
    public ModelPrism() {
        textureWidth=64; textureHeight=64;
        shell=new ModelRenderer(this,0,0); shell.addBox(-6,-4,-7,12,7,14); shell.setRotationPoint(0,15,0);
        shell.setTextureOffset(0,22).addBox(-5,-6,-6,10,2,12);
        shell.setTextureOffset(46,22).addBox(-1,-10,-4,2,4,3);
        shell.setTextureOffset(46,30).addBox(-4,-9,0,2,3,3);
        shell.setTextureOffset(46,30).addBox(2,-9,0,2,3,3);
        head=new ModelRenderer(this,0,38); head.addBox(-3,-2,-5,6,4,5); head.setRotationPoint(0,17,-7);
        head.setTextureOffset(24,38).addBox(-2,0,-7,4,2,2);
        tail=new ModelRenderer(this,24,46); tail.addBox(-1,-1,0,2,2,6); tail.setRotationPoint(0,17,7);
        for(int i=0;i<4;i++) { legs[i]=new ModelRenderer(this,40,46); legs[i].addBox(-2,0,-2,4,5,4); legs[i].setRotationPoint(i%2==0?-5:5,19,i<2?-4:4); }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e); shell.render(scale); head.render(scale); tail.render(scale); for(ModelRenderer p:legs)p.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-25,25)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-15,15)*0.017453292F;
        for(int i=0;i<4;i++)legs[i].rotateAngleX=MathHelper.cos(swing*0.6662F+((i==0||i==3)?0F:3.14159F))*0.55F*amount;
        tail.rotateAngleY=MathHelper.cos(age*0.08F)*0.12F;
    }
}
