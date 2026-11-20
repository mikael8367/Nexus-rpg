package com.nexusrpg.armor;

import com.nexusrpg.client.GearArmorModel;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class GearArmor extends ItemArmor {
    private final String tier;

    public GearArmor(ArmorMaterial material, EntityEquipmentSlot slot, String tier) {
        super(material, 0, slot);
        this.tier = tier;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return "nexusrpg:textures/models/armor/" + tier + "_layer_" + (slot == EntityEquipmentSlot.LEGS ? "2" : "1") + ".png";
    }

    /** Forge removes this method on dedicated servers. */
    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack,
            EntityEquipmentSlot slot, ModelBiped original) {
        return GearArmorModel.forArmor(tier, slot, original);
    }
}
