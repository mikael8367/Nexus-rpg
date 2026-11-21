package com.nexusrpg.energy;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;

public final class CoalNbtAuditTest {
    static final String ARCHIVE = "NexusRpgOriginalFuelInventory";
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static TileCoalGenerator load(NBTTagCompound input) {
        TileCoalGenerator tile = new TileCoalGenerator(); tile.readFromNBT(input); return tile;
    }
    static NBTTagCompound save(TileCoalGenerator tile) { return tile.writeToNBT(new NBTTagCompound()); }
    static boolean contains(NBTTagCompound tag, NBTBase value) {
        for (String key : tag.getKeySet())
            if (key.startsWith(ARCHIVE) && value.equals(tag.getTag(key))) return true;
        return false;
    }
    static int archives(NBTTagCompound tag) {
        int n = 0; for (String key : tag.getKeySet()) if (key.startsWith(ARCHIVE)) n++; return n;
    }
    public static void main(String[] args) {
        Bootstrap.register(); TileEntity.register("nexusrpg:coal_nbt_audit", TileCoalGenerator.class);
        NBTTagCompound normal = save(new TileCoalGenerator());
        check(archives(save(load(normal))) == 0, "normal empty inventory backed up");
        TileCoalGenerator fueled = new TileCoalGenerator();
        fueled.inventory.insertItem(0, new ItemStack(Items.COAL, 12, 1), false);
        NBTTagCompound good = save(fueled); NBTTagCompound goodAgain = save(load(good));
        check(archives(goodAgain) == 0 && load(goodAgain).inventory.getStackInSlot(0).getCount() == 12, "normal fuel or backup");

        NBTTagCompound wrong = new NBTTagCompound(); wrong.setString("FuelInventory", "recoverable");
        NBTBase raw = wrong.getTag("FuelInventory").copy();
        NBTTagCompound result = save(load(wrong)); check(contains(result, raw), "wrong root type lost");
        check(contains(save(load(result)), raw), "wrong root lost after reload");
        check(raw.equals(wrong.getTag("FuelInventory")), "input mutated");

        NBTTagCompound extra = good.copy();
        extra.getCompoundTag("FuelInventory").setString("foreign_metadata", "retain");
        NBTTagCompound originalExtra = extra.getCompoundTag("FuelInventory").copy();
        result = save(load(extra)); check(contains(result, originalExtra), "extra field lost");
        // A divergent inventory is archived as one opaque unit. Activating its
        // fuel as well would duplicate the same item; undiverged fuel above stays live.
        check(load(result).inventory.getStackInSlot(0).isEmpty(), "divergent fuel both live and archived");
        for (int i = 0; i < 5; i++) result = save(load(result));
        check(contains(result, originalExtra) && archives(result) == 1, "repeated reload loses or duplicates backup");

        NBTTagCompound multiple = new NBTTagCompound(); NBTTagCompound malformed = new NBTTagCompound();
        malformed.setInteger("Size", 99); malformed.setString("new-data", "retain");
        multiple.setTag("FuelInventory", malformed.copy()); multiple.setString(ARCHIVE, "old wrong-type backup");
        multiple.setInteger(ARCHIVE + "_1", 42);
        result = save(load(multiple));
        check(result.getString(ARCHIVE).equals("old wrong-type backup") && result.getInteger(ARCHIVE + "_1") == 42, "prior backups overwritten");
        check(contains(result, malformed), "new malformed data lost with old backups");
        NBTTagCompound second = save(load(result));
        check(second.equals(result), "backups not stable across reload");

        NBTTagCompound listInput = new NBTTagCompound(); NBTTagCompound wrongList = new NBTTagCompound();
        wrongList.setInteger("Size", 1); NBTTagList strings = new NBTTagList(); strings.appendTag(new NBTTagString("not an item"));
        wrongList.setTag("Items", strings); listInput.setTag("FuelInventory", wrongList.copy());
        check(contains(save(load(listInput)), wrongList), "wrong list type lost");

        NBTTagCompound weird = new NBTTagCompound(); weird.setInteger("Size", 1);
        NBTTagList entries = new NBTTagList();
        NBTTagCompound missingItem = new NBTTagCompound(); missingItem.setInteger("Slot", 0); missingItem.setString("id", "not valid:name::!"); missingItem.setByte("Count", (byte) 3);
        entries.appendTag(missingItem); weird.setTag("Items", entries);
        NBTTagCompound weirdInput = new NBTTagCompound(); weirdInput.setTag("FuelInventory", weird.copy());
        check(contains(save(load(weirdInput)), weird), "unrecognized item lost");

        NBTTagCompound target = new NBTTagCompound(); target.setString(ARCHIVE, "caller data");
        NBTTagCompound collision = load(multiple).writeToNBT(target);
        check(collision.getString(ARCHIVE).equals("caller data") && contains(collision, new NBTTagString("old wrong-type backup")) && contains(collision, malformed), "output collision lost backup");
        TileCoalGenerator reused = load(multiple); reused.readFromNBT(good);
        check(archives(save(reused)) == 0, "prior data leaked when reused for another tile");
        System.out.println("PASS coal NBT audit: types, extra fields, prior archives, collisions, repeat saves, live fuel");
    }
}
