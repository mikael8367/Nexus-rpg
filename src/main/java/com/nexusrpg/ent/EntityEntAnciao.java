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

/** Persistent arena boss. EntityCreature avoids EntityMob's removal in peaceful mode. */
public final class EntityEntAnciao extends EntityCreature implements IMob, IEntAnciaoAnimated {
    private static final DataParameter<Integer> ANIMATION = EntityDataManager.createKey(EntityEntAnciao.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ANIMATION_TICKS = EntityDataManager.createKey(EntityEntAnciao.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> ENRAGED = EntityDataManager.createKey(EntityEntAnciao.class, DataSerializers.BOOLEAN);
    private final BossInfoServer bossBar = new BossInfoServer(new TextComponentString("Ent Ancião"),
            BossInfo.Color.GREEN, BossInfo.Overlay.PROGRESS);
    private int attackCooldown;
    private boolean rageRoared;

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

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        bossBar.setPercent(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
        bossBar.setName(hasCustomName() ? getDisplayName() : new TextComponentString("Ent Ancião"));
        boolean rage = getHealth() < getMaxHealth() * 0.5F;
        dataManager.set(ENRAGED, rage);
        if (attackCooldown > 0) --attackCooldown;
        if (rage && !rageRoared && getAnimState() == NONE) {
            rageRoared = true;
            startAnimation(ROAR);
            world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 2.0F, 0.7F);
        }
        int state = getAnimState();
        if (state == NONE || state == DEATH) return;
        int tick = getAnimTicks() + 1;
        dataManager.set(ANIMATION_TICKS, tick);
        // The model's slam impact is between ticks 20 and 24. Each impact is applied once.
        if (state == SLAM && tick == 22) hitNearbyPlayers(3.2D, isEnraged() ? 17.0F : 12.0F, 0.9D);
        if (state == SPIN && (tick == 20 || tick == 40 || tick == 60)) hitNearbyPlayers(3.6D, 7.0F, 0.65D);
        int duration = state == ROAR ? 60 : state == SPIN ? 80 : 52;
        if (tick >= duration) {
            startAnimation(NONE);
            attackCooldown = isEnraged() ? 12 : 28;
        }
    }

    private void hitNearbyPlayers(double radius, float damage, double knockback) {
        AxisAlignedBB area = getEntityBoundingBox().grow(radius, 0.8D, radius);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, area)) {
            if (!player.isEntityAlive() || player.isSpectator() || player.capabilities.isCreativeMode
                    || getDistanceSq(player) > radius * radius || !getEntitySenses().canSee(player)) continue;
            if (player.attackEntityFrom(DamageSource.causeMobDamage(this), damage)) {
                double dx = player.posX - posX, dz = player.posZ - posZ;
                double length = Math.sqrt(dx * dx + dz * dz);
                if (length > 0.0001D) player.addVelocity(dx / length * knockback, 0.25D, dz / length * knockback);
                player.velocityChanged = true;
            }
        }
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0F, 0.7F);
    }

    private final class ArenaCombat extends EntityAIBase {
        ArenaCombat() { setMutexBits(3); }
        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = getAttackTarget();
            return target != null && target.isEntityAlive();
        }
        @Override
        public void resetTask() { getNavigator().clearPath(); }
        @Override
        public void updateTask() {
            EntityLivingBase target = getAttackTarget();
            if (target == null || !target.isEntityAlive()) return;
            getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (getAnimState() != NONE) {
                getNavigator().clearPath();
            } else if (getDistanceSq(target) <= 3.0D * 3.0D && getEntitySenses().canSee(target)) {
                getNavigator().clearPath();
                if (attackCooldown <= 0) startAnimation(isEnraged() && rand.nextBoolean() ? SPIN : SLAM);
            } else {
                getNavigator().tryMoveToEntityLiving(target, isEnraged() ? 1.35D : 1.0D);
            }
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (!world.isRemote) startAnimation(DEATH);
        super.onDeath(cause);
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
        int state = tag.getInteger("NexusEntAnimation");
        if (state < NONE || state > SLAM) state = NONE;
        int maxTicks = state == ROAR ? 59 : state == SPIN ? 79 : state == SLAM ? 51 : 0;
        dataManager.set(ANIMATION, state);
        dataManager.set(ANIMATION_TICKS, Math.max(0, Math.min(maxTicks, tag.getInteger("NexusEntAnimationTicks"))));
        dataManager.set(ENRAGED, getHealth() < getMaxHealth() * 0.5F);
        attackCooldown = Math.max(0, Math.min(100, tag.getInteger("NexusEntAttackCooldown")));
        rageRoared = tag.getBoolean("NexusEntRageRoared");
    }
}
