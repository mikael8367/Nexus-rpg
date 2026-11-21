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
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** Ground-based encounter: targeted attacks only, no terrain damage or natural spawns. */
public abstract class EntityElementalDragon extends EntityCreature implements IMob {
    public enum Element { FIRE, WATER, WIND, EARTH }
    private static final int COOLDOWN_MAX = 200;
    private int attackCooldown;
    private int pathCooldown;

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
        if (!world.isRemote && attackCooldown > 0) --attackCooldown;
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
    }
    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        attackCooldown = Math.max(0, Math.min(COOLDOWN_MAX, tag.getInteger("DragonAttackCooldown")));
        enablePersistence();
    }
}
