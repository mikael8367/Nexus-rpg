package com.nexusrpg.supplies;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import java.util.List;
import javax.annotation.Nullable;

/** One-shot field supplies. No player NBT, quest progress or inventory scanning. */
public final class ItemFieldSupply extends Item {
    public enum Kind { BANDAGE, ANTIDOTE, ESCAPE }
    public static final int COOLDOWN = 40;
    private final Kind kind;
    public ItemFieldSupply(Kind kind) {
        if (kind == null) throw new IllegalArgumentException("kind");
        this.kind = kind;
        setMaxStackSize(16);
    }

    /** Narrow adapter permits deterministic tests of the actual consumption rules. */
    interface Target {
        boolean cooldown(Item item);
        void cooldown(Item item, int ticks);
        boolean heal();
        boolean cleanse();
        boolean speed();
    }
    boolean use(ItemStack stack, Target target, boolean creative) {
        if (stack == null || stack.isEmpty() || stack.getItem() != this || target == null || target.cooldown(this)) return false;
        boolean changed;
        switch (kind) {
            case BANDAGE: changed = target.heal(); break;
            case ANTIDOTE: changed = target.cleanse(); break;
            case ESCAPE: changed = target.speed(); break;
            default: return false;
        }
        if (!changed) return false;
        target.cooldown(this, COOLDOWN);
        if (!creative) stack.shrink(1);
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isEntityAlive() || player.isSpectator() || stack.isEmpty() || stack.getItem() != this)
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        // Client predicts only the interaction result, never the effect or consumption.
        // SUCCESS prevents a second offhand use for the same click when likely useful.
        if (world.isRemote) return new ActionResult<ItemStack>(likelyUseful(player) ? EnumActionResult.SUCCESS : EnumActionResult.PASS, stack);
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer)
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        boolean used = use(stack, new PlayerTarget(player), player.capabilities.isCreativeMode);
        return new ActionResult<ItemStack>(used ? EnumActionResult.SUCCESS : EnumActionResult.PASS, stack);
    }
    private boolean likelyUseful(EntityPlayer player) {
        if (player.getCooldownTracker().hasCooldown(this)) return false;
        switch (kind) {
            case BANDAGE: return player.getHealth() > 0 && player.getHealth() < player.getMaxHealth();
            case ANTIDOTE: return player.isPotionActive(MobEffects.POISON) || player.isPotionActive(MobEffects.WITHER);
            case ESCAPE: return !player.isPotionActive(MobEffects.SPEED);
            default: return false;
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("tooltip.nexusrpg.supply." + kind.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private static final class PlayerTarget implements Target {
        private final EntityPlayer player;
        PlayerTarget(EntityPlayer player) { this.player = player; }
        public boolean cooldown(Item item) { return player.getCooldownTracker().hasCooldown(item); }
        public void cooldown(Item item, int ticks) { player.getCooldownTracker().setCooldown(item, ticks); }
        public boolean heal() {
            float before = player.getHealth();
            if (!(before > 0.0F) || !(before < player.getMaxHealth())) return false;
            player.heal(4.0F);
            return player.getHealth() > before;
        }
        public boolean cleanse() {
            boolean poison = player.isPotionActive(MobEffects.POISON);
            boolean wither = player.isPotionActive(MobEffects.WITHER);
            if (poison) player.removePotionEffect(MobEffects.POISON);
            if (wither) player.removePotionEffect(MobEffects.WITHER);
            return (poison && !player.isPotionActive(MobEffects.POISON)) || (wither && !player.isPotionActive(MobEffects.WITHER));
        }
        public boolean speed() {
            // Do not overwrite, shorten or extend another potion, beacon or mod's effect.
            if (player.isPotionActive(MobEffects.SPEED)) return false;
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 200, 0, false, true));
            return player.isPotionActive(MobEffects.SPEED);
        }
    }
}
