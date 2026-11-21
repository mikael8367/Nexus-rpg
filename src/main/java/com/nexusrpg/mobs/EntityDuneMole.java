package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Ordinary hostile ground melee; no extra hit effects or natural spawn registration. */
public final class EntityDuneMole extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/dune_mole");
    public EntityDuneMole(World world) { super(world); setSize(1.1F, 1.0F); }
    @Override protected double health() { return 34D; }
    @Override protected double speed() { return 0.24D; }
    @Override protected double damage() { return 5D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
