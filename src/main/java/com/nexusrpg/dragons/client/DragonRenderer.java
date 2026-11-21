package com.nexusrpg.dragons.client;

import com.nexusrpg.dragons.EntityElementalDragon;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class DragonRenderer<T extends EntityElementalDragon> extends RenderLiving<T> {
    private final ResourceLocation texture;

    public DragonRenderer(RenderManager manager, DragonModel.Kind kind) {
        super(manager, new DragonModel(kind), 1.5F);
        texture = new ResourceLocation("nexusrpg", "textures/entity/dragons/" + kind.textureName + ".png");
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return texture;
    }
}
