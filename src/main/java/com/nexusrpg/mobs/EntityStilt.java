package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Hostile ground creature using standard melee AI, without secondary hit effects. */
public final class EntityStilt extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/reed_stilt");
    public EntityStilt(World world) { super(world); setSize(0.8F, 2.1F); }
    @Override protected double health() { return 30D; }
    @Override protected double speed() { return 0.28D; }
    @Override protected double damage() { return 4D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
