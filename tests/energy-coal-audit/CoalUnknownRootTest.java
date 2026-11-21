package com.nexusrpg.energy;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

/** Independently reports lost unknown roots and incompatible known-field data. */
public final class CoalUnknownRootTest {
    static boolean contains(NBTBase tree, NBTBase expected) {
        if (tree == null) return false;
        if (tree.equals(expected)) return true;
        if (tree instanceof NBTTagCompound) {
            NBTTagCompound c = (NBTTagCompound) tree;
            for (String key : c.getKeySet()) if (contains(c.getTag(key), expected)) return true;
        } else if (tree instanceof NBTTagList) {
            NBTTagList l = (NBTTagList) tree;
            for (int i = 0; i < l.tagCount(); i++) if (contains(l.get(i), expected)) return true;
        }
        return false;
    }
    static NBTTagCompound roundTrip(NBTTagCompound input) {
        TileCoalGenerator tile = new TileCoalGenerator();
        tile.readFromNBT(input);
        return tile.writeToNBT(new NBTTagCompound());
    }
    public static void main(String[] args) {
        Bootstrap.register();
        TileEntity.register("nexusrpg:coal_unknown_audit", TileCoalGenerator.class);
        int failures = 0;
        NBTTagCompound input = new NBTTagCompound();
        NBTTagCompound future = new NBTTagCompound();
        future.setInteger("schema", 9000);
        future.setString("future_payload", "must survive");
        input.setTag("OtherModFutureState", future.copy());
        input.setString("NexusRpgSchemaVersion", "future-version");
        NBTTagCompound output = input;
        for (int i = 0; i < 3; i++) {
            output = roundTrip(output);
            if (!future.equals(output.getTag("OtherModFutureState"))
                    || !"future-version".equals(output.getString("NexusRpgSchemaVersion"))) {
                System.err.println("FAIL unknown root/future NBT lost on save " + i);
                failures++;
                break;
            }
        }
        NBTTagCompound invalid = new NBTTagCompound();
        invalid.setString("Energy", "unknown future energy encoding");
        invalid.setString("BurnTicks", "unknown future timer encoding");
        output = roundTrip(invalid);
        if (!contains(output, invalid.getTag("Energy"))) {
            System.err.println("FAIL incompatible Energy NBT lost"); failures++;
        }
        if (!contains(output, invalid.getTag("BurnTicks"))) {
            System.err.println("FAIL incompatible BurnTicks NBT lost"); failures++;
        }
        NBTTagCompound range = new NBTTagCompound();
        range.setInteger("Energy", 100001); range.setInteger("BurnTicks", -1);
        NBTTagCompound rangeOut = roundTrip(range);
        if (!contains(rangeOut, range.getTag("Energy")) || !contains(rangeOut, range.getTag("BurnTicks"))) {
            System.err.println("FAIL out-of-range known fields lost"); failures++;
        }
        NBTTagCompound collision = new NBTTagCompound(); collision.setString("OtherModFutureState", "caller");
        TileCoalGenerator loaded = new TileCoalGenerator(); loaded.readFromNBT(input);
        NBTTagCompound collOut = loaded.writeToNBT(collision);
        if (!"caller".equals(collOut.getString("OtherModFutureState")) || !contains(collOut, future)) {
            System.err.println("FAIL colliding unknown root lost"); failures++;
        }
        loaded.readFromNBT(new NBTTagCompound());
        if (loaded.writeToNBT(new NBTTagCompound()).hasKey("OtherModFutureState")) {
            System.err.println("FAIL unknown roots leaked to reused tile"); failures++;
        }
        if (failures != 0) throw new AssertionError(failures + " preservation failures");
        System.out.println("PASS unknown root, future version, incompatible known fields, range, collisions, reuse");
    }
}
