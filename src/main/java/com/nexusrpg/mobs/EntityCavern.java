package com.nexusrpg.mobs;
import net.minecraft.world.World;
public final class EntityCavern extends EntityNexusMob {
    public EntityCavern(World world) { super(world); setSize(1.4F, 0.7F); }
    @Override protected double health() { return 20D; }
    @Override protected double speed() { return 0.32D; }
    @Override protected double damage() { return 4D; }
}
