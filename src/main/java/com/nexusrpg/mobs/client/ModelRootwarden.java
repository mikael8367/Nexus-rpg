package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
/** Upright wooden guardian with forked crown, long arms and root feet. */
@SideOnly(Side.CLIENT)
public final class ModelRootwarden extends ModelBase {
    private final ModelRenderer trunk, head, crownLeft, crownRight;
    private final ModelRenderer[] legs = new ModelRenderer[2], arms = new ModelRenderer[2];
    public ModelRootwarden() {
        textureWidth=64; textureHeight=64;
        trunk=new ModelRenderer(this,0,0); trunk.addBox(-4,-8,-3,8,12,6); trunk.setRotationPoint(0,10,0);
        trunk.setTextureOffset(30,0).addBox(-2,-6,-4,4,5,1);
        head=new ModelRenderer(this,0,20); head.addBox(-3,-5,-3,6,5,6); head.setRotationPoint(0,2,0);
        crownLeft=new ModelRenderer(this,26,20); crownLeft.addBox(-1,-6,-1,2,6,2); crownLeft.setRotationPoint(-2,-3,0);
        crownRight=new ModelRenderer(this,26,20); crownRight.addBox(-1,-8,-1,2,8,2); crownRight.setRotationPoint(2,-3,0);
        for(int i=0;i<2;i++) { int side=i==0?-1:1;
            legs[i]=new ModelRenderer(this,0,36); legs[i].addBox(-2,0,-2,4,10,4); legs[i].setTextureOffset(18,36).addBox(-2,9,-4,4,1,6); legs[i].setRotationPoint(side*2,14,0);
            arms[i]=new ModelRenderer(this,34,36); arms[i].addBox(-2,0,-2,4,11,4); arms[i].setTextureOffset(50,36).addBox(side<0?-4:2,7,-1,2,4,2); arms[i].setRotationPoint(side*6,4,0);
        }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e);
        trunk.render(scale); head.render(scale); crownLeft.render(scale); crownRight.render(scale);
        for(ModelRenderer leg:legs) leg.render(scale); for(ModelRenderer arm:arms) arm.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY=MathHelper.clamp(yaw,-25F,25F)*0.017453292F; head.rotateAngleX=MathHelper.clamp(pitch,-15F,15F)*0.017453292F;
        float step=MathHelper.cos(swing*0.6662F)*0.6F*amount; legs[0].rotateAngleX=step; legs[1].rotateAngleX=-step;
        arms[0].rotateAngleX=-step*0.7F; arms[1].rotateAngleX=step*0.7F;
        arms[0].rotateAngleZ=0.08F+MathHelper.cos(age*0.08F)*0.03F; arms[1].rotateAngleZ=-arms[0].rotateAngleZ;
        crownLeft.rotateAngleZ=-0.25F; crownRight.rotateAngleZ=0.25F;
    }
}
