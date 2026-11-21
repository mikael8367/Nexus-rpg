package com.nexusrpg.character;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class CharacterNetwork {
    private static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("nexus_character");
    private static boolean initialized;
    private CharacterNetwork() {}
    public static void init() {
        if(initialized) return; initialized=true;
        CHANNEL.registerMessage(ActionHandler.class,Action.class,0,Side.SERVER);
        CHANNEL.registerMessage(SnapshotHandler.class,Snapshot.class,1,Side.CLIENT);
        if(FMLCommonHandler.instance().getSide()==Side.CLIENT) initClient();
    }
    @SideOnly(Side.CLIENT) private static void initClient() { CharacterClient.init(); }
    static void sync(EntityPlayerMP p) { if(initialized && p.connection!=null) CHANNEL.sendTo(new Snapshot(CharacterProfile.root(p).copy()),p); }
    @SideOnly(Side.CLIENT) static void send(int type,int value) { CHANNEL.sendToServer(new Action(type,value)); }
    public static final class Action implements IMessage {
        int type,value; public Action() {} Action(int type,int value) {this.type=type;this.value=value;}
        public void fromBytes(ByteBuf b) {type=b.readInt();value=b.readInt();}
        public void toBytes(ByteBuf b) {b.writeInt(type);b.writeInt(value);}
    }
    public static final class Snapshot implements IMessage {
        NBTTagCompound data; public Snapshot() {} Snapshot(NBTTagCompound data){this.data=data;}
        public void fromBytes(ByteBuf b) {data=ByteBufUtils.readTag(b);}
        public void toBytes(ByteBuf b) {ByteBufUtils.writeTag(b,data);}
    }
    public static final class ActionHandler implements IMessageHandler<Action,IMessage> {
        public IMessage onMessage(Action msg,MessageContext context) {
            EntityPlayerMP p=context.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> {
                if(p.hasDisconnected()||p.isDead) return;
                if(msg.type==0) CharacterProfile.select(p,msg.value);
                else if(msg.type==1) CharacterProfile.allocate(p,msg.value);
                else if(msg.type==2) CharacterProfile.sync(p);
            }); return null;
        }
    }
    public static final class SnapshotHandler implements IMessageHandler<Snapshot,IMessage> {
        public IMessage onMessage(Snapshot msg,MessageContext context) { receive(msg); return null; }
        @SideOnly(Side.CLIENT) private static void receive(Snapshot msg) { CharacterClient.receive(msg.data); }
    }
}
