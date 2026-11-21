package com.nexusrpg.skills;

import com.nexusrpg.character.CharacterEvents;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** Real Minecraft NBT and clone helper regression; not a live server test. */
public final class CloneNbtRegressionTest {
    private static final String P=EntityPlayer.PERSISTED_NBT_TAG, C="NexusRpgCharacter", S="NexusRpgSkills", B="NexusRpgCloneBackup_v1_";
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static NBTTagCompound root(String key, NBTBase value) {
        NBTTagCompound root=new NBTTagCompound(), p=new NBTTagCompound(); p.setTag(key,value); root.setTag(P,p); return root;
    }
    private static NBTTagCompound backup(NBTTagCompound root, String reason) {
        for (String key:root.getKeySet()) if(key.startsWith(B) && root.hasKey(key,10)) {
            NBTTagCompound record=root.getCompoundTag(key); if(reason.equals(record.getString("reason"))) return record;
        }
        throw new AssertionError("missing backup "+reason);
    }
    private static NBTTagCompound profile(String clazz,int level,int points) {
        NBTTagCompound p=new NBTTagCompound(); p.setString("class",clazz); p.setInteger("level",level); p.setInteger("points",points); return p;
    }
    public static void main(String[] args) throws Exception {
        NBTTagCompound source=profile("mage",12,4), nested=new NBTTagCompound(); nested.setString("unknown","source"); source.setTag("future",nested);
        NBTTagCompound dest=profile("warrior",3,1), dn=new NBTTagCompound(); dn.setString("unknown","destination"); dest.setTag("future",dn);
        NBTTagCompound old=root(C,source), target=root(C,dest); target.getCompoundTag(P).setString("other_mod","keep");
        target.setString(B+"0","occupied");
        NBTTagCompound oldBefore=old.copy(), targetBefore=target.copy();
        check(CharacterEvents.cloneSection(old,target,C),"profile clone active");
        check(target.getCompoundTag(P).getCompoundTag(C).equals(source),"source profile is active as a whole");
        NBTTagCompound b=backup(target,"conflicting_snapshots");
        check(b.getInteger("version")==1 && b.getString("section").equals(C) && b.getTag("original").equals(source) && b.getTag("destination").equals(dest),"both exact profile snapshots retained");
        check(target.getString(B+"0").equals("occupied") && target.getCompoundTag(P).getString("other_mod").equals("keep"),"occupied backup and other mod unchanged");
        check(old.equals(oldBefore) && targetBefore.getCompoundTag(P).getCompoundTag(C).equals(dest),"input snapshots untouched");
        ByteArrayOutputStream out=new ByteArrayOutputStream(); CompressedStreamTools.writeCompressed(target,out);
        NBTTagCompound loaded=CompressedStreamTools.readCompressed(new ByteArrayInputStream(out.toByteArray()));
        check(loaded.equals(target),"serialized backups survive NBT roundtrip");
        NBTTagCompound next=new NBTTagCompound(); next.setString(B+"1","already here");
        CharacterEvents.cloneSection(loaded,next,C); CharacterEvents.cloneSection(loaded,next,S);
        check(next.getString(B+"1").equals("already here") && backup(next,"conflicting_snapshots").getTag("destination").equals(dest),"backup carried across clones without collision");
        int n=0; for(String k:next.getKeySet()) if(k.startsWith(B) && next.hasKey(k,10) && next.getCompoundTag(k).getString("reason").equals("conflicting_snapshots")) n++;
        check(n==1,"two handlers do not duplicate carried backup");
        NBTTagCompound empty=new NBTTagCompound(), partial=profile("mage",2,0), noDest=root(C,new NBTTagCompound());
        CharacterEvents.cloneSection(root(C,partial),noDest,C); check(noDest.getCompoundTag(P).getCompoundTag(C).equals(partial),"normal empty target clone");
        NBTTagCompound targetOnly=root(C,partial); CharacterEvents.cloneSection(root(C,empty),targetOnly,C); check(targetOnly.getCompoundTag(P).getCompoundTag(C).equals(partial),"empty source does not erase target");
        NBTTagCompound malformedSource=root(C,new net.minecraft.nbt.NBTTagString("broken")), validTarget=root(C,partial);
        CharacterEvents.cloneSection(malformedSource,validTarget,C);
        check(validTarget.getCompoundTag(P).getCompoundTag(C).equals(partial) && backup(validTarget,"malformed_section").getTag("original").equals(malformedSource.getCompoundTag(P).getTag(C)),"malformed source kept as backup");
        NBTTagCompound malformedTarget=root(C,new net.minecraft.nbt.NBTTagInt(123));
        CharacterEvents.cloneSection(root(C,partial),malformedTarget,C);
        check(malformedTarget.getCompoundTag(P).getInteger(C)==123 && backup(malformedTarget,"malformed_section").getTag("original").equals(partial),"malformed target not overwritten");
        NBTTagCompound badRoot=new NBTTagCompound(); badRoot.setString(P,"bad persisted");
        CharacterEvents.cloneSection(root(C,partial),badRoot,C);
        check(badRoot.getString(P).equals("bad persisted") && backup(badRoot,"malformed_destination_persisted").getTag("original").equals(partial),"malformed persisted target not overwritten");
        NBTTagCompound badOld=new NBTTagCompound(); badOld.setString(P,"bad source persisted"); NBTTagCompound goodRoot=root(C,partial);
        CharacterEvents.cloneSection(badOld,goodRoot,C);
        check(goodRoot.getCompoundTag(P).getCompoundTag(C).equals(partial) && backup(goodRoot,"malformed_persisted").getString("original").equals("bad source persisted"),"malformed persisted source backed up");
        NBTTagCompound srcSkills=new NBTTagCompound(), dstSkills=new NBTTagCompound(), st=new NBTTagCompound(), dt=new NBTTagCompound();
        srcSkills.setInteger("rank_warrior",2); srcSkills.setLong("combo_start_warrior",10); srcSkills.setLong("combo_until_warrior",130); srcSkills.setString("combo_target_warrior","enemy");
        st.setInteger("warrior.active",1); st.setTag("nested",nested.copy()); srcSkills.setTag("tree_v1",st);
        dstSkills.setInteger("rank_warrior",4); dt.setInteger("mage.active",2); dt.setTag("nested",dn.copy()); dstSkills.setTag("tree_v1",dt); dstSkills.setString("future","keep only in backup");
        NBTTagCompound sr=root(S,srcSkills), dr=root(S,dstSkills); SkillEvents.cloneSkillData(sr,dr,true);
        NBTTagCompound active=dr.getCompoundTag(P).getCompoundTag(S);
        check(active.getInteger("rank_warrior")==2 && !active.getCompoundTag("tree_v1").hasKey("mage.active") && !active.hasKey("combo_until_warrior"),"no double allocation; death resets combo");
        check(backup(dr,"conflicting_snapshots").getTag("original").equals(srcSkills) && backup(dr,"conflicting_snapshots").getTag("destination").equals(dstSkills),"complete nested conflict preserved");
        NBTTagCompound firstResult=dr.copy();
        for (int i=0;i<20;i++) SkillEvents.cloneSkillData(sr,dr,true);
        check(dr.equals(firstResult),"duplicate death event leaves allocation and backups unchanged");
        // Legacy recovery records and unrelated NBT survive many ordinary deaths.
        NBTTagCompound legacy=new NBTTagCompound(); legacy.setString("reason","death_combo_reset"); legacy.setString("legacy_unknown","preserve");
        NBTTagCompound player=new NBTTagCompound(); player.setTag(B+"77",legacy.copy());
        player.setString("foreign_root","keep");
        for (int i=0;i<300;i++) {
            NBTTagCompound live=srcSkills.copy(); live.setLong("combo_start_warrior",i); live.setLong("combo_until_warrior",i+120);
            player.setTag(P,root(S,live).getTag(P).copy());
            NBTTagCompound respawn=new NBTTagCompound(); respawn.setString("foreign_root","keep");
            SkillEvents.cloneSkillData(player,respawn,true);
            SkillEvents.cloneSkillData(player,respawn,true);
            check(respawn.getTag(B+"77").equals(legacy),"legacy recovery survives death "+i);
            int count=0; for (String k:respawn.getKeySet()) if(k.startsWith(B)) count++;
            check(count==1,"ordinary deaths do not create unbounded snapshots "+i);
            check(respawn.getCompoundTag(P).getCompoundTag(S).getCompoundTag("tree_v1").equals(st),"tree preserved on death "+i);
            player=respawn;
        }
        NBTTagCompound repeatedBadTarget=root(C,new net.minecraft.nbt.NBTTagInt(123));
        CharacterEvents.cloneSection(root(C,partial),repeatedBadTarget,C);
        NBTTagCompound once=repeatedBadTarget.copy();
        for(int i=0;i<20;i++) CharacterEvents.cloneSection(root(C,partial),repeatedBadTarget,C);
        check(repeatedBadTarget.equals(once),"duplicate malformed conflict does not grow backups");
        NBTTagCompound dimensional=new NBTTagCompound(); SkillEvents.cloneSkillData(sr,dimensional,false);
        check(dimensional.getCompoundTag(P).getCompoundTag(S).equals(srcSkills),"dimension clone keeps combo");
        NBTTagCompound corrupt=srcSkills.copy(); corrupt.setString("combo_until_warrior","broken"); NBTTagCompound corruptDest=new NBTTagCompound();
        SkillEvents.cloneSkillData(root(S,corrupt),corruptDest,true);
        check(corruptDest.getCompoundTag(P).getCompoundTag(S).equals(corrupt),"malformed runtime skill preserved on death");
        System.out.println("CloneNbtRegressionTest: passed");
    }
}
