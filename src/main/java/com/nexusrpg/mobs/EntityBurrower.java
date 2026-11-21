package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Ordinary hostile ground melee AI; no secondary hit effects. */
public final class EntityBurrower extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/dune_burrower");
    public EntityBurrower(World world) { super(world); setSize(1.4F, 0.8F); }
    @Override protected double health() { return 32D; }
    @Override protected double speed() { return 0.23D; }
    @Override protected double damage() { return 5D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
