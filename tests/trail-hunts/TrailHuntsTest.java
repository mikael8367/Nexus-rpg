package com.nexusrpg.quests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public final class TrailHuntsTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static NBTTagCompound section(NBTTagCompound root, TrailData.Trail t) {
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(t.key);
    }
    public static void main(String[] args) throws Exception {
        TrailData.Trail a = TrailData.Trail.SKELETON, b = TrailData.Trail.SPIDER;
        NBTTagCompound root = new NBTTagCompound(); root.setString("unrelated", "preserve");
        check(!TrailData.record(root,a,UUID.randomUUID()),"cannot record before start");
        check(TrailData.start(root,a) && !TrailData.start(root,a),"start once");
        check(TrailData.start(root,b),"second independent quest");
        UUID id=UUID.randomUUID();
        check(TrailData.record(root,a,id) && !TrailData.record(root,a,id),"duplicate event");
        check(TrailData.count(root,b)==0 && TrailData.record(root,b,id),"independent progress");
        check(!TrailData.record(root,a,null),"null rejected");
        NBTTagCompound clone = new NBTTagCompound();
        clone.setString("destinationOther", "keep");
        for (TrailData.Trail t : TrailData.Trail.values()) check(TrailData.cloneProgress(root,clone,t),"clone");
        for (int i=1; i<TrailData.GOAL; i++) check(TrailData.record(clone,a,UUID.randomUUID()),"record distinct");
        check(TrailData.status(clone,a)==2 && TrailData.count(clone,b)==1,"complete independent");
        check(!TrailData.record(clone,a,UUID.randomUUID()) && !TrailData.start(clone,a),"no farming");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(clone,bytes);
        NBTTagCompound loaded = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
        check(TrailData.status(loaded,a)==2 && TrailData.count(loaded,b)==1 && loaded.getString("destinationOther").equals("keep"),"disk round trip");
        NBTTagCompound respawn=new NBTTagCompound();
        for (TrailData.Trail t : TrailData.Trail.values()) TrailData.cloneProgress(loaded,respawn,t);
        check(TrailData.status(respawn,a)==2 && TrailData.count(respawn,b)==1,"respawn progress");
        NBTTagCompound bad = new NBTTagCompound(); bad.setString(EntityPlayer.PERSISTED_NBT_TAG,"unknown");
        NBTTagCompound before=bad.copy();
        check(TrailData.status(bad,a)==-1 && !TrailData.start(bad,a) && !TrailData.record(bad,a,id) && bad.equals(before),"bad persisted unchanged");
        bad=root.copy(); section(bad,a).setInteger("version",99); before=bad.copy();
        check(TrailData.status(bad,a)==-1 && !TrailData.start(bad,a) && !TrailData.record(bad,a,id) && bad.equals(before),"future version unchanged");
        bad=root.copy(); section(bad,a).setString("future","preserve"); before=bad.copy();
        check(TrailData.status(bad,a)==-1 && !TrailData.record(bad,a,id) && bad.equals(before),"unknown field unchanged");
        NBTTagCompound target=bad.copy(); NBTTagCompound originalTarget=section(target,a).copy();
        check(!TrailData.cloneProgress(root,target,a) && section(target,a).equals(originalTarget),"unknown destination not overwritten");
        boolean backup=false; for (String key : target.getKeySet()) if (key.startsWith("NexusRpgCloneBackup_v1_")) backup=true;
        check(backup,"source backed up on collision");
        NBTTagCompound once=target.copy(); TrailData.cloneProgress(root,target,a); check(target.equals(once),"clone replay does not multiply backup");
        bad=root.copy(); NBTTagList ints=new NBTTagList(); ints.appendTag(new NBTTagInt(1)); section(bad,a).setTag("kills",ints); before=bad.copy();
        check(TrailData.status(bad,a)==-1 && !TrailData.record(bad,a,id) && bad.equals(before),"wrong list type unchanged");
        bad=root.copy(); NBTTagList duplicates=new NBTTagList(); duplicates.appendTag(new NBTTagString(id.toString())); duplicates.appendTag(new NBTTagString(id.toString())); section(bad,a).setTag("kills",duplicates); before=bad.copy();
        check(TrailData.status(bad,a)==-1 && !TrailData.record(bad,a,UUID.randomUUID()) && bad.equals(before),"duplicates rejected");
        bad=root.copy(); NBTTagList malformed=new NBTTagList(); malformed.appendTag(new NBTTagString("not-uuid")); section(bad,a).setTag("kills",malformed);
        check(TrailData.status(bad,a)==-1,"bad uuid rejected");
        bad=root.copy(); NBTTagList overlong=new NBTTagList();
        for(int i=0;i<TrailData.GOAL+1;i++) overlong.appendTag(new NBTTagString(UUID.randomUUID().toString()));
        section(bad,a).setTag("kills",overlong); before=bad.copy();
        check(TrailData.status(bad,a)==-1 && !TrailData.record(bad,a,UUID.randomUUID()) && bad.equals(before),"overlong data untouched");
        bad=root.copy(); section(bad,a).setString("extension","unknown");
        NBTTagCompound validTarget=root.copy(), oldSection=section(validTarget,a).copy();
        check(!TrailData.cloneProgress(bad,validTarget,a) && section(validTarget,a).equals(oldSection),"unknown source cannot replace valid destination");
        for(int i=1;i<TrailData.GOAL;i++) check(TrailData.record(respawn,b,UUID.randomUUID()),"spider progress");
        check(TrailData.status(respawn,b)==2 && TrailData.status(respawn,a)==2,"both rewards completed independently");
        AttributeMap attributes=new AttributeMap();
        IAttributeInstance speed=attributes.registerAttribute(new RangedAttribute(null,"test.speed",1,0,10));
        IAttributeInstance armor=attributes.registerAttribute(new RangedAttribute(null,"test.armor",0,0,30));
        UUID speedId=UUID.randomUUID(), armorId=UUID.randomUUID();
        for (int i=0;i<100;i++) {
            TrailHunts.updateBonus(speed,speedId,"speed",0.02D,2,true);
            TrailHunts.updateBonus(armor,armorId,"armor",1D,0,true);
        }
        check(Math.abs(speed.getAttributeValue()-1.02)<1e-9 && speed.getModifiers().size()==1 && armor.getAttributeValue()==1 && armor.getModifiers().size()==1,"rewards do not stack");
        TrailHunts.updateBonus(speed,speedId,"speed",0.02D,2,false);
        check(speed.getAttributeValue()==1,"unearned bonus removed");
        check(root.getString("unrelated").equals("preserve"),"other tags survive");
        System.out.println("TrailHuntsTest passed");
    }
}
