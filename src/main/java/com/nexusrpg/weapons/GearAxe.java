package com.nexusrpg.weapons;

import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;

/** O construtor da classe vanilla é protegido; usa suas propriedades e ações normais. */
public final class GearAxe extends ItemAxe {
    public GearAxe(Item.ToolMaterial material) {
        super(material, material.getAttackDamage() + 6.0F, -3.0F);
    }
}
