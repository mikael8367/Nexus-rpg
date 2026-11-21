package com.nexusrpg.dragons;
import net.minecraft.world.World;
public final class EntityWindDragon extends EntityElementalDragon {
    public EntityWindDragon(World world) { super(world); }
    @Override public Element getElement() { return Element.WIND; }
    @Override public void fall(float distance, float damageMultiplier) { }
}
