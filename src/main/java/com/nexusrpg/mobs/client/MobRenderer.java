package com.nexusrpg.mobs.client;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
@SideOnly(Side.CLIENT)
public final class MobRenderer<T extends EntityLiving> extends RenderLiving<T> {
    private final ResourceLocation texture;
    public MobRenderer(RenderManager manager, ModelBase model, String name) {
        super(manager, model, 0.6F);
        texture = new ResourceLocation("nexusrpg", "textures/entity/mobs/" + name + ".png");
    }
    @Override protected ResourceLocation getEntityTexture(T entity) { return texture; }
}
