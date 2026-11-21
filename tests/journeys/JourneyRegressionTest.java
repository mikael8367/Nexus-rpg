package com.nexusrpg.quests;

import java.io.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public final class JourneyRegressionTest {
    static void check(boolean x, String message) { if (!x) throw new AssertionError(message); }
    static NBTTagCompound section(NBTTagCompound root, JourneyData.Journey j) {
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getCompoundTag(j.key);
    }
    static void unchanged(NBTTagCompound root, JourneyData.Journey j, String name) {
        NBTTagCompound before = root.copy();
        check(JourneyData.mask(root,j)==-1 && !JourneyData.record(root,j,1) && root.equals(before), name);
    }
    public static void main(String[] args) throws Exception {
        JourneyData.Journey e=JourneyData.Journey.EXPLORER, c=JourneyData.Journey.CRAFTER;
        NBTTagCompound root=new NBTTagCompound(); root.setString("other", "keep");
        check(JourneyData.mask(root,e)==0 && JourneyData.mask(root,c)==0,"fresh");
        check(!JourneyData.record(root,e,0) && !JourneyData.record(root,e,3) && !JourneyData.record(root,e,8) && root.getKeySet().size()==1,"invalid bits");
        check(JourneyData.record(root,e,1) && !JourneyData.record(root,e,1),"duplicate");
        check(JourneyData.record(root,e,4) && JourneyData.record(root,c,1),"independent");
        check(JourneyData.mask(root,e)==5 && JourneyData.mask(root,c)==1,"counts");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); CompressedStreamTools.writeCompressed(root,bytes);
        NBTTagCompound loaded=CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray()));
        check(JourneyData.mask(loaded,e)==5 && JourneyData.mask(loaded,c)==1 && loaded.getString("other").equals("keep"),"NBT round trip");
        NBTTagCompound spawn=new NBTTagCompound(); spawn.setString("destination","survives");
        for (JourneyData.Journey j : JourneyData.Journey.values()) JourneyData.cloneProgress(loaded,spawn,j);
        check(JourneyData.mask(spawn,e)==5 && JourneyData.mask(spawn,c)==1 && spawn.getString("destination").equals("survives"),"respawn");
        check(JourneyData.record(spawn,e,2) && JourneyData.record(spawn,c,2) && JourneyData.record(spawn,c,4),"completion");
        check(JourneyData.mask(spawn,e)==7 && JourneyData.mask(spawn,c)==7 && !JourneyData.record(spawn,c,4),"only once");
        NBTTagCompound bad=new NBTTagCompound(); bad.setString(EntityPlayer.PERSISTED_NBT_TAG,"unknown"); unchanged(bad,e,"wrong root");
        bad=root.copy(); section(bad,e).setInteger("version",99); unchanged(bad,e,"future version");
        bad=root.copy(); section(bad,e).setString("future","keep"); unchanged(bad,e,"unknown field");
        bad=root.copy(); section(bad,e).setString("mask","1"); unchanged(bad,e,"wrong type");
        bad=root.copy(); section(bad,e).setInteger("mask",8); unchanged(bad,e,"bits out of bounds");
        bad=root.copy(); section(bad,e).setInteger("mask",-1); unchanged(bad,e,"negative");
        bad=root.copy(); section(bad,e).setString("future","keep");
        NBTTagCompound valid=root.copy(), before=section(valid,e).copy();
        check(!JourneyData.cloneProgress(bad,valid,e) && before.equals(section(valid,e)),"unknown source preserved as backup");
        boolean backup=false; for (String key : valid.getKeySet()) if (key.startsWith("NexusRpgCloneBackup_v1_")) backup=true;
        check(backup,"backup exists"); NBTTagCompound replay=valid.copy(); JourneyData.cloneProgress(bad,valid,e); check(valid.equals(replay),"replay");
        NBTTagCompound zero=root.copy(); section(zero,e).setInteger("mask",0); before=section(zero,e).copy();
        JourneyData.cloneProgress(bad,zero,e); check(before.equals(section(zero,e)),"zero-progress destination protected");
        NBTTagCompound brokenDest=bad.copy(); before=section(brokenDest,e).copy(); JourneyData.cloneProgress(root,brokenDest,e); check(before.equals(section(brokenDest,e)),"unknown destination protected");
        check(JourneyEvents.locationBits(0,24,false,false)==1 && JourneyEvents.locationBits(0,25,false,false)==0,"depth limit");
        check(JourneyEvents.locationBits(0,110,false,false)==2 && JourneyEvents.locationBits(0,109,false,false)==0,"height limit");
        check(JourneyEvents.locationBits(0,64,true,true)==4 && JourneyEvents.locationBits(0,64,false,true)==0 && JourneyEvents.locationBits(-1,20,true,true)==0,"ocean and dimension");
        AttributeMap map=new AttributeMap(); IAttributeInstance speed=map.registerAttribute(new RangedAttribute(null,"test.speed",1,0,10));
        for(int i=0;i<100;i++) JourneyEvents.bonus(speed,JourneyEvents.EXPLORER_SPEED,"test",0.02D,2,true);
        check(speed.getModifiers().size()==1 && Math.abs(speed.getAttributeValue()-1.02)<1e-8,"idempotent bonus");
        JourneyEvents.bonus(speed,JourneyEvents.EXPLORER_SPEED,"test",0.02D,2,false); check(speed.getModifiers().isEmpty(),"unearned removed");
        PlayerEvent.ItemCraftedEvent crafted=new PlayerEvent.ItemCraftedEvent(null,null,null);
        check(!crafted.isCancelable(),"craft callback is not cancelable; no cancelled pre-event counted");
        System.out.println("JourneyRegressionTest passed");
    }
}
