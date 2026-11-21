package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
/** A low, six-legged crawler; legs articulate from a wide carapace. */
public final class ModelCavern extends ModelBase {
    private final ModelRenderer body, head;
    private final ModelRenderer[] legs = new ModelRenderer[6];
    public ModelCavern() {
        textureWidth=64; textureHeight=64;
        body=new ModelRenderer(this,0,0); body.addBox(-5,-3,-6,10,6,12); body.setRotationPoint(0,18,0);
        body.setTextureOffset(0,20).addBox(-6,-5,-3,12,2,8);
        head=new ModelRenderer(this,0,32); head.addBox(-4,-2,-5,8,4,5); head.setRotationPoint(0,19,-6);
        head.setTextureOffset(48,48).addBox(-3,-1,-5.1F,2,1,1); head.setTextureOffset(48,48).addBox(1,-1,-5.1F,2,1,1);
        head.setTextureOffset(32,32).addBox(-4,1,-8,2,1,4); head.setTextureOffset(32,32).addBox(2,1,-8,2,1,4);
        for(int i=0;i<6;i++) { int side=i%2==0?-1:1; legs[i]=new ModelRenderer(this,32,40); legs[i].setRotationPoint(side*4,19,-4+(i/2)*4);
            legs[i].addBox(side<0?-8:0,-1,-1,8,2,2); legs[i].setTextureOffset(52,32).addBox(side<0?-8:6,0,-1,2,5,2); }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e); body.render(scale); head.render(scale); for(ModelRenderer leg:legs) leg.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-35,35)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-20,20)*0.017453292F;
        for(int i=0;i<6;i++) { float phase=swing*0.8F+((i%2+i/2)%2)*3.14159F; legs[i].rotateAngleY=MathHelper.cos(phase)*0.3F*amount; legs[i].rotateAngleZ=(i%2==0?-1:1)*MathHelper.sin(phase)*0.2F*amount; }
    }
}
