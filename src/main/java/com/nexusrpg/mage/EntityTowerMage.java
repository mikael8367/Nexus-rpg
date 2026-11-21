package com.nexusrpg.mage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.util.text.TextComponentString;

/** A persistent tower guardian. The spell is an instant, line-of-sight magic hit. */
public final class EntityTowerMage extends EntityMob implements IRangedAttackMob {
    private final BossInfoServer bossBar = new BossInfoServer(
            new TextComponentString("Mago da Torre"), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);

    public EntityTowerMage(World world) {
        super(world);
        setSize(0.6F, 1.95F);
        enablePersistence();
        experienceValue = 25;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackRanged(this, 1.0D, 40, 16.0F));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.7D));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(80.0D);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(24.0D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.26D);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(3.0D);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        if (world.isRemote || !isEntityAlive() || target == null || !target.isEntityAlive()
                || getDistanceSq(target) > 256.0D || !getEntitySenses().canSee(target)) return;
        // Real damage credited to this mob, not a visual-only effect. No explosion or terrain damage.
        target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this), 6.0F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            double sx = posX, sy = posY + getEyeHeight(), sz = posZ;
            double ex = target.posX, ey = target.posY + target.height * 0.55D, ez = target.posZ;
            for (int i = 1; i <= 12; i++) {
                double t = i / 12.0D;
                server.spawnParticle(EnumParticleTypes.SPELL_WITCH, sx + (ex - sx) * t,
                        sy + (ey - sy) * t, sz + (ez - sz) * t, 1, 0.0D, 0.0D, 0.0D, 0.0D, new int[0]);
            }
        }
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_WITCH_THROW,
                SoundCategory.HOSTILE, 1.0F, 1.0F);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) { }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (hasHome()) {
            BlockPos home = getHomePosition();
            tag.setBoolean("NexusMageHasHome", true);
            tag.setInteger("NexusMageHomeX", home.getX());
            tag.setInteger("NexusMageHomeY", home.getY());
            tag.setInteger("NexusMageHomeZ", home.getZ());
            tag.setInteger("NexusMageHomeRadius", (int) getMaximumHomeDistance());
        } else {
            tag.removeTag("NexusMageHasHome");
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.getBoolean("NexusMageHasHome")) {
            setHomePosAndDistance(new BlockPos(tag.getInteger("NexusMageHomeX"),
                    tag.getInteger("NexusMageHomeY"), tag.getInteger("NexusMageHomeZ")),
                    Math.max(1, tag.getInteger("NexusMageHomeRadius")));
        } else {
            detachHome();
        }
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        bossBar.setPercent(Math.max(0.0F, Math.min(1.0F, getHealth() / getMaxHealth())));
        if (hasCustomName()) bossBar.setName(getDisplayName());
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
}
