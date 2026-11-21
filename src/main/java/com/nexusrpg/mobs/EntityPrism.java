package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Hostile ground creature using standard melee AI, without secondary hit effects. */
public final class EntityPrism extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/prism_tortoise");
    public EntityPrism(World world) { super(world); setSize(1.3F, 1.0F); }
    @Override protected double health() { return 36D; }
    @Override protected double speed() { return 0.19D; }
    @Override protected double damage() { return 6D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
