package com.nexusrpg.magma;

import java.util.UUID;
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
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;

/** Arena boss. The arena must explicitly spawn it; it has no natural biome spawn. */
public final class EntityColossoMagma extends EntityCreature implements IMob, IColossoMagmaAnimated {
    private static final DataParameter<Integer> STATE = EntityDataManager.createKey(EntityColossoMagma.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TICKS = EntityDataManager.createKey(EntityColossoMagma.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> RAGE = EntityDataManager.createKey(EntityColossoMagma.class, DataSerializers.BOOLEAN);
    private final BossInfoServer bossBar = new BossInfoServer(new TextComponentTranslation("entity.nexusrpg.colosso_magma.name"), BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
    private UUID attackTargetId;
    private int cooldown;
    private int pathDelay;
    private int lastAttack;
    private double facingX, facingZ;
    private float attackYaw;

    public EntityColossoMagma(World world) {
        super(world);
        setSize(2.2F, 4.1F);
        isImmuneToFire = true;
        enablePersistence();
        experienceValue = 100;
    }

    @Override protected void entityInit() {
        super.entityInit();
        dataManager.register(STATE, NONE);
        dataManager.register(TICKS, 0);
        dataManager.register(RAGE, false);
    }
    @Override protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new Combat());
        tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        tasks.addTask(6, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
    }
    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(360.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(10.0D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.9D);
    }
    @Override public boolean getCanSpawnHere() { return false; }
    @Override public int getAnimState() { return dataManager.get(STATE); }
    @Override public int getAnimTicks() { return dataManager.get(TICKS); }
    @Override public boolean isEnraged() { return dataManager.get(RAGE); }

    private boolean eligible(EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || !entity.isEntityAlive()) return false;
        EntityPlayer p = (EntityPlayer) entity;
        return !p.isSpectator() && !p.capabilities.isCreativeMode && !p.capabilities.disableDamage && p.world == world;
    }
    private void animation(int state) {
        dataManager.set(STATE, state);
        dataManager.set(TICKS, 0);
        getNavigator().clearPath();
    }
    private void begin(EntityLivingBase target, int state) {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001D) {
            facingX = -Math.sin(Math.toRadians(rotationYaw));
            facingZ = Math.cos(Math.toRadians(rotationYaw));
        } else { facingX = dx / length; facingZ = dz / length; }
        attackYaw = (float) Math.toDegrees(Math.atan2(-facingX, facingZ));
        attackTargetId = target.getUniqueID();
        lastAttack = state;
        animation(state);
        world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_LAVA_POP, SoundCategory.HOSTILE, 1.4F, 0.7F);
    }
    private void finish(int delay) {
        attackTargetId = null;
        cooldown = delay;
        animation(NONE);
    }
    @Override protected void updateAITasks() {
        super.updateAITasks();
        if (world.isRemote) return;
        bossBar.setPercent(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
        bossBar.setName(hasCustomName() ? getDisplayName() : new TextComponentTranslation("entity.nexusrpg.colosso_magma.name"));
        dataManager.set(RAGE, getHealth() < getMaxHealth() * 0.5F);
        if (cooldown > 0) --cooldown;
        int state = getAnimState();
        if (state != PUNCH && state != NOVA) return;
        EntityLivingBase target = getAttackTarget();
        if (!eligible(target) || attackTargetId == null || !attackTargetId.equals(target.getUniqueID()) || getDistanceSq(target) > 256.0D) {
            finish(30);
            return;
        }
        rotationYaw = attackYaw;
        rotationYawHead = attackYaw;
        renderYawOffset = attackYaw;
        getNavigator().clearPath();
        int tick = getAnimTicks() + 1;
        dataManager.set(TICKS, tick);
        if ((state == PUNCH && tick == 28) || (state == NOVA && tick == 24)) impact(state);
        if (tick >= (state == PUNCH ? 38 : 60)) finish(isEnraged() ? 20 : 34);
    }
    private void impact(int state) {
        double radius = state == PUNCH ? 3.6D : 4.5D;
        float damage = state == PUNCH ? (isEnraged() ? 17F : 12F) : (isEnraged() ? 14F : 10F);
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(radius, 0.8D, radius))) {
            if (!eligible(p) || !getEntitySenses().canSee(p)) continue;
            double dx = p.posX - posX, dz = p.posZ - posZ;
            double distance = dx * dx + dz * dz;
            if (distance > radius * radius || p.posY > posY + 3.0D || p.getEntityBoundingBox().maxY < posY - 0.8D) continue;
            if (state == PUNCH && distance > 0.0001D && (dx * facingX + dz * facingZ) / Math.sqrt(distance) < 0.6428D) continue;
            p.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        }
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.0F, 0.75F);
    }
    private final class Combat extends EntityAIBase {
        Combat() { setMutexBits(3); }
        @Override public boolean shouldExecute() { return eligible(getAttackTarget()); }
        @Override public void resetTask() { getNavigator().clearPath(); }
        @Override public void updateTask() {
            EntityLivingBase target = getAttackTarget();
            if (!eligible(target)) return;
            if (getAnimState() != NONE) { getNavigator().clearPath(); return; }
            getLookHelper().setLookPositionWithEntity(target, 30F, 30F);
            if (getDistanceSq(target) <= 3.5D * 3.5D && getEntitySenses().canSee(target)) {
                getNavigator().clearPath();
                if (cooldown <= 0) begin(target, lastAttack == PUNCH ? NOVA : PUNCH);
            } else if (--pathDelay <= 0) {
                pathDelay = 10;
                getNavigator().tryMoveToEntityLiving(target, isEnraged() ? 1.3D : 1.0D);
            }
        }
    }
    @Override public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!world.isRemote && dead) animation(DEATH);
    }
    @Override public void setDead() { if (!world.isRemote) bossBar.setVisible(false); super.setDead(); }
    @Override public void addTrackingPlayer(EntityPlayerMP player) { super.addTrackingPlayer(player); bossBar.addPlayer(player); }
    @Override public void removeTrackingPlayer(EntityPlayerMP player) { super.removeTrackingPlayer(player); bossBar.removePlayer(player); }
    @Override public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("NexusMagmaCooldown", cooldown);
        tag.setBoolean("NexusMagmaAttacking", getAnimState() == PUNCH || getAnimState() == NOVA);
    }
    @Override public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        dataManager.set(STATE, NONE);
        dataManager.set(TICKS, 0);
        dataManager.set(RAGE, getHealth() < getMaxHealth() * 0.5F);
        attackTargetId = null;
        cooldown = Math.max(tag.getBoolean("NexusMagmaAttacking") ? 40 : 0, Math.max(0, Math.min(100, tag.getInteger("NexusMagmaCooldown"))));
    }
}
