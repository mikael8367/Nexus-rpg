package com.nexusrpg.dragons;
import net.minecraft.world.World;
public final class EntityFireDragon extends EntityElementalDragon {
    public EntityFireDragon(World world) { super(world); isImmuneToFire = true; }
    @Override public Element getElement() { return Element.FIRE; }
}
