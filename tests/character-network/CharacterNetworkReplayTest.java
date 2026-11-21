package com.nexusrpg.character;

/** Executa a mesma máquina de replay usada pelos handlers, sem simular Forge. */
public final class CharacterNetworkReplayTest {
    private static void check(boolean ok,String why) { if(!ok) throw new AssertionError(why); }
    public static void main(String[] args) {
        io.netty.buffer.ByteBuf wire=io.netty.buffer.Unpooled.buffer();
        CharacterNetwork.SkillRequest sentSkill=new CharacterNetwork.SkillRequest(42,17,3);
        sentSkill.toBytes(wire);
        CharacterNetwork.SkillRequest readSkill=new CharacterNetwork.SkillRequest(); readSkill.fromBytes(wire);
        check(readSkill.session==42 && readSkill.sequence==17 && readSkill.nodeIndex==3, "skill wire roundtrip");
        wire.clear();
        CharacterNetwork.AttributeRequest sentStat=new CharacterNetwork.AttributeRequest(2147483648L,6);
        sentStat.toBytes(wire);
        CharacterNetwork.AttributeRequest readStat=new CharacterNetwork.AttributeRequest(); readStat.fromBytes(wire);
        check(readStat.sequence==2147483648L && readStat.stat==6, "stat wire roundtrip");
        wire.clear();
        CharacterNetwork.Action action=new CharacterNetwork.Action(2,123);
        action.toBytes(wire);
        check(wire.readableBytes()==8, "action wire size");
        CharacterNetwork.Action readAction=new CharacterNetwork.Action(); readAction.fromBytes(wire);
        check(readAction.type==2 && readAction.value==123, "action wire roundtrip");
        net.minecraft.nbt.NBTTagCompound data=new net.minecraft.nbt.NBTTagCompound();
        data.setString("class","mage"); data.setInteger("points",42);
        wire.clear();
        new CharacterNetwork.Snapshot(data).toBytes(wire);
        CharacterNetwork.Snapshot snapshot=new CharacterNetwork.Snapshot(); snapshot.fromBytes(wire);
        check(snapshot.data!=null && snapshot.data.getString("class").equals("mage") && snapshot.data.getInteger("points")==42, "profile snapshot wire roundtrip");
        wire.clear();
        new CharacterNetwork.SkillSnapshot(42,17,data).toBytes(wire);
        CharacterNetwork.SkillSnapshot skills=new CharacterNetwork.SkillSnapshot(); skills.fromBytes(wire);
        check(skills.session==42 && skills.sequence==17 && skills.data.getInteger("points")==42, "skill snapshot wire roundtrip");
        wire.clear();
        new CharacterNetwork.AttributeSnapshot(2147483648L,true,data).toBytes(wire);
        CharacterNetwork.AttributeSnapshot stat=new CharacterNetwork.AttributeSnapshot(); stat.fromBytes(wire);
        check(stat.sequence==2147483648L && stat.accepted && stat.data.getString("class").equals("mage"), "attribute snapshot wire roundtrip");
        wire.clear();
        new CharacterNetwork.AttributeSnapshot(Long.MAX_VALUE,false,data).toBytes(wire);
        stat=new CharacterNetwork.AttributeSnapshot(); stat.fromBytes(wire);
        check(stat.sequence==Long.MAX_VALUE && !stat.accepted, "attribute snapshot negative acknowledgement");
        wire.release();
        CharacterNetwork.ReplayState c=new CharacterNetwork.ReplayState();
        check(c.skill(0,1,0)==-1 && c.skill(1,0,0)==-1, "invalid skill tokens");
        check(c.skill(1,1,-1)==1, "initial refresh");
        check(c.skill(1,2,4)==1, "first buy"); c.result="ok";
        check(c.skill(1,2,4)==0 && c.result.equals("ok"), "duplicate cached");
        check(c.skill(1,2,5)==-1, "same token altered payload");
        check(c.skill(1,2,4)==0 && c.result.equals("ok"), "altered payload cannot replace cached result");
        check(c.skill(1,4,-1)==1, "barrier may skip sequence");
        check(c.skill(1,3,4)==-1 && c.skill(1,2,4)==-1, "delayed and old duplicate");
        check(c.skill(2,1,-1)==1 && c.skill(1,99,4)==-1, "new screen supersedes old");
        check(c.skill(2,2,4)==1, "normal second screen");
        check(c.attribute(0,0)==-1 && c.attribute(-1,0)==-1, "invalid stat tokens");
        check(c.attribute(1,0)==1, "stat purchase"); c.attributeResult=true;
        check(c.attribute(1,0)==0 && c.attributeResult, "stat retry cached");
        check(c.attribute(1,1)==-1, "stat retry altered payload");
        check(c.attribute(2,-1)==1 && c.attribute(1,0)==-1, "barrier cancels delayed stat");
        CharacterNetwork.ReplayState reconnect=new CharacterNetwork.ReplayState();
        check(reconnect.skill(1,1,4)==1 && reconnect.attribute(1,0)==1, "new connection independent");
        check(c.skill(2,2,4)==0 && c.attribute(2,-1)==0, "respawn shares connection state");
        for(int i=3;i<100003;i++) {
            check(c.skill(2,i,4)==1 && c.skill(2,i,4)==0, "many skill tokens");
            check(c.attribute(i,0)==1 && c.attribute(i,0)==0, "many stat tokens");
        }
        // No per-request collections/cache: fixed number of scalar fields per connection.
        for(java.lang.reflect.Field f:CharacterNetwork.ReplayState.class.getDeclaredFields())
            check(f.getType().isPrimitive() || f.getType()==String.class, "unbounded field: "+f);
        System.out.println("CharacterNetworkReplayTest: OK");
    }
}
