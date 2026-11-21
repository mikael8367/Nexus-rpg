package com.nexusrpg.energy;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Additional fail-closed and repeated-save checks. */
public final class CoalEdgeRegressionTest {
    static void check(boolean b, String s) { if (!b) throw new AssertionError(s); }
    static void expectFailure(TileCoalGenerator tile, String msg) {
        try { tile.writeToNBT(new NBTTagCompound()); throw new AssertionError(msg); }
        catch (IllegalStateException expected) { }
    }
    public static void main(String[] args) throws Exception {
        Bootstrap.register(); TileEntity.register("nexusrpg:coal_edge_regression", TileCoalGenerator.class);
        NBTTagCompound future = new NBTTagCompound(); future.setInteger("NexusRpgSchemaVersion", 9);
        TileCoalGenerator f = new TileCoalGenerator(); f.readFromNBT(future);
        try { f.inventory.setStackInSlot(0, new ItemStack(Items.COAL)); throw new AssertionError("future setter accepted fuel"); }
        catch (IllegalStateException expected) { }
        try { f.inventory.deserializeNBT(new NBTTagCompound()); throw new AssertionError("future inventory deserialized"); }
        catch (IllegalStateException expected) { }
        NBTTagCompound conflict = new NBTTagCompound(); conflict.setInteger("NexusRpgSchemaVersion", 10);
        try { f.writeToNBT(conflict); throw new AssertionError("conflicting future marker accepted"); }
        catch (IllegalStateException expected) { }
        NBTTagCompound out = f.writeToNBT(new NBTTagCompound());
        check(!out.hasKey("Energy") && !out.hasKey("BurnTicks") && !out.hasKey("FuelInventory"), "future absent keys added");
        f.readFromNBT(new NBTTagCompound());
        check(f.inventory.insertItem(0, new ItemStack(Items.COAL, 7), false).isEmpty(), "tile reuse remained locked");
        check(f.inventory.getStackInSlot(0).getCount() == 7, "valid fuel not active");

        NBTTagCompound wrongCaps = new NBTTagCompound(); wrongCaps.setString("ForgeCaps", "malformed");
        CoalCriticalRegressionTest.Provider p = new CoalCriticalRegressionTest.Provider();
        TileCoalGenerator cap = CoalCriticalRegressionTest.withProvider(p); cap.readFromNBT(wrongCaps); p.state = 17;
        expectFailure(cap, "malformed caps overwritten by live provider");
        check(p.state == 17 && "malformed".equals(wrongCaps.getString("ForgeCaps")), "live or raw caps mutated");

        NBTTagCompound wrongData = new NBTTagCompound(); wrongData.setInteger("ForgeData", 9);
        TileCoalGenerator data = new TileCoalGenerator(); data.readFromNBT(wrongData); data.getTileData().setString("live", "updated");
        expectFailure(data, "malformed ForgeData overwritten by live data");
        check("updated".equals(data.getTileData().getString("live")) && wrongData.getInteger("ForgeData") == 9, "live or raw ForgeData mutated");

        NBTTagCompound proper = new NBTTagCompound(); NBTTagCompound base = new NBTTagCompound(); base.setString("old", "value"); proper.setTag("ForgeData", base);
        TileCoalGenerator update = new TileCoalGenerator(); update.readFromNBT(proper); update.getTileData().setString("new", "changed");
        out = update.writeToNBT(new NBTTagCompound());
        check("value".equals(out.getCompoundTag("ForgeData").getString("old")) && "changed".equals(out.getCompoundTag("ForgeData").getString("new")), "live ForgeData update lost");
        check(!proper.getCompoundTag("ForgeData").hasKey("new"), "caller input mutated");
        System.out.println("PASS fail-closed malformed Forge collisions; live updates; future missing keys; reuse");
    }
}
