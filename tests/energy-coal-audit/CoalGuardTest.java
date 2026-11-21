package com.nexusrpg.energy;

import java.lang.reflect.Field;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import sun.misc.Unsafe;

public final class CoalGuardTest {
    static void check(boolean v, String s) { if (!v) throw new AssertionError(s); }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        Bootstrap.register(); TileEntity.register("nexusrpg:coal_guard_test", TileCoalGenerator.class);
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        CoalBreakPreservationTest.TestWorld world = (CoalBreakPreservationTest.TestWorld) ((Unsafe)f.get(null)).allocateInstance(CoalBreakPreservationTest.TestWorld.class);
        world.rules = new GameRules();
        BlockPowerMachine block = new BlockPowerMachine("coal_guard_test", false);
        TileCoalGenerator normal = new TileCoalGenerator();
        check(!normal.hasOpaqueRecoveryData(), "fresh generator unexpectedly protected");
        NBTTagCompound input = new NBTTagCompound(); input.setString("FuelInventory", "opaque");
        input.setInteger("Energy", 100001); input.setInteger("BurnTicks", 1601);
        TileCoalGenerator tile = new TileCoalGenerator(); tile.readFromNBT(input); world.tile = tile;
        check(tile.hasOpaqueRecoveryData(), "opaque data not protected");
        check(tile.guiEnergy() == 0 && tile.getBurnTicks() == 0, "invalid values became active");
        for (boolean drops : new boolean[] {false, true}) {
            world.rules.setOrCreateGameRule("doTileDrops", String.valueOf(drops));
            for (boolean harvest : new boolean[] {false, true}) {
                check(!block.removedByPlayer(block.getDefaultState(), world, BlockPos.ORIGIN, null, harvest), "player destruction not blocked");
                check(world.tile == tile, "player removal deleted tile");
            }
            NonNullList<ItemStack> explosionDrops = NonNullList.create();
            block.getDrops(explosionDrops, world, BlockPos.ORIGIN, block.getDefaultState(), 0);
            check(explosionDrops.isEmpty(), "explosion would duplicate protected block");
            block.onBlockExploded(world, BlockPos.ORIGIN, null);
            check(world.tile == tile, "explosion deleted tile");
        }
        NBTTagCompound save = tile.writeToNBT(new NBTTagCompound());
        for (int i=0; i<3; i++) { tile = new TileCoalGenerator(); tile.readFromNBT(save); save = tile.writeToNBT(new NBTTagCompound()); }
        check(tile.hasOpaqueRecoveryData() && "opaque".equals(save.getString("NexusRpgOriginalFuelInventory")), "archive did not survive save/reload");
        NBTTagCompound inv = new NBTTagCompound(); inv.setInteger("Size", 99);
        NBTTagList entries = new NBTTagList(); NBTTagCompound entry = new NBTTagCompound(); entry.setInteger("Slot", 0); new ItemStack(Items.COAL, 10).writeToNBT(entry); entries.appendTag(entry); inv.setTag("Items", entries);
        NBTTagCompound bad = new NBTTagCompound(); bad.setTag("FuelInventory", inv);
        TileCoalGenerator malformed = new TileCoalGenerator(); malformed.readFromNBT(bad);
        check(malformed.inventory.getStackInSlot(0).isEmpty(), "malformed inventory active");
        check(inv.equals(malformed.writeToNBT(new NBTTagCompound()).getTag("NexusRpgOriginalFuelInventory")), "malformed inventory lost");
        NBTTagCompound future = new NBTTagCompound(); future.setInteger("NexusRpgSchemaVersion", 1234); future.setInteger("Energy", 100); future.setInteger("BurnTicks", 200);
        NBTTagCompound futureInv = new NBTTagCompound(); futureInv.setInteger("Size", 1); futureInv.setTag("Items", entries.copy()); future.setTag("FuelInventory", futureInv.copy());
        TileCoalGenerator unknown = new TileCoalGenerator(); unknown.readFromNBT(future);
        check(unknown.guiEnergy() == 0 && unknown.getBurnTicks() == 0 && unknown.inventory.getStackInSlot(0).isEmpty(), "future state became active");
        check(!unknown.inventory.insertItem(0, new ItemStack(Items.COAL, 1), false).isEmpty(), "future machine accepted fuel");
        NBTTagCompound unknownSave = unknown.writeToNBT(new NBTTagCompound());
        // Future inventory remains opaque at its original schema key, not duplicated in an archive.
        check(futureInv.equals(unknownSave.getTag("FuelInventory")) && !unknownSave.hasKey("NexusRpgOriginalFuelInventory"), "future inventory changed or duplicated");
        TileCoalGenerator unknownAgain = new TileCoalGenerator(); unknownAgain.readFromNBT(unknownSave);
        check(unknownAgain.inventory.getStackInSlot(0).isEmpty() && unknownAgain.guiEnergy() == 0 && unknownAgain.hasOpaqueRecoveryData(), "future data reactivated after reload");
        TileCoalGenerator snapshot = new TileCoalGenerator(); snapshot.inventory.insertItem(0, new ItemStack(Items.COAL, 10), false); world.tile = snapshot; world.restoringBlockSnapshots = true;
        Field cf = Block.class.getDeclaredField("captureDrops"); cf.setAccessible(true); Field df = Block.class.getDeclaredField("capturedDrops"); df.setAccessible(true);
        ThreadLocal<Boolean> capture = (ThreadLocal<Boolean>) cf.get(null); ThreadLocal<NonNullList<ItemStack>> captured = (ThreadLocal<NonNullList<ItemStack>>) df.get(null);
        capture.set(true); captured.set(NonNullList.create());
        try { block.breakBlock(world, BlockPos.ORIGIN, block.getDefaultState()); check(snapshot.inventory.getStackInSlot(0).getCount() == 10 && captured.get().isEmpty(), "snapshot caused extraction or drops"); }
        finally { capture.remove(); captured.remove(); }
        System.out.println("PASS guarded removal/explosion; drops gamerule; invalid values/inventory; reload; snapshot extraction");
    }
}
