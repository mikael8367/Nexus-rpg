package com.nexusrpg.expedition;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
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

/** Disposable aids. No world mutation or persistent player data. */
public final class ItemExpeditionAid extends Item {
    public enum Kind { QUENCH, DIVE, SIGHT }
    static final int COOLDOWN_TICKS = 40;
    static final int EFFECT_TICKS = 600;
    private final Kind kind;
    public ItemExpeditionAid(Kind kind) {
        if (kind == null) throw new IllegalArgumentException("kind");
        this.kind = kind;
        setMaxStackSize(16);
    }
    interface Target {
        boolean cooling(Item item);
        void cooldown(Item item, int ticks);
        boolean burning();
        void extinguish();
        boolean underwater();
        boolean active(Kind kind);
        void apply(Kind kind, int ticks);
    }
    boolean use(ItemStack stack, Target target, boolean creative) {
        if (stack == null || stack.isEmpty() || stack.getItem() != this || target == null || target.cooling(this)) return false;
        boolean changed;
        switch (kind) {
            case QUENCH:
                if (!target.burning()) return false;
                target.extinguish();
                changed = !target.burning();
                break;
            case DIVE:
                if (!target.underwater() || target.active(kind)) return false;
                target.apply(kind, EFFECT_TICKS);
                changed = target.active(kind);
                break;
            case SIGHT:
                if (target.active(kind)) return false;
                target.apply(kind, EFFECT_TICKS);
                changed = target.active(kind);
                break;
            default: return false;
        }
        if (!changed) return false;
        target.cooldown(this, COOLDOWN_TICKS);
        if (!creative) stack.shrink(1);
        return true;
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.isEntityAlive() || player.isSpectator() || stack.isEmpty() || stack.getItem() != this)
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        if (world.isRemote) return new ActionResult<ItemStack>(likelyUseful(player) ? EnumActionResult.SUCCESS : EnumActionResult.PASS, stack);
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer)
            return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
        return new ActionResult<ItemStack>(use(stack, new PlayerTarget(player), player.capabilities.isCreativeMode)
            ? EnumActionResult.SUCCESS : EnumActionResult.PASS, stack);
    }
    private boolean likelyUseful(EntityPlayer player) {
        if (player.getCooldownTracker().hasCooldown(this)) return false;
        switch (kind) {
            case QUENCH: return player.isBurning();
            case DIVE: return player.isInWater() && !player.isPotionActive(MobEffects.WATER_BREATHING);
            case SIGHT: return !player.isPotionActive(MobEffects.NIGHT_VISION);
            default: return false;
        }
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("tooltip.nexusrpg.expedition." + kind.name().toLowerCase(Locale.ROOT)));
    }
    private static final class PlayerTarget implements Target {
        private final EntityPlayer player;
        PlayerTarget(EntityPlayer player) { this.player = player; }
        public boolean cooling(Item item) { return player.getCooldownTracker().hasCooldown(item); }
        public void cooldown(Item item, int ticks) { player.getCooldownTracker().setCooldown(item, ticks); }
        public boolean burning() { return player.isBurning(); }
        public void extinguish() { player.extinguish(); }
        public boolean underwater() { return player.isInWater(); }
        public boolean active(Kind kind) { return player.isPotionActive(effect(kind)); }
        public void apply(Kind kind, int ticks) { player.addPotionEffect(new PotionEffect(effect(kind), ticks, 0, false, true)); }
        private net.minecraft.potion.Potion effect(Kind kind) {
            if (kind == Kind.DIVE) return MobEffects.WATER_BREATHING;
            if (kind == Kind.SIGHT) return MobEffects.NIGHT_VISION;
            throw new IllegalArgumentException("No effect for " + kind);
        }
    }
}
