package com.nexusrpg.energy;

import java.lang.reflect.Field;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import sun.misc.Unsafe;

public final class CoalRecoveryCarrierTest {
    static void check(boolean b, String s) { if (!b) throw new AssertionError(s); }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        Bootstrap.register(); TileEntity.register("nexusrpg:coal_carrier_test", TileCoalGenerator.class);
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        CoalBreakPreservationTest.TestWorld world = (CoalBreakPreservationTest.TestWorld) ((Unsafe)f.get(null)).allocateInstance(CoalBreakPreservationTest.TestWorld.class);
        world.rules = new GameRules();
        BlockPowerMachine block = new BlockPowerMachine("coal_carrier_test", false);
        Field cf = Block.class.getDeclaredField("captureDrops"); cf.setAccessible(true);
        Field df = Block.class.getDeclaredField("capturedDrops"); df.setAccessible(true);
        ThreadLocal<Boolean> capture = (ThreadLocal<Boolean>) cf.get(null);
        ThreadLocal<NonNullList<ItemStack>> captured = (ThreadLocal<NonNullList<ItemStack>>) df.get(null);
        capture.set(true); captured.set(NonNullList.create());
        try {
            TileCoalGenerator tile = new TileCoalGenerator();
            NBTTagCompound tag = new NBTTagCompound(); tag.setString("otherModData", "opaque");
            tile.readFromNBT(tag); tile.inventory.insertItem(0, new ItemStack(Items.COAL, 10), false);
            NBTTagCompound original = tile.writeToNBT(new NBTTagCompound());
            world.tile = tile; block.breakBlock(world, BlockPos.ORIGIN, block.getDefaultState());
            check(world.tile == null && captured.get().size() == 1, "expected exactly one carrier and no fuel drops");
            ItemStack item = captured.get().get(0);
            check(item.getItem() == BlockPowerMachine.COAL_RECOVERY && !(item.getItem() instanceof ItemBlock), "not inert dedicated item");
            check(item.getMaxStackSize() == 1 && item.getCount() == 1, "carrier stackable");
            check(!item.getTagCompound().hasKey("BlockEntityTag") && !item.getTagCompound().hasKey("ForgeCaps"), "unsafe root payload");
            check(original.equals(item.getTagCompound().getCompoundTag("NexusRpgCoalRecovery")), "not a lossless nested tile payload");
            check(tile.inventory.getStackInSlot(0).getCount() == 10, "separately extracted live inventory");
            for (boolean opaque : new boolean[]{false, true}) for (int mode=0; mode<3; mode++) {
                TileCoalGenerator next = new TileCoalGenerator();
                if (opaque) next.readFromNBT(tag);
                next.inventory.insertItem(0, new ItemStack(Items.COAL, 10), false);
                world.tile = next; captured.get().clear();
                world.captureBlockSnapshots = mode != 1; world.restoringBlockSnapshots = mode != 0;
                check(!block.removedByPlayer(block.getDefaultState(), world, BlockPos.ORIGIN, null, false), "snapshot player guard");
                NonNullList<ItemStack> drops = NonNullList.create();
                block.getDrops(drops, world, BlockPos.ORIGIN, block.getDefaultState(), 0);
                check(drops.isEmpty(), "snapshot getDrops created a block");
                block.onBlockExploded(world, BlockPos.ORIGIN, null);
                check(world.tile == next, "snapshot explosion guard");
                block.breakBlock(world, BlockPos.ORIGIN, block.getDefaultState());
                check(captured.get().isEmpty() && next.inventory.getStackInSlot(0).getCount() == 10, "snapshot emitted carrier or inventory");
            }
            world.captureBlockSnapshots = false; world.restoringBlockSnapshots = false;
            TileCoalGenerator disabled = new TileCoalGenerator(); disabled.readFromNBT(tag); world.tile = disabled;
            world.rules.setOrCreateGameRule("doTileDrops", "false"); captured.get().clear();
            check(!block.removedByPlayer(block.getDefaultState(), world, BlockPos.ORIGIN, null, false), "disabled drops must block player removal");
            block.onBlockExploded(world, BlockPos.ORIGIN, null); check(world.tile == disabled, "disabled drops must block explosion");
            block.breakBlock(world, BlockPos.ORIGIN, block.getDefaultState());
            check(captured.get().isEmpty() && world.tile == null, "forced disabled drops caveat");
            System.out.println("PASS inert complete carrier, no active drop, snapshot suppression, guarded no-drops; forced no-drops loss documented");
        } finally { capture.remove(); captured.remove(); }
    }
}
