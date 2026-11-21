package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Ordinary hostile ground melee; no extra hit effects or natural spawn registration. */
public final class EntityBellback extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/bellback");
    public EntityBellback(World world) { super(world); setSize(1.1F, 1.3F); }
    @Override protected double health() { return 40D; }
    @Override protected double speed() { return 0.19D; }
    @Override protected double damage() { return 4D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
