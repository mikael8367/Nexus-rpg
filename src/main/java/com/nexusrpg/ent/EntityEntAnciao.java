package com.nexusrpg.ent;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Persistent arena boss. EntityCreature avoids EntityMob's removal in peaceful mode. */
public final class EntityEntAnciao extends EntityCreature implements IMob, IEntAnciaoAnimated {
    private static final DataParameter<Integer> ANIMATION = EntityDataManager.createKey(EntityEntAnciao.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ANIMATION_TICKS = EntityDataManager.createKey(EntityEntAnciao.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> ENRAGED = EntityDataManager.createKey(EntityEntAnciao.class, DataSerializers.BOOLEAN);
    private final BossInfoServer bossBar = new BossInfoServer(new TextComponentString("Ent Ancião"),
            BossInfo.Color.GREEN, BossInfo.Overlay.PROGRESS);
    private int attackCooldown;
    private boolean rageRoared;
    private UUID attackTargetId;
    private final Set<UUID> struckThisAttack = new HashSet<UUID>();
    private double attackFacingX, attackFacingZ;
    private float attackYaw;
    private int lastAttack = NONE;
    private int pathDelay;

    public EntityEntAnciao(World world) {
        super(world);
        setSize(1.8F, 3.9F);
        enablePersistence();
        experienceValue = 100;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ANIMATION, NONE);
        dataManager.register(ANIMATION_TICKS, 0);
        dataManager.register(ENRAGED, false);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new ArenaCombat());
        tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        tasks.addTask(6, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(300.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(8.0D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9D);
    }

    /** Direct world.spawnEntity is still permitted; no biome spawn entry is registered. */
    @Override
    public boolean getCanSpawnHere() { return false; }

    @Override
    public int getAnimState() { return dataManager.get(ANIMATION); }
    @Override
    public int getAnimTicks() { return dataManager.get(ANIMATION_TICKS); }
    @Override
    public boolean isEnraged() { return dataManager.get(ENRAGED); }

    private void startAnimation(int state) {
        dataManager.set(ANIMATION, state);
        dataManager.set(ANIMATION_TICKS, 0);
        getNavigator().clearPath();
    }

    private boolean eligible(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || !entity.isEntityAlive()) return false;
        EntityPlayer player = (EntityPlayer) entity;
        return !player.isSpectator() && !player.capabilities.isCreativeMode
                && !player.capabilities.disableDamage && player.world == world;
    }

    private void beginAttack(EntityLivingBase target, int state) {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001D) {
            double angle = Math.toRadians(rotationYaw);
            attackFacingX = -Math.sin(angle);
            attackFacingZ = Math.cos(angle);
        } else {
            attackFacingX = dx / length;
            attackFacingZ = dz / length;
        }
        attackYaw = (float) Math.toDegrees(Math.atan2(-attackFacingX, attackFacingZ));
        attackTargetId = target.getUniqueID();
        struckThisAttack.clear();
        lastAttack = state;
        startAnimation(state);
        world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_WOOD_BREAK,
                SoundCategory.HOSTILE, 1.2F, state == SLAM ? 0.65F : 0.9F);
    }

    private void finishAttack(int cooldown) {
        attackTargetId = null;
        struckThisAttack.clear();
        startAnimation(NONE);
        attackCooldown = Math.max(attackCooldown, cooldown);
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (world.isRemote) return;
        bossBar.setPercent(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
        bossBar.setName(hasCustomName() ? getDisplayName() : new TextComponentString("Ent Ancião"));
        boolean rage = getHealth() < getMaxHealth() * 0.5F;
        dataManager.set(ENRAGED, rage);
        if (attackCooldown > 0) --attackCooldown;
        if (rage && !rageRoared && getAnimState() == NONE) {
            rageRoared = true;
            startAnimation(ROAR);
            world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 2.0F, 0.7F);
            EntCombatEffects.rageStart(this);
        }
        int state = getAnimState();
        if (state == NONE || state == DEATH) return;
        if (state == SLAM || state == SPIN) {
            EntityLivingBase target = getAttackTarget();
            // Do not finish an attack against a disconnected, invalid, or replaced target.
            if (!eligible(target) || attackTargetId == null || !attackTargetId.equals(target.getUniqueID())
                    || getDistanceSq(target) > 16.0D * 16.0D) {
                finishAttack(30);
                return;
            }
            // Telegraph and hit direction remain fixed; players can dodge around the boss.
            rotationYaw = attackYaw;
            rotationYawHead = attackYaw;
            renderYawOffset = attackYaw;
            getNavigator().clearPath();
        }
        int tick = getAnimTicks() + 1;
        dataManager.set(ANIMATION_TICKS, tick);
        EntCombatEffects.onAnimationTick(this, state, tick, attackFacingX, attackFacingZ);
        if (state == SLAM || state == SPIN) {
            if (state == SLAM && tick == 22) impact(state);
            if (state == SPIN && (tick == 20 || tick == 40 || tick == 60)) impact(state);
        }
        int duration = state == ROAR ? 60 : state == SPIN ? 80 : 52;
        if (tick >= duration) finishAttack(isEnraged() ? 20 : 34);
    }

    static boolean withinSlamArc(double dx, double dz, double facingX, double facingZ) {
        double distanceSq = dx * dx + dz * dz;
        return distanceSq <= 0.0001D
                || (dx * facingX + dz * facingZ) / Math.sqrt(distanceSq) >= 0.6428D;
    }

    private void impact(int state) {
        if (world.isRemote) return;
        double radius = state == SLAM ? 3.2D : 3.6D;
        float damage = state == SLAM ? (isEnraged() ? 17.0F : 12.0F) : (isEnraged() ? 9.0F : 7.0F);
        AxisAlignedBB area = getEntityBoundingBox().grow(radius, 0.8D, radius);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, area)) {
            if (!eligible(player) || struckThisAttack.contains(player.getUniqueID())) continue;
            double dx = player.posX - posX, dz = player.posZ - posZ;
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq > radius * radius || player.posY > posY + 2.4D
                    || player.getEntityBoundingBox().maxY < posY - 0.8D
                    || !getEntitySenses().canSee(player)) continue;
            if (state == SLAM && !withinSlamArc(dx, dz, attackFacingX, attackFacingZ)) continue;
            // Mark even a blocked/cancelled hit: no repeated attempts or protection bypass in a spin.
            struckThisAttack.add(player.getUniqueID());
            // Mob damage follows Forge/player damage hooks and vanilla knockback/resistance. No
            // extra velocity is applied after a protected, shielded or cancelled hit.
            player.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        }
        world.playSound(null, posX, posY, posZ,
                state == SLAM ? SoundEvents.BLOCK_WOOD_FALL : SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.HOSTILE, 1.2F, state == SLAM ? 0.6F : 0.7F);
    }

    private final class ArenaCombat extends EntityAIBase {
        ArenaCombat() { setMutexBits(3); }
        @Override
        public boolean shouldExecute() { return eligible(getAttackTarget()); }
        @Override
        public void resetTask() { getNavigator().clearPath(); }
        @Override
        public void updateTask() {
            EntityLivingBase target = getAttackTarget();
            if (!eligible(target)) return;
            if (getAnimState() != NONE) {
                getNavigator().clearPath();
                return;
            }
            getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (getDistanceSq(target) <= 3.0D * 3.0D && getEntitySenses().canSee(target)) {
                getNavigator().clearPath();
                if (attackCooldown <= 0) {
                    // Both moves are possible in either phase, but avoid monotonous repeats.
                    int state = lastAttack == SLAM ? (rand.nextInt(3) == 0 ? SLAM : SPIN)
                            : lastAttack == SPIN ? (rand.nextInt(3) == 0 ? SPIN : SLAM)
                            : (rand.nextBoolean() ? SLAM : SPIN);
                    beginAttack(target, state);
                }
            } else if (--pathDelay <= 0) {
                pathDelay = 10;
                getNavigator().tryMoveToEntityLiving(target, isEnraged() ? 1.35D : 1.0D);
            }
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        // Forge may cancel LivingDeathEvent before EntityLivingBase marks itself dead.
        if (!world.isRemote && dead && getAnimState() != DEATH) startAnimation(DEATH);
    }

    @Override
    protected void onDeathUpdate() {
        if (!world.isRemote) dataManager.set(ANIMATION_TICKS, getAnimTicks() + 1);
        super.onDeathUpdate();
    }

    @Override
    public void setDead() {
        if (!world.isRemote) bossBar.setVisible(false);
        super.setDead();
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        bossBar.addPlayer(player);
    }
    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("NexusEntAnimation", getAnimState());
        tag.setInteger("NexusEntAnimationTicks", getAnimTicks());
        tag.setInteger("NexusEntAttackCooldown", attackCooldown);
        tag.setBoolean("NexusEntRageRoared", rageRoared);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        // Cancel in-flight attacks on reload: no surprise unwarned hit or repeated pulse.
        int savedState = tag.getInteger("NexusEntAnimation");
        dataManager.set(ANIMATION, NONE);
        dataManager.set(ANIMATION_TICKS, 0);
        dataManager.set(ENRAGED, getHealth() < getMaxHealth() * 0.5F);
        attackTargetId = null;
        struckThisAttack.clear();
        attackCooldown = Math.max(savedState == SLAM || savedState == SPIN ? 40 : 0,
                Math.max(0, Math.min(100, tag.getInteger("NexusEntAttackCooldown"))));
        rageRoared = tag.getBoolean("NexusEntRageRoared");
        if (savedState == ROAR) rageRoared = false;
    }
}
