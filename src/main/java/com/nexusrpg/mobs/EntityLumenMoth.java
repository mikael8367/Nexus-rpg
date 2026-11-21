package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Ordinary hostile ground melee; no extra hit effects or natural spawn registration. */
public final class EntityLumenMoth extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/lumen_moth");
    public EntityLumenMoth(World world) { super(world); setSize(1.0F, 1.2F); }
    @Override protected double health() { return 20D; }
    @Override protected double speed() { return 0.3D; }
    @Override protected double damage() { return 3D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
