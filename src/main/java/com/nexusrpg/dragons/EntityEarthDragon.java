package com.nexusrpg.dragons;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;
public final class EntityEarthDragon extends EntityElementalDragon {
    public EntityEarthDragon(World world) { super(world); }
    @Override public Element getElement() { return Element.EARTH; }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(8.0D);
    }
}
