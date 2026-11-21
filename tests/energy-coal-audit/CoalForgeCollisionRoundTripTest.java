package com.nexusrpg.energy;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Forge headless regression: inert archives never become live state. */
public final class CoalForgeCollisionRoundTripTest {
    static void check(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    static int count(NBTTagCompound n, String prefix, String key, NBTBase value) {
        int result = 0;
        for (String k : n.getKeySet()) if ((k.equals(prefix) || k.startsWith(prefix + "_")) && n.hasKey(k, 10)) {
            NBTTagCompound c = n.getCompoundTag(k);
            if (key.equals(c.getString("Key")) && value.equals(c.getTag("Value"))) result++;
        }
        return result;
    }
    static void bothCollisions() throws Exception {
        NBTTagCompound original = new NBTTagCompound();
        original.setString("ForgeData", "raw-data"); original.setInteger("ForgeCaps", 714);
        NBTBase rawData = original.getTag("ForgeData").copy(), rawCaps = original.getTag("ForgeCaps").copy();
        NBTTagCompound occupied = new NBTTagCompound(); occupied.setString("prior", "opaque");
        original.setTag("NexusRpgForgeCollision", occupied.copy());
        CoalCriticalRegressionTest.Provider p = new CoalCriticalRegressionTest.Provider();
        TileCoalGenerator tile = CoalCriticalRegressionTest.withProvider(p);
        tile.readFromNBT(original); p.state = 37; tile.getTileData().setInteger("othermod", 42);
        NBTTagCompound before = original.copy();
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        for (int i = 0; i < 5; i++) {
            check(saved.equals(tile.writeToNBT(new NBTTagCompound())), "same-instance save " + i);
            check(count(saved,"NexusRpgForgeCollision","ForgeData",rawData)==1, "data archive not unique");
            check(count(saved,"NexusRpgForgeCollision","ForgeCaps",rawCaps)==1, "caps archive not unique");
            check(occupied.equals(saved.getTag("NexusRpgForgeCollision")), "occupied root changed");
            check(saved.getCompoundTag("ForgeData").getInteger("othermod")==42, "active data lost");
            check(saved.getCompoundTag("ForgeCaps").getInteger("othermod:live")==37, "live cap lost");
            check(tile.hasOpaqueRecoveryData(), "archive not protected");
            CoalCriticalRegressionTest.Provider next = new CoalCriticalRegressionTest.Provider();
            TileCoalGenerator reloaded = CoalCriticalRegressionTest.withProvider(next); reloaded.readFromNBT(saved);
            check(next.state==37 && reloaded.getTileData().getInteger("othermod")==42, "reload live state");
            NBTTagCompound again = reloaded.writeToNBT(new NBTTagCompound());
            check(saved.equals(again), "reload/save " + i);
            tile = reloaded; saved = again;
        }
        check(original.equals(before), "input changed");
    }
    static void callerCollision() {
        NBTTagCompound input = new NBTTagCompound(); input.setInteger("NexusRpgSchemaVersion", 99);
        input.setInteger("Energy", 56); input.setString("ForgeData", "raw");
        TileCoalGenerator tile = new TileCoalGenerator(); tile.readFromNBT(input);
        tile.getTileData().setString("active", "yes");
        NBTTagCompound caller = new NBTTagCompound(); caller.setInteger("NexusRpgSchemaVersion", 100);
        caller.setInteger("Energy", 57); caller.setInteger("BurnTicks", 77); caller.setString("ForgeData", "caller");
        NBTTagCompound inCopy = caller.copy(); NBTTagCompound out = tile.writeToNBT(caller);
        check(out.getInteger("NexusRpgSchemaVersion")==99 && out.getInteger("Energy")==56 && !out.hasKey("BurnTicks"), "future roots changed");
        for (String key : inCopy.getKeySet()) check(count(out,"NexusRpgCallerCollision",key,inCopy.getTag(key))==1, "caller root lost " + key);
        check(count(out,"NexusRpgForgeCollision","ForgeData",input.getTag("ForgeData"))==1, "raw ForgeData missing");
        check("yes".equals(out.getCompoundTag("ForgeData").getString("active")), "caller shadowed live data");
        TileCoalGenerator next = new TileCoalGenerator(); next.readFromNBT(out);
        check(next.guiEnergy()==0 && next.getBurnTicks()==0 && next.inventory.getStackInSlot(0).isEmpty(), "archive activated");
        check(out.equals(next.writeToNBT(new NBTTagCompound())), "caller archive reload changed");
    }
    static void equalCaller() {
        TileCoalGenerator tile = new TileCoalGenerator(); NBTTagCompound out = tile.writeToNBT(new NBTTagCompound());
        NBTTagCompound caller = new NBTTagCompound(); caller.setTag("Energy", out.getTag("Energy").copy());
        caller.setTag("FuelInventory", out.getTag("FuelInventory").copy());
        check(!tile.writeToNBT(caller).hasKey("NexusRpgCallerCollision"), "equal caller archived");
    }
    public static void main(String[] args) throws Exception {
        Bootstrap.register(); TileEntity.register("nexusrpg:coal_collision_roundtrip", TileCoalGenerator.class);
        bothCollisions(); callerCollision(); equalCaller();
        System.out.println("PASS both collisions, occupied keys, reloads, caller collisions, no archive reactivation");
    }
}
