package com.nexusrpg.dragons;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

/** Server-broadcast visual only: no entity, collision, damage or world/block lookup. */
public final class DragonBreathVisual {
    private static final int SAMPLES = 12;
    private static final double MAX_DISTANCE_SQ = 16.0D * 16.0D;

    private DragonBreathVisual() { }

    /** Call once for a validated ranged attack, on the server thread. */
    public static void show(EntityElementalDragon dragon, EntityLivingBase target) {
        if (dragon == null || target == null || dragon.world == null || dragon.world.isRemote
                || !(dragon.world instanceof WorldServer) || target.world != dragon.world) return;
        EntityElementalDragon.Element element = dragon.getElement();
        if (element == null) return;

        double sx = dragon.posX, sy = dragon.posY + dragon.height * 0.70D, sz = dragon.posZ;
        double ex = target.posX, ey = target.posY + target.height * 0.50D, ez = target.posZ;
        if (!finite(sx) || !finite(sy) || !finite(sz) || !finite(ex) || !finite(ey) || !finite(ez)) return;
        double dx = ex - sx, dy = ey - sy, dz = ez - sz;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (!finite(distanceSq) || distanceSq < 0.0001D || distanceSq > MAX_DISTANCE_SQ) return;
        // Move the source toward the head, but never past the target at close range.
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > 0.0001D) {
            double shift = Math.min(0.9D, horizontal * 0.25D);
            sx += dx / horizontal * shift;
            sz += dz / horizontal * shift;
        }

        WorldServer world = (WorldServer) dragon.world;
        for (int i = 0; i < SAMPLES; i++) {
            double t = (i + 1.0D) / SAMPLES;
            double x = sx + (ex - sx) * t, y = sy + (ey - sy) * t, z = sz + (ez - sz) * t;
            double spread = 0.06D + 0.20D * t;
            switch (element) {
                case FIRE:
                    emit(world, EnumParticleTypes.FLAME, x, y, z, spread);
                    emit(world, EnumParticleTypes.SMOKE_NORMAL, x, y, z, spread);
                    break;
                case WATER:
                    emit(world, EnumParticleTypes.WATER_SPLASH, x, y, z, spread);
                    emit(world, EnumParticleTypes.WATER_DROP, x, y, z, spread);
                    break;
                case WIND:
                    emit(world, EnumParticleTypes.CLOUD, x, y, z, spread);
                    emit(world, EnumParticleTypes.SPELL_INSTANT, x, y, z, spread);
                    break;
                case EARTH:
                    // A fixed block state supplies the particle texture; no block is read or changed.
                    world.spawnParticle(EnumParticleTypes.BLOCK_DUST, x, y, z, 2,
                            spread, spread, spread, 0.0D, Block.getStateId(Blocks.DIRT.getDefaultState()));
                    emit(world, EnumParticleTypes.CLOUD, x, y, z, spread);
                    break;
                default: return;
            }
        }
    }

    private static void emit(WorldServer world, EnumParticleTypes particle,
                             double x, double y, double z, double spread) {
        world.spawnParticle(particle, x, y, z, 2, spread, spread, spread, 0.0D);
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
