package com.nexusrpg.dragons;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** Encounter with experimental, bounded combat flight; no terrain damage or natural spawns. */
public abstract class EntityElementalDragon extends EntityCreature implements IMob {
    public enum Element { FIRE, WATER, WIND, EARTH }
    private static final int COOLDOWN_MAX = 200;
    private int attackCooldown;
    private int pathCooldown;
    // Landing intent only, never a persistent immunity to fall damage.
    private boolean flightAssist;
    private EntityLivingBase flightTarget;

    public EntityElementalDragon(World world) {
        super(world);
        setSize(2.8F, 3.4F);
        enablePersistence();
        experienceValue = 80;
    }

    /** A class fixes its element; it is not mutable or inferred from entity NBT. */
    public abstract Element getElement();

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(180.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(6.0D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.65D);
    }

    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new DragonCombat());
        tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        tasks.addTask(6, new EntityAILookIdle(this));
        // Sight is checked inside combat only after the loaded-area guard.
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, false));
    }

    @Override protected PathNavigate createNavigator(World world) { return new LoadedOnlyNavigator(world); }

    /** Vanilla builds a ChunkCache out to followRange + 16, including on deferred repaths. */
    private final class LoadedOnlyNavigator extends PathNavigateGround {
        LoadedOnlyNavigator(World world) { super(EntityElementalDragon.this, world); }
        private boolean safe() {
            float range = getPathSearchRange();
            return !Float.isNaN(range) && range >= 0 && range <= 128
                    && world.isAreaLoaded(getPosition(), (int)Math.ceil(range) + 17);
        }
        @Override public Path getPathToPos(BlockPos pos) { return safe() ? super.getPathToPos(pos) : null; }
        @Override public Path getPathToEntityLiving(Entity target) { return safe() ? super.getPathToEntityLiving(target) : null; }
        @Override public void onUpdateNavigation() {
            if (!safe()) { clearPath(); return; }
            super.onUpdateNavigation();
        }
    }

    /** Spawn eggs and explicit world.spawnEntity bypass this natural-spawn gate. */
    @Override public boolean getCanSpawnHere() { return false; }

    @Override protected void updateAITasks() {
        super.updateAITasks();
        if (!world.isRemote) {
            if (attackCooldown > 0) --attackCooldown;
            updateFlight();
            flightTarget = null;
        }
    }

    @Override public void onLivingUpdate() {
        // updateAITasks is not called with NoAI, including immediately after reload.
        if (!world.isRemote && isAIDisabled()) {
            if (flightAssist) { motionX = 0; motionZ = 0; motionY = Math.min(0, motionY); }
            flightAssist = false;
            flightTarget = null;
        }
        super.onLivingUpdate();
    }

    /** Powered flight never teleports, disables gravity/collisions, or requests chunks. */
    private void updateFlight() {
        EntityLivingBase target = flightTarget;
        // Clear even when the surrounding chunks are no longer loaded.
        boolean wasAssisted = flightAssist;
        if (onGround) flightAssist = false;
        if (!world.isAreaLoaded(getPosition(), 8)) {
            if (wasAssisted) {
                getNavigator().clearPath();
                getMoveHelper().setMoveTo(posX, posY, posZ, 0.0D);
                motionX = 0; motionZ = 0; motionY = Math.min(0, motionY);
            }
            flightAssist = false;
            return;
        }
        // Lava checks may access blocks: always perform them after the loaded guard.
        if (isInWater() || isInLava() || isRiding() || isBeingRidden()) {
            flightAssist = false;
            return;
        }
        boolean pursue = target == getAttackTarget() && eligible(target)
                && Double.isFinite(posX) && Double.isFinite(posY) && Double.isFinite(posZ)
                && Double.isFinite(target.posX) && Double.isFinite(target.posY) && Double.isFinite(target.posZ)
                && getDistanceSq(target) <= 144.0D;
        if (!pursue && !flightAssist) return;
        double dx = 0, dy = -0.12D, dz = 0;
        if (pursue) {
            double top = Math.max(1.0D, world.getActualHeight() - 5.0D);
            double desiredY = Math.max(1.0D, Math.min(top, target.posY + 5.0D));
            dy = Math.max(-0.12D, Math.min(0.16D, (desiredY - posY) * 0.12D));
            double x = target.posX - posX, z = target.posZ - posZ;
            double length = Math.sqrt(x*x + z*z);
            if (length > 7.0D) { dx = x / length * 0.16D; dz = z / length * 0.16D; }
            if (posY >= top) dy = Math.min(dy, 0.0D);
            else dy = Math.min(dy, top - posY);
            if (posY <= 1.0D) dy = Math.max(dy, 0.0D);
        }
        AxisAlignedBB box = getEntityBoundingBox();
        AxisAlignedBB sweep = box.expand(dx, dy, dz);
        // Don't steer into fluids; hand control back to vanilla and its swim AI.
        if (world.containsAnyLiquid(sweep)) {
            flightAssist = false;
            motionX = 0; motionZ = 0; motionY = Math.min(0, motionY);
            return;
        }
        // Test horizontal and ascending paths separately: a floor below the dragon must
        // not prevent horizontal steering. Vanilla clips the final motion per axis.
        if (!world.getCollisionBoxes(this, box.expand(dx, 0, dz)).isEmpty()) {
            dx = 0; dz = 0;
        }
        if (dy > 0 && !world.getCollisionBoxes(this, box.expand(0, dy, 0)).isEmpty()) dy = 0;
        // Negative motion is intentionally retained: vanilla clips it and sets onGround.
        getNavigator().clearPath();
        getMoveHelper().setMoveTo(posX, posY, posZ, 0.0D);
        motionX = dx; motionY = dy; motionZ = dz;
        flightAssist = !onGround || dy > 0;
        // Only erase fall distance while the bounded controller is actually running.
        // Normal fall damage resumes immediately when control is relinquished.
        if (flightAssist) fallDistance = 0;
    }

    private boolean eligible(EntityLivingBase target) {
        if (!(target instanceof EntityPlayer) || !target.isEntityAlive() || target.world != world
                || isOnSameTeam(target)) return false;
        EntityPlayer player = (EntityPlayer) target;
        return !player.isSpectator() && !player.capabilities.isCreativeMode && !player.capabilities.disableDamage;
    }

    /** Only call from server AI after validating loaded area, target, range and line of sight. */
    private void elementalStrike(EntityLivingBase target, boolean melee) {
        float damage = melee ? 8.0F : 6.0F;
        // Set cooldown even if another mod cancels damage; prevents rapid repeated event spam.
        attackCooldown = melee ? 30 : 80;
        if (!melee) DragonBreathVisual.show(this, target);
        // Forge 1.12 can return true even if LivingHurtEvent or LivingDamageEvent
        // cancels the actual health loss. Never apply secondary effects in that case.
        // Conservatively treat absorption-only hits as blocked too: absorption may be
        // consumed before a later LivingDamageEvent is cancelled.
        float healthBefore = target.getHealth();
        if (!target.attackEntityFrom(DamageSource.causeMobDamage(this), damage)
                || !(target.getHealth() < healthBefore) || !target.isEntityAlive()) return;
        if (melee) return;
        switch (getElement()) {
            case FIRE:
                target.setFire(3);
                break;
            case WATER:
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 0));
                target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 0));
                break;
            case WIND:
                double dx = target.posX - posX, dz = target.posZ - posZ;
                double length = Math.sqrt(dx * dx + dz * dz);
                if (length > 0.0001D) {
                    target.addVelocity(dx / length * 0.8D, 0.3D, dz / length * 0.8D);
                    target.velocityChanged = true;
                }
                break;
            case EARTH:
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
                target.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 80, 0));
                break;
        }
    }

    private final class DragonCombat extends EntityAIBase {
        DragonCombat() { setMutexBits(3); }
        @Override public boolean shouldExecute() { return eligible(getAttackTarget()); }
        @Override public boolean shouldContinueExecuting() { return eligible(getAttackTarget()); }
        @Override public void resetTask() { getNavigator().clearPath(); }
        @Override public void updateTask() {
            EntityLivingBase target = getAttackTarget();
            if (world.isRemote || !eligible(target)) return;
            // Pathfinding and sight rays may query blocks. Only run when the entire nearby
            // area is already loaded, never requesting new chunks for pursuit or attacks.
            if (!world.isAreaLoaded(getPosition(), 32)) { getNavigator().clearPath(); return; }
            double distance = getDistanceSq(target);
            getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            boolean visible = distance <= 144.0D && getEntitySenses().canSee(target);
            if (visible) {
                flightTarget = target;
                getNavigator().clearPath();
                if (attackCooldown == 0) elementalStrike(target, distance <= 16.0D);
            } else if (--pathCooldown <= 0 && distance <= 24.0D * 24.0D) {
                pathCooldown = 20;
                getNavigator().tryMoveToEntityLiving(target, 1.0D);
            }
        }
    }

    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("DragonAttackCooldown", attackCooldown);
        tag.setBoolean("DragonFlightLanding", flightAssist);
    }
    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        attackCooldown = Math.max(0, Math.min(COOLDOWN_MAX, tag.getInteger("DragonAttackCooldown")));
        flightAssist = tag.getBoolean("DragonFlightLanding");
        flightTarget = null;
        enablePersistence();
    }
}
