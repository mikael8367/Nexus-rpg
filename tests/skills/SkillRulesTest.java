package com.nexusrpg.skills;

import java.lang.reflect.Method;
import net.minecraft.nbt.NBTTagCompound;

/** Lightweight rule checks; not a substitute for a Minecraft runtime test. */
public final class SkillRulesTest {
    public static void main(String[] args) throws Exception {
        Method points = SkillEvents.class.getDeclaredMethod("points", NBTTagCompound.class, int.class);
        Method rank = SkillEvents.class.getDeclaredMethod("rank", NBTTagCompound.class, String.class);
        Method known = SkillEvents.class.getDeclaredMethod("known", String.class);
        points.setAccessible(true); rank.setAccessible(true); known.setAccessible(true);
        NBTTagCompound data = new NBTTagCompound();
        check((Integer) points.invoke(null, data, 1) == 0, "level 1");
        check((Integer) points.invoke(null, data, 3) == 0, "level 3");
        check((Integer) points.invoke(null, data, 4) == 1, "level 4");
        check((Integer) points.invoke(null, data, 16) == 5, "level 16");
        data.setInteger("rank_warrior", 2); data.setInteger("rank_mage", 1);
        check((Integer) points.invoke(null, data, 16) == 2, "global spending across classes");
        check((Integer) points.invoke(null, data, 4) == 0, "no negative points");
        data.setInteger("rank_warrior", 100);
        check((Integer) rank.invoke(null, data, "warrior") == 5, "rank cap");
        data.setInteger("rank_warrior", -2);
        check((Integer) rank.invoke(null, data, "warrior") == 0, "negative rank");
        check((Boolean) known.invoke(null, "necromancer"), "known class");
        check(!(Boolean) known.invoke(null, "admin"), "unknown class");
        check(!(Boolean) known.invoke(null, new Object[]{null}), "null class");
        check((Integer) points.invoke(null, new NBTTagCompound(), 1000000) == 333333, "high level points");
        check((Integer) points.invoke(null, new NBTTagCompound(), Integer.MAX_VALUE) == 715827882, "integer boundary");
        Method clear = SkillEvents.class.getDeclaredMethod("clearCombos", NBTTagCompound.class);
        clear.setAccessible(true);
        data.setLong("combo_start_mage", 10); data.setLong("combo_until_mage", 130);
        data.setString("combo_target_mage", "target");
        data.setLong("pal_combo_until", 130); data.setLong("cast_mage", 170);
        data.setLong("ultimate_mage", 1210); data.setLong("leech_ready", 210);
        data.setInteger("rank_mage", 3);
        clear.invoke(null, data);
        check(!data.hasKey("combo_start_mage") && !data.hasKey("combo_until_mage")
            && !data.hasKey("combo_target_mage") && !data.hasKey("pal_combo_until"), "combos cleared");
        check(data.getLong("cast_mage") == 170 && data.getLong("ultimate_mage") == 1210
            && data.getLong("leech_ready") == 210 && data.getInteger("rank_mage") == 3,
            "death preserves cooldowns and rank");
        // Exercise the exact copy used by the clone handler, including non-death clones.
        NBTTagCompound source=new NBTTagCompound(), dest=new NBTTagCompound();
        source.setInteger("rank_warrior",2); source.setLong("cast_warrior",170);
        source.setLong("combo_start_warrior",10); source.setLong("combo_until_warrior",130);
        source.setString("combo_target_warrior","target");
        NBTTagCompound oldTree=new NBTTagCompound(); oldTree.setInteger("warrior.active",2);
        source.setTag("tree_v1",oldTree);
        dest.setInteger("rank_warrior",5); dest.setString("unrelated_destination","kept");
        NBTTagCompound destTree=new NBTTagCompound(); destTree.setString("future.data","kept");
        dest.setTag("tree_v1",destTree);
        NBTTagCompound sourceBefore=source.copy(), destBefore=dest.copy();
        NBTTagCompound deathCopy=SkillEvents.cloneSkills(source,dest,true);
        check(deathCopy.getInteger("rank_warrior")==2 && deathCopy.getLong("cast_warrior")==170, "old authoritative skills and timer");
        check(deathCopy.getCompoundTag("tree_v1").getInteger("warrior.active")==2
            && deathCopy.getCompoundTag("tree_v1").getString("future.data").equals("kept")
            && deathCopy.getString("unrelated_destination").equals("kept"),"preserve both sides' distinct tags");
        check(!deathCopy.hasKey("combo_until_warrior") && source.equals(sourceBefore) && dest.equals(destBefore),"death clears copied combos only");
        check(SkillEvents.cloneSkills(source,null,false).getLong("combo_until_warrior")==130,"non-death clone retains combos");
        source.setString("combo_until_warrior","corrupt");
        check(SkillEvents.cloneSkills(source,null,true).equals(source),"malformed combo preserved through death");
        check(Math.abs(SkillEvents.boostedDamage(100F,0.1F,2,1)-115F)<0.001F,"legacy and tree damage bonuses");
        check(SkillEvents.boostedDamage(Float.MAX_VALUE,0.15F,5,3)==Float.MAX_VALUE,"damage saturates, not infinity");
        System.out.println("Skill rule checks passed");
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
