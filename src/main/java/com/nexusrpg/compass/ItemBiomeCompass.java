package com.nexusrpg.compass;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemBiomeCompass extends Item {
    public ItemBiomeCompass() { setMaxStackSize(1); }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) openScreen();
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
    @SideOnly(Side.CLIENT)
    private void openScreen() { CompassClient.open(); }
}
