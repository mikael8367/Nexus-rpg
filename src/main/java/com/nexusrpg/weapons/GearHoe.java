package com.nexusrpg.weapons;

import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;

/** Enxada que aceita a barra do seu próprio material para reparo. */
public final class GearHoe extends ItemHoe {
    private final Item repairIngot;

    public GearHoe(Item.ToolMaterial material, Item repairIngot) {
        super(material);
        this.repairIngot = repairIngot;
    }

    @Override
    public boolean getIsRepairable(ItemStack tool, ItemStack repair) {
        return !repair.isEmpty() && repair.getItem() == repairIngot;
    }
}
