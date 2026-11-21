package com.nexusrpg.mobs;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
/** Ordinary hostile ground melee AI; no secondary hit effects. */
public final class EntityRootwarden extends EntityNexusMob {
    private static final ResourceLocation LOOT = new ResourceLocation("nexusrpg", "entities/root_warden");
    public EntityRootwarden(World world) { super(world); setSize(0.9F, 2.2F); }
    @Override protected double health() { return 40D; }
    @Override protected double speed() { return 0.2D; }
    @Override protected double damage() { return 6D; }
    @Override protected ResourceLocation getLootTable() { return LOOT; }
}
