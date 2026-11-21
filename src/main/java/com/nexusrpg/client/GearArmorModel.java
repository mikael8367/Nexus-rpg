package com.nexusrpg.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Modelos Biped vanilla com relevos filhos dos membros correspondentes. */
@SideOnly(Side.CLIENT)
public final class GearArmorModel extends ModelBiped {
    private static final Map<String, GearArmorModel> MODELS = new HashMap<>();
    private final int reliefU;
    private final int reliefV;

    private GearArmorModel(String tier, EntityEquipmentSlot slot) {
        super(slot == EntityEquipmentSlot.LEGS ? 0.5F : 1.0F);
        // Amostra regiões opacas das texturas vanilla 64x32 para cada camada.
        reliefU = slot == EntityEquipmentSlot.CHEST || slot == EntityEquipmentSlot.FEET ? 16 : 0;
        reliefV = slot == EntityEquipmentSlot.HEAD ? 8 : 20;
        int grade = "draconite".equals(tier) ? 3 : "adamantite".equals(tier) ? 2 : 1;
        if (slot == EntityEquipmentSlot.HEAD) {
            // Sobrancelha em relevo; adornos maiores em cada material.
            add(bipedHead, -4, -7, -5.2F, 8, 2, 1);
            add(bipedHead, -4.5F, -8 - grade, -1, 2, grade + 1, 2);
            add(bipedHead, 2.5F, -8 - grade, -1, 2, grade + 1, 2);
            if (grade >= 2) add(bipedHead, -1, -9 - grade, -3, 2, grade + 2, 2);
            if (grade == 3) add(bipedHead, -3, -5, -5.6F, 6, 1, 1);
        } else if (slot == EntityEquipmentSlot.CHEST) {
            add(bipedBody, -3, 2, -3.2F, 6, 6, 1);
            add(bipedRightArm, -4.7F, -2.8F, -2.8F, 5, 2 + grade, 5);
            add(bipedLeftArm, -0.3F, -2.8F, -2.8F, 5, 2 + grade, 5);
            if (grade >= 2) add(bipedBody, -1, 0, -3.6F, 2, 10, 1);
            if (grade == 3) {
                add(bipedRightArm, -4.4F, -4.8F, -1, 2, 3, 2);
                add(bipedLeftArm, 2.4F, -4.8F, -1, 2, 3, 2);
            }
        } else if (slot == EntityEquipmentSlot.LEGS) {
            add(bipedBody, -4, 8, -2.7F, 8, 3, 1);
            add(bipedRightLeg, -2, 3, -2.8F, 4, 2 + grade, 1);
            add(bipedLeftLeg, -2, 3, -2.8F, 4, 2 + grade, 1);
            if (grade >= 2) {
                add(bipedRightLeg, -2, 8, -2.6F, 4, 2, 1);
                add(bipedLeftLeg, -2, 8, -2.6F, 4, 2, 1);
            }
        } else if (slot == EntityEquipmentSlot.FEET) {
            add(bipedRightLeg, -2, 8, -3.2F, 4, 3, 1);
            add(bipedLeftLeg, -2, 8, -3.2F, 4, 3, 1);
            if (grade >= 2) {
                add(bipedRightLeg, -2, 6, -2.8F, 4, 2, 1);
                add(bipedLeftLeg, -2, 6, -2.8F, 4, 2, 1);
            }
        }
        setVisible(false);
        if (slot == EntityEquipmentSlot.HEAD) bipedHead.showModel = true;
        if (slot == EntityEquipmentSlot.CHEST) {
            bipedBody.showModel = true;
            bipedRightArm.showModel = true;
            bipedLeftArm.showModel = true;
        }
        if (slot == EntityEquipmentSlot.LEGS) {
            bipedBody.showModel = true;
            bipedRightLeg.showModel = true;
            bipedLeftLeg.showModel = true;
        }
        if (slot == EntityEquipmentSlot.FEET) {
            bipedRightLeg.showModel = true;
            bipedLeftLeg.showModel = true;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scaleFactor, net.minecraft.entity.Entity entity) {
        com.nexusrpg.client.anim.PlayerPose.resetPoints(this, 2F);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
        com.nexusrpg.client.anim.PlayerAnim anim = com.nexusrpg.client.anim.PlayerAnimManager.get(entity, ageInTicks);
        if (anim != null) {
            com.nexusrpg.client.anim.PlayerPose.apply(this, anim, com.nexusrpg.client.anim.PlayerAnimManager.time(entity, ageInTicks));
            copyModelAngles(bipedHead, bipedHeadwear);
        }
    }

    private void add(ModelRenderer parent, float x, float y, float z, int width, int height, int depth) {
        ModelRenderer relief = new ModelRenderer(this, reliefU, reliefV);
        relief.addBox(x, y, z, width, height, depth, 0.0F);
        parent.addChild(relief);
    }

    public static ModelBiped forArmor(String tier, EntityEquipmentSlot slot, ModelBiped original) {
        String key = tier + ":" + slot.getName();
        GearArmorModel model = MODELS.get(key);
        if (model == null) {
            model = new GearArmorModel(tier, slot);
            MODELS.put(key, model);
        }
        // Retém poses de braços, agachamento, montaria e swing do renderizador.
        model.setModelAttributes(original);
        return model;
    }
}
