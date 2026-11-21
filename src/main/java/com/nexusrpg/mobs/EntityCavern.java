package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
public final class EntityCavern extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/cavern_crawler");
    public EntityCavern(World world) { super(world); setSize(1.4F, 0.7F); }
    @Override protected double health() { return 20D; }
    @Override protected double speed() { return 0.32D; }
    @Override protected double damage() { return 4D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
