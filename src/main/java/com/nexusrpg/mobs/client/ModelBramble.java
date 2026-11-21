package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
public final class ModelBramble extends ModelBase {
    private final ModelRenderer body, head, tail;
    private final ModelRenderer[] legs = new ModelRenderer[4];
    public ModelBramble() {
        textureWidth = 64; textureHeight = 64;
        body = new ModelRenderer(this, 0, 0); body.addBox(-5, -4, -8, 10, 8, 16); body.setRotationPoint(0, 14, 0);
        head = new ModelRenderer(this, 0, 24); head.addBox(-4, -4, -6, 8, 7, 7); head.setRotationPoint(0, 13, -8);
        head.setTextureOffset(32, 24).addBox(-3, 0, -10, 6, 3, 4);
        head.setTextureOffset(48, 48).addBox(-4.1F, -2, -5, 1, 1, 1);
        head.setTextureOffset(48, 48).addBox(3.1F, -2, -5, 1, 1, 1);
        head.setTextureOffset(32, 32).addBox(-4, -7, -3, 2, 4, 2);
        head.setTextureOffset(32, 32).addBox(2, -7, -3, 2, 4, 2);
        body.setTextureOffset(0, 40).addBox(-1, -7, -5, 2, 3, 11);
        tail = new ModelRenderer(this, 40, 32); tail.addBox(-1, -1, 0, 2, 2, 8); tail.setRotationPoint(0, 12, 8); tail.rotateAngleX = -0.35F;
        for (int i=0; i<4; i++) { legs[i]=new ModelRenderer(this, 32, 40); legs[i].addBox(-1.5F, 0, -1.5F, 3, 6, 3); legs[i].setRotationPoint(i%2==0?-3.5F:3.5F, 18, i<2?-5:5); }
    }
    @Override public void render(Entity e,float swing,float amount,float age,float yaw,float pitch,float scale) {
        setRotationAngles(swing,amount,age,yaw,pitch,scale,e); body.render(scale); head.render(scale); tail.render(scale); for(ModelRenderer leg:legs) leg.render(scale);
    }
    @Override public void setRotationAngles(float swing,float amount,float age,float yaw,float pitch,float scale,Entity e) {
        head.rotateAngleY = MathHelper.clamp(yaw,-45,45)*0.017453292F; head.rotateAngleX = MathHelper.clamp(pitch,-30,30)*0.017453292F;
        for(int i=0;i<4;i++) legs[i].rotateAngleX = MathHelper.cos(swing*0.6662F + ((i==0 || i==3)?0F:3.14159F))*0.8F*amount;
        tail.rotateAngleY = MathHelper.cos(age*0.1F)*0.15F;
    }
}
