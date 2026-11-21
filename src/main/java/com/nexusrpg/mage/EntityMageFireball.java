package com.nexusrpg.mage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** A non-explosive fire spell: it can damage and ignite entities, never blocks. */
public final class EntityMageFireball extends EntityThrowable {
    private int flightTicks;
    public EntityMageFireball(World world) {
        super(world);
    }

    public EntityMageFireball(World world, EntityLivingBase caster) {
        super(world, caster);
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F;
    }

    @Override
    public void onUpdate() {
        // A saved projectile at its limit must not move or hit anything on its first
        // tick after loading. Vanilla resolves collisions inside super.onUpdate().
        if (!world.isRemote && flightTicks >= 80) {
            setDead();
            return;
        }
        super.onUpdate();
        if (!world.isRemote && !isDead) {
            flightTicks++;
            if (flightTicks >= 80) {
                setDead();
            } else if (flightTicks % 2 == 0 && world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME, posX, posY, posZ,
                        2, 0.06D, 0.06D, 0.06D, 0.0D);
            }
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        // Vanilla only writes ownerName automatically for players, not mob casters.
        EntityLivingBase caster = getThrower();
        if (caster != null) tag.setString("ownerName", caster.getUniqueID().toString());
        tag.setInteger("NexusMageFlightTicks", flightTicks);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        flightTicks = Math.max(0, Math.min(80, tag.getInteger("NexusMageFlightTicks")));
    }

    @Override
    protected void onImpact(RayTraceResult hit) {
        if (world.isRemote || isDead) return;
        EntityLivingBase caster = getThrower();
        if (hit.entityHit != null && hit.entityHit != caster && caster != null) {
            boolean damaged = hit.entityHit.attackEntityFrom(
                    DamageSource.causeIndirectMagicDamage(this, caster).setProjectile().setFireDamage(), 7.0F);
            if (damaged && hit.entityHit instanceof EntityLivingBase) hit.entityHit.setFire(3);
        }
        // No explosion, block placement, or terrain ignition, regardless of mobGriefing.
        setDead();
    }
}
