package com.nexusrpg.magic;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Right-click spells: all state changes and targeting happen on the server. */
public final class ItemMagicStaff extends Item {
    public enum Spell { ARCANE, HEAL, WARD, SWIFT }
    private final Spell spell;
    private final int cooldown;

    public ItemMagicStaff(Spell spell, int cooldown) {
        this.spell = spell;
        this.cooldown = cooldown;
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("tooltip.nexusrpg.magic." + spell.name().toLowerCase(java.util.Locale.ROOT)));
        tooltip.add(I18n.format("tooltip.nexusrpg.magic.cost"));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack staff = player.getHeldItem(hand);
        // Client never changes inventory, health, effects, or targeting.
        if (world.isRemote) return new ActionResult<>(EnumActionResult.SUCCESS, staff);
        if (player.getCooldownTracker().hasCooldown(this) || !canApply(player)) {
            return new ActionResult<>(EnumActionResult.FAIL, staff);
        }
        int dustSlot = -1;
        if (!player.capabilities.isCreativeMode) {
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack stack = player.inventory.mainInventory.get(i);
                if (!stack.isEmpty() && stack.getItem() == Items.REDSTONE) { dustSlot = i; break; }
            }
            if (dustSlot < 0) return new ActionResult<>(EnumActionResult.FAIL, staff);
        }
        // Reserve the dust before calling Forge hooks: a hook may change inventory.
        // A rejected heal/effect returns the reservation without starting cooldown.
        ItemStack reserved = dustSlot < 0 ? ItemStack.EMPTY : player.inventory.decrStackSize(dustSlot, 1);
        if (dustSlot >= 0) player.inventory.markDirty();
        switch (spell) {
            case ARCANE: castArcane((WorldServer) world, player); break;
            case HEAL:
                float oldHealth = player.getHealth();
                player.heal(6.0F);
                if (player.getHealth() <= oldHealth) return failedCast(player, staff, reserved);
                particles((WorldServer) world, player, EnumParticleTypes.HEART);
                break;
            case WARD:
                if (!applyPotion(player, MobEffects.RESISTANCE, 200)) return failedCast(player, staff, reserved);
                particles((WorldServer) world, player, EnumParticleTypes.SPELL_INSTANT);
                break;
            case SWIFT:
                if (!applyPotion(player, MobEffects.SPEED, 300)) return failedCast(player, staff, reserved);
                particles((WorldServer) world, player, EnumParticleTypes.CLOUD);
                break;
            default: throw new IllegalStateException("Unknown spell");
        }
        player.getCooldownTracker().setCooldown(this, cooldown);
        world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 0.5F, 1.4F);
        return new ActionResult<>(EnumActionResult.SUCCESS, staff);
    }

    private static ActionResult<ItemStack> failedCast(EntityPlayer player, ItemStack staff, ItemStack reserved) {
        if (!reserved.isEmpty()) {
            player.inventory.addItemStackToInventory(reserved);
            // A hook could fill the inventory while the spell was being applied.
            if (!reserved.isEmpty()) player.dropItem(reserved, false);
            player.inventory.markDirty();
        }
        return new ActionResult<>(EnumActionResult.FAIL, staff);
    }

    private boolean canApply(EntityPlayer player) {
        switch (spell) {
            case HEAL: return player.isEntityAlive() && player.getHealth() < player.getMaxHealth();
            case WARD: return canApplyPotion(player, MobEffects.RESISTANCE, 200);
            case SWIFT: return canApplyPotion(player, MobEffects.SPEED, 300);
            case ARCANE: return true;
            default: return false;
        }
    }

    private static boolean canApplyPotion(EntityPlayer player, Potion potion, int duration) {
        PotionEffect existing = player.getActivePotionEffect(potion);
        // Never downgrade a stronger effect, or spend dust refreshing a longer one.
        return existing == null || (existing.getAmplifier() == 0 && existing.getDuration() < duration);
    }

    private static boolean applyPotion(EntityPlayer player, Potion potion, int duration) {
        PotionEffect before = player.getActivePotionEffect(potion);
        int oldAmplifier = before == null ? -1 : before.getAmplifier();
        int oldDuration = before == null ? 0 : before.getDuration();
        player.addPotionEffect(new PotionEffect(potion, duration, 0));
        PotionEffect after = player.getActivePotionEffect(potion);
        return after != null && (after.getAmplifier() > oldAmplifier
            || (after.getAmplifier() == oldAmplifier && after.getDuration() > oldDuration));
    }

    private static void particles(WorldServer world, EntityPlayer player, EnumParticleTypes type) {
        world.spawnParticle(type, player.posX, player.posY + 1.0D, player.posZ,
            12, 0.35D, 0.45D, 0.35D, 0.02D);
    }

    private static void castArcane(WorldServer world, EntityPlayer player) {
        Vec3d start = player.getPositionEyes(1.0F);
        Vec3d direction = player.getLook(1.0F);
        Vec3d end = start.add(direction.scale(16.0D));
        RayTraceResult wall = world.rayTraceBlocks(start, end, false, true, false);
        if (wall != null && wall.hitVec != null) end = wall.hitVec;
        double closest = start.distanceTo(end);
        Entity hit = null;
        Vec3d hitPoint = end;
        AxisAlignedBB search = player.getEntityBoundingBox().expand(direction.x * 16.0D,
            direction.y * 16.0D, direction.z * 16.0D).grow(1.0D);
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, search);
        for (Entity candidate : entities) {
            if (!candidate.canBeCollidedWith() || candidate.isDead || candidate == player.getRidingEntity()) continue;
            AxisAlignedBB box = candidate.getEntityBoundingBox().grow(candidate.getCollisionBorderSize());
            RayTraceResult intercept = box.calculateIntercept(start, end);
            Vec3d point = box.contains(start) ? start : (intercept == null ? null : intercept.hitVec);
            if (point == null) continue;
            double distance = start.distanceTo(point);
            if (distance < closest) { closest = distance; hit = candidate; hitPoint = point; }
        }
        // All collidable entities stop the ray; only hostile, non-owned, non-teammates take damage.
        if (hit instanceof EntityLivingBase && hit instanceof IMob && !(hit instanceof IEntityOwnable)
                && !player.isOnSameTeam(hit)) {
            hit.attackEntityFrom(DamageSource.causePlayerDamage(player), 6.0F);
        }
        double length = start.distanceTo(hitPoint);
        int count = Math.min(32, (int) (length * 2.0D));
        for (int i = 1; i <= count; i++) {
            Vec3d point = start.add(direction.scale(length * i / (count + 1.0D)));
            world.spawnParticle(EnumParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
