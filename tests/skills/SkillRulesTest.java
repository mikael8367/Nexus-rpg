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
        SkillEvents.OnePerTick<Object> limiter = new SkillEvents.OnePerTick<>();
        Object first = new Object(), second = new Object();
        check(!limiter.admit(null, 0) && !limiter.admit(first, -1), "invalid rate limit input rejected");
        check(limiter.admit(first, 0), "first attempt allowed even at tick zero");
        for (int i=0; i<10000; i++) check(!limiter.admit(first, 0), "same-tick burst rejected");
        check(limiter.admit(second, 0), "other player unaffected");
        check(limiter.admit(first, 1) && !limiter.admit(first, 1), "new tick has one attempt");
        limiter.succeeded(first, 0);
        check(!limiter.admit(first, 1), "stale success does not clear newer pending attempt");
        limiter.succeeded(first, 1);
        check(limiter.admit(first, 1) && !limiter.admit(first, 1), "successful active permits same-tick ultimate, then bounds failure");
        check(limiter.admit(first, 0) && !limiter.admit(first, 0), "world clock reset remains bounded");
        check(limiter.admit(first, Long.MAX_VALUE), "large tick supported");
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
        // Exercise the snapshot and transient-combo rules; root-level backups are tested separately.
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
        NBTTagCompound deathCopy=cloneSkills(source,dest,true);
        check(deathCopy.getInteger("rank_warrior")==2 && deathCopy.getLong("cast_warrior")==170, "old authoritative skills and timer");
        check(deathCopy.getCompoundTag("tree_v1").getInteger("warrior.active")==2
            && !deathCopy.getCompoundTag("tree_v1").hasKey("future.data")
            && !deathCopy.hasKey("unrelated_destination"),"do not combine independently allocated snapshots");
        check(!deathCopy.hasKey("combo_until_warrior") && source.equals(sourceBefore) && dest.equals(destBefore),"death clears copied combos only");
        check(cloneSkills(source,null,false).getLong("combo_until_warrior")==130,"non-death clone retains combos");
        source.setString("combo_until_warrior","corrupt");
        check(cloneSkills(source,null,true).equals(source),"malformed combo preserved through death");
        check(Math.abs(SkillEvents.boostedDamage(100F,0.1F,2,1)-115F)<0.001F,"legacy and tree damage bonuses");
        check(SkillEvents.boostedDamage(Float.MAX_VALUE,0.15F,5,3)==Float.MAX_VALUE,"damage saturates, not infinity");
        int successful=0;
        for (int roll=0; roll<100; roll++) if (SkillEvents.dodgeRoll(400,400,roll)) successful++;
        check(successful==15, "dodge exactly 15 percent");
        check(!SkillEvents.dodgeRoll(399,400,0) && SkillEvents.dodgeRoll(400,400,0), "dodge cooldown boundary");
        check(!SkillEvents.dodgeRoll(-1,0,0) && !SkillEvents.dodgeRoll(1,-1,0)
            && !SkillEvents.dodgeRoll(Long.MAX_VALUE-399,0,0), "dodge invalid or overflowing ticks");
        check(!SkillEvents.dodgeRoll(0,0,-1) && !SkillEvents.dodgeRoll(0,0,15), "dodge roll bounds");
        check(SkillEvents.skillDamageSource(null,true).isFireDamage(), "mage fire source is elemental");
        check(!SkillEvents.skillDamageSource(null,false).isFireDamage(), "ordinary source unchanged");
        net.minecraft.util.DamageSource ordinary = new net.minecraft.util.DamageSource("test");
        check(SkillEvents.dodgeSourceAllowed(ordinary, 1), "ordinary source eligible for further attacker checks");
        check(!SkillEvents.dodgeSourceAllowed(null, 1) && !SkillEvents.dodgeSourceAllowed(ordinary, 0)
            && !SkillEvents.dodgeSourceAllowed(ordinary, Float.NaN) && !SkillEvents.dodgeSourceAllowed(ordinary, Float.POSITIVE_INFINITY), "invalid amounts rejected");
        check(!SkillEvents.dodgeSourceAllowed(new net.minecraft.util.DamageSource("test").setFireDamage(), 1)
            && !SkillEvents.dodgeSourceAllowed(new net.minecraft.util.DamageSource("test").setMagicDamage(), 1)
            && !SkillEvents.dodgeSourceAllowed(new net.minecraft.util.DamageSource("test").setExplosion(), 1)
            && !SkillEvents.dodgeSourceAllowed(new net.minecraft.util.DamageSource("test").setDamageBypassesArmor(), 1)
            && !SkillEvents.dodgeSourceAllowed(new net.minecraft.util.DamageSource("test").setDamageIsAbsolute(), 1), "special damage excluded");
        NBTTagCompound cooldown=new NBTTagCompound(); cooldown.setLong("assassin_dodge_ready_v1",400);
        check(cloneSkills(cooldown,null,true).getLong("assassin_dodge_ready_v1")==400,
            "death preserves dodge cooldown");
        System.out.println("Skill rule checks passed");
    }
    private static NBTTagCompound cloneSkills(NBTTagCompound original, NBTTagCompound destination, boolean death) {
        String persisted=net.minecraft.entity.player.EntityPlayer.PERSISTED_NBT_TAG, key="NexusRpgSkills";
        NBTTagCompound sourceRoot=new NBTTagCompound(), sourcePersisted=new NBTTagCompound(), targetRoot=new NBTTagCompound();
        sourcePersisted.setTag(key,original.copy()); sourceRoot.setTag(persisted,sourcePersisted);
        if (destination!=null) { NBTTagCompound dp=new NBTTagCompound(); dp.setTag(key,destination.copy()); targetRoot.setTag(persisted,dp); }
        SkillEvents.cloneSkillData(sourceRoot,targetRoot,death);
        return targetRoot.getCompoundTag(persisted).getCompoundTag(key);
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
