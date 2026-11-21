package com.nexusrpg.dragons;
import net.minecraft.world.World;
public final class EntityWaterDragon extends EntityElementalDragon {
    public EntityWaterDragon(World world) { super(world); }
    @Override public Element getElement() { return Element.WATER; }
    @Override public boolean canBreatheUnderwater() { return true; }
}
