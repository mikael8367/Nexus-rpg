package com.nexusrpg.mobs;
import net.minecraft.world.World;
public final class EntityBramble extends EntityNexusMob {
    public EntityBramble(World world) { super(world); setSize(1.1F, 1.0F); }
    @Override protected double health() { return 28D; }
    @Override protected double speed() { return 0.29D; }
    @Override protected double damage() { return 5D; }
}
