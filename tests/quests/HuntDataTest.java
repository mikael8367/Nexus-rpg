package com.nexusrpg.quests;

import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagInt;
import com.nexusrpg.character.CharacterEvents;

public final class HuntDataTest {
    private static void check(boolean b,String m) { if(!b) throw new AssertionError(m); }
    public static void main(String[] a) throws Exception {
        NBTTagCompound root=new NBTTagCompound(); root.setString("other", "keep");
        check(HuntData.status(root)==0,"initial");
        check(!HuntData.record(root,UUID.randomUUID()),"no kills before start");
        check(HuntData.start(root) && !HuntData.start(root),"start once");
        UUID first=UUID.randomUUID();
        check(HuntData.record(root,first),"first kill");
        check(!HuntData.record(root,first) && HuntData.count(root)==1,"duplicate kill");
        NBTTagCompound restored=root.copy();
        check(HuntData.count(restored)==1,"NBT copy persistence");
        NBTTagCompound cloned=new NBTTagCompound();
        check(CharacterEvents.cloneSection(restored,cloned,HuntData.KEY),"death clone");
        check(HuntData.count(cloned)==1,"clone preserves progress");
        for(int i=1;i<5;i++) check(HuntData.record(cloned,UUID.randomUUID()),"new kill "+i);
        check(HuntData.status(cloned)==2 && HuntData.count(cloned)==5,"complete");
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        net.minecraft.nbt.CompressedStreamTools.writeCompressed(cloned,bytes);
        NBTTagCompound done=net.minecraft.nbt.CompressedStreamTools.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        check(HuntData.status(done)==2,"serialized completion persists");
        net.minecraft.entity.ai.attributes.AttributeMap map=new net.minecraft.entity.ai.attributes.AttributeMap();
        net.minecraft.entity.ai.attributes.IAttributeInstance health=map.registerAttribute(new net.minecraft.entity.ai.attributes.RangedAttribute(null,"test.health",20,0,100));
        ZombieHunt.updateBonus(health,true); ZombieHunt.updateBonus(health,true);
        check(health.getAttributeValue()==22 && health.getModifiers().size()==1,"one permanent bonus, idempotent");
        ZombieHunt.updateBonus(health,false); check(health.getAttributeValue()==20,"bonus removed when not earned");
        ZombieHunt.updateBonus(health,HuntData.status(done)==2); check(health.getAttributeValue()==22,"restored reward");
        check(!HuntData.start(done) && !HuntData.record(done,UUID.randomUUID()),"completed cannot repeat");
        NBTTagCompound cloneDone=new NBTTagCompound(); CharacterEvents.cloneSection(done,cloneDone,HuntData.KEY);
        check(HuntData.status(cloneDone)==2,"completion survives clone");
        NBTTagCompound invalid=new NBTTagCompound(); invalid.setString(EntityPlayer.PERSISTED_NBT_TAG,"bad");
        NBTTagCompound before=invalid.copy();
        check(HuntData.status(invalid)==-1 && !HuntData.start(invalid) && !HuntData.record(invalid,first) && invalid.equals(before),"malformed persisted untouched");
        NBTTagCompound bad=root.copy();
        NBTTagCompound section=bad.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(HuntData.KEY);
        NBTTagList ints=new NBTTagList(); ints.appendTag(new NBTTagInt(1)); section.setTag("kills",ints);
        before=bad.copy(); check(HuntData.status(bad)==-1 && !HuntData.record(bad,first) && bad.equals(before),"wrong list untouched");
        section.setInteger("version",99); before=bad.copy();
        check(HuntData.status(bad)==-1 && !HuntData.start(bad) && bad.equals(before),"future version untouched");
        check("keep".equals(root.getString("other")),"other data preserved");
        System.out.println("HuntDataTest passed");
    }
}
