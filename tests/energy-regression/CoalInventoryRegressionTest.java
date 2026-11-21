package com.nexusrpg.energy;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/** Real Forge item/NBT classes. Malformed/legacy contents must not silently disappear. */
public final class CoalInventoryRegressionTest {
    private static void check(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    private static NBTTagCompound inventory(ItemStack stack, int size) {
        NBTTagCompound out = new NBTTagCompound(); out.setInteger("Size", size);
        NBTTagList list = new NBTTagList();
        NBTTagCompound item = new NBTTagCompound(); item.setInteger("Slot", 0);
        stack.writeToNBT(item); list.appendTag(item); out.setTag("Items", list); return out;
    }
    public static void main(String[] args) {
        Bootstrap.register();
        TileEntity.register("nexusrpg:test_coal", TileCoalGenerator.class);
        NBTTagCompound metadata = new NBTTagCompound(); metadata.setString("custom", "retain me");
        ItemStack diamond = new ItemStack(Items.DIAMOND, 3); diamond.setTagCompound(metadata);
        NBTTagCompound input = new NBTTagCompound(); input.setTag("FuelInventory", inventory(diamond, 1));
        TileCoalGenerator gen = new TileCoalGenerator(); gen.readFromNBT(input);
        ItemStack loaded = gen.inventory.getStackInSlot(0);
        check(loaded.getItem() == Items.DIAMOND && loaded.getCount() == 3 && metadata.equals(loaded.getTagCompound()), "legacy non-fuel item/NBT lost");
        check(gen.inventory.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1, "non-fuel inserted");
        NBTTagCompound saved = gen.writeToNBT(new NBTTagCompound());
        TileCoalGenerator again = new TileCoalGenerator(); again.readFromNBT(saved);
        check(again.inventory.extractItem(0, 64, false).getCount() == 3, "item not recoverable after reload");
        NBTTagCompound malformed = inventory(new ItemStack(Items.COAL, 100), 99);
        NBTTagCompound badInput = new NBTTagCompound(); badInput.setTag("FuelInventory", malformed.copy());
        TileCoalGenerator bad = new TileCoalGenerator(); bad.readFromNBT(badInput);
        NBTTagCompound badSaved = bad.writeToNBT(new NBTTagCompound());
        check(malformed.equals(badSaved.getCompoundTag("NexusRpgOriginalFuelInventory")), "malformed original not preserved");
        TileCoalGenerator badReload = new TileCoalGenerator(); badReload.readFromNBT(badSaved);
        check(malformed.equals(badReload.writeToNBT(new NBTTagCompound()).getCompoundTag("NexusRpgOriginalFuelInventory")), "backup lost on second save");
        check(malformed.equals(badInput.getCompoundTag("FuelInventory")), "input mutated");
        NBTTagCompound wrongList = new NBTTagCompound(); wrongList.setInteger("Size", 1);
        NBTTagList strings = new NBTTagList(); strings.appendTag(new net.minecraft.nbt.NBTTagString("not an item"));
        wrongList.setTag("Items", strings);
        NBTTagCompound wrongInput = new NBTTagCompound(); wrongInput.setTag("FuelInventory", wrongList.copy());
        TileCoalGenerator wrong = new TileCoalGenerator(); wrong.readFromNBT(wrongInput);
        check(wrongList.equals(wrong.writeToNBT(new NBTTagCompound()).getCompoundTag("NexusRpgOriginalFuelInventory")), "wrong list type lost");
        TileCoalGenerator normal = new TileCoalGenerator();
        check(normal.inventory.insertItem(0, new ItemStack(Items.COAL, 12, 1), false).isEmpty(), "charcoal rejected");
        NBTTagCompound normalSaved = normal.writeToNBT(new NBTTagCompound());
        check(!normalSaved.hasKey("NexusRpgOriginalFuelInventory"), "normal inventory unnecessarily backed up");
        TileCoalGenerator normalAgain = new TileCoalGenerator(); normalAgain.readFromNBT(normalSaved);
        check(normalAgain.inventory.extractItem(0, 64, false).getCount() == 12, "normal charcoal lost");
        System.out.println("PASS coal inventory: recovery, rejection, malformed NBT and reload");
    }
}
