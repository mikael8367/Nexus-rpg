package com.nexusrpg.energy;

import java.lang.reflect.Field;
import java.util.Collections;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

/** Regression specifications. Run separately until all three bugs are fixed. */
public final class CoalCriticalRegressionTest {
    interface Test { void run() throws Exception; }
    static int failures;
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
    static void run(String name, Test test) {
        try { test.run(); System.out.println("PASS " + name); }
        catch (Throwable e) { failures++; System.err.println("FAIL " + name + ": " + e); }
    }
    static NBTTagCompound save(TileCoalGenerator t) { return t.writeToNBT(new NBTTagCompound()); }
    static TileCoalGenerator load(NBTTagCompound n) { TileCoalGenerator t = new TileCoalGenerator(); t.readFromNBT(n); return t; }
    static NBTTagCompound fuel() {
        TileCoalGenerator t = new TileCoalGenerator();
        t.inventory.insertItem(0, new ItemStack(Items.COAL, 12), false);
        return t.inventory.serializeNBT();
    }
    static void future() {
        NBTTagCompound in = new NBTTagCompound();
        in.setInteger("NexusRpgSchemaVersion", 999);
        in.setInteger("Energy", 9876); in.setInteger("BurnTicks", 1234);
        in.setTag("FuelInventory", fuel());
        NBTTagCompound out = in.copy();
        for (int i = 0; i < 4; i++) {
            TileCoalGenerator t = load(out);
            check(t.guiEnergy() == 0 && t.getBurnTicks() == 0 && t.inventory.getStackInSlot(0).isEmpty(), "future fields active");
            check(!t.inventory.insertItem(0, new ItemStack(Items.COAL, 1), false).isEmpty(), "future state accepted fuel");
            out = save(t);
            NBTTagCompound secondSave = save(t);
            for (String key : new String[]{"NexusRpgSchemaVersion", "Energy", "BurnTicks", "FuelInventory"}) {
                check(in.getTag(key).equals(out.getTag(key)), "original " + key + " changed on save " + i);
                check(in.getTag(key).equals(secondSave.getTag(key)), "original " + key + " changed on repeated same-instance save " + i);
            }
        }
    }
    static void divergentInventory() {
        NBTTagCompound raw = fuel(); raw.setString("unknown_future_item_metadata", "retain");
        NBTTagCompound in = new NBTTagCompound(); in.setTag("FuelInventory", raw.copy());
        for (int i = 0; i < 4; i++) {
            TileCoalGenerator t = load(in);
            check(t.inventory.getStackInSlot(0).isEmpty(), "archived item also active on load " + i);
            in = save(t);
            check(raw.equals(in.getTag("NexusRpgOriginalFuelInventory")), "original inventory not archived");
            check(!raw.equals(in.getTag("FuelInventory")), "archive also written as live inventory");
        }
    }
    static final class Provider implements ICapabilitySerializable<NBTTagInt> {
        int state;
        public boolean hasCapability(Capability<?> c, EnumFacing f) { return false; }
        public <T> T getCapability(Capability<T> c, EnumFacing f) { return null; }
        public NBTTagInt serializeNBT() { return new NBTTagInt(state); }
        public void deserializeNBT(NBTTagInt n) { state = n.getInt(); }
    }
    static TileCoalGenerator withProvider(Provider p) throws Exception {
        TileCoalGenerator t = new TileCoalGenerator();
        Field field = TileEntity.class.getDeclaredField("capabilities"); field.setAccessible(true);
        field.set(t, new CapabilityDispatcher(Collections.<ResourceLocation, ICapabilityProvider>singletonMap(new ResourceLocation("othermod", "live"), p)));
        return t;
    }
    static void forgeUnknown() throws Exception {
        NBTTagCompound in = new NBTTagCompound(), caps = new NBTTagCompound();
        caps.setInteger("othermod:live", 41); caps.setString("absentmod:unknown", "opaque");
        in.setTag("ForgeCaps", caps); NBTTagCompound data = new NBTTagCompound(); data.setString("othermod", "data"); in.setTag("ForgeData", data);
        for (int i = 0; i < 3; i++) {
            Provider p = new Provider(); TileCoalGenerator t = withProvider(p); t.readFromNBT(in);
            check(p.state == 41, "attached capability failed to load");
            p.state = 42; NBTTagCompound out = save(t);
            check(data.equals(out.getTag("ForgeData")), "ForgeData lost");
            check("opaque".equals(out.getCompoundTag("ForgeCaps").getString("absentmod:unknown")), "unknown capability lost");
            check(out.getCompoundTag("ForgeCaps").getInteger("othermod:live") == 42, "live capability stale or duplicated");
            check(!out.hasKey("NexusRpgOriginalForgeCaps"), "entire capability map copied as opaque archive");
            in = out; in.getCompoundTag("ForgeCaps").setInteger("othermod:live", 41);
        }
    }
    static void forgeWrongType(String key) {
        NBTTagCompound in = new NBTTagCompound();
        if (key.equals("ForgeData")) in.setString(key, "not a compound");
        else in.setInteger(key, 76543);
        NBTBase expected = in.getTag(key).copy();
        for (int i = 0; i < 3; i++) {
            NBTTagCompound out = save(load(in));
            check(expected.equals(out.getTag(key)), "invalid " + key + " lost on save " + i);
            in = out;
        }
    }
    public static void main(String[] args) {
        Bootstrap.register(); TileEntity.register("nexusrpg:coal_critical_regression", TileCoalGenerator.class);
        run("future fields remain passive under their original keys", () -> future());
        run("divergent inventory is exclusively opaque", () -> divergentInventory());
        run("unknown capability plus live attached capability", () -> forgeUnknown());
        run("invalid ForgeData type", () -> forgeWrongType("ForgeData"));
        run("invalid ForgeCaps type", () -> forgeWrongType("ForgeCaps"));
        if (failures != 0) throw new AssertionError(failures + " critical regression(s)");
    }
}
