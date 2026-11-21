package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
public final class EntityBasalt extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/basalt_guardian");
    public EntityBasalt(World world) { super(world); setSize(1.0F, 2.4F); }
    @Override protected double health() { return 42D; }
    @Override protected double speed() { return 0.2D; }
    @Override protected double damage() { return 7D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
