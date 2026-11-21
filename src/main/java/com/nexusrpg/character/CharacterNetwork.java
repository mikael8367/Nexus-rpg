package com.nexusrpg.character;

import com.nexusrpg.skills.SkillTree;
import com.nexusrpg.skills.SkillTreeService;
import java.util.Map;
import java.util.WeakHashMap;
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
    // Bound the amount of work one connection can queue on the server thread.
    private static final Map<EntityPlayerMP,Integer> QUEUED = new WeakHashMap<>();
    private static synchronized boolean reserve(EntityPlayerMP p) {
        int count=QUEUED.containsKey(p) ? QUEUED.get(p) : 0;
        if(count>=16) return false;
        QUEUED.put(p,count+1); return true;
    }
    private static synchronized void release(EntityPlayerMP p) {
        Integer count=QUEUED.get(p);
        if(count==null || count<=1) QUEUED.remove(p); else QUEUED.put(p,count-1);
    }
    private CharacterNetwork() {}
    public static void init() {
        if(initialized) return; initialized=true;
        CHANNEL.registerMessage(ActionHandler.class,Action.class,0,Side.SERVER);
        CHANNEL.registerMessage(SnapshotHandler.class,Snapshot.class,1,Side.CLIENT);
        CHANNEL.registerMessage(SkillRequestHandler.class,SkillRequest.class,2,Side.SERVER);
        CHANNEL.registerMessage(SkillSnapshotHandler.class,SkillSnapshot.class,3,Side.CLIENT);
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
            if(p==null || msg.type<0 || msg.type>2 || !reserve(p)) return null;
            try {
                p.getServerWorld().addScheduledTask(() -> {
                    try {
                        if(p.hasDisconnected()||p.isDead) return;
                        if(msg.type==0) CharacterProfile.select(p,msg.value);
                        else if(msg.type==1) CharacterProfile.allocate(p,msg.value);
                        else CharacterProfile.sync(p);
                    } finally { release(p); }
                });
            } catch (RuntimeException e) { release(p); throw e; }
            return null;
        }
    }
    @SideOnly(Side.CLIENT) static void requestSkills(int session, int sequence, int nodeIndex) {
        CHANNEL.sendToServer(new SkillRequest(session, sequence, nodeIndex));
    }
    /** nodeIndex=-1 requests a snapshot; other values address immutable shared definitions. */
    public static final class SkillRequest implements IMessage {
        int session, sequence, nodeIndex;
        public SkillRequest() {}
        SkillRequest(int session,int sequence,int nodeIndex) { this.session=session; this.sequence=sequence; this.nodeIndex=nodeIndex; }
        public void fromBytes(ByteBuf b) { session=b.readInt(); sequence=b.readInt(); nodeIndex=b.readInt(); }
        public void toBytes(ByteBuf b) { b.writeInt(session); b.writeInt(sequence); b.writeInt(nodeIndex); }
    }
    public static final class SkillSnapshot implements IMessage {
        int session, sequence; NBTTagCompound data;
        public SkillSnapshot() {}
        SkillSnapshot(int session,int sequence,NBTTagCompound data) { this.session=session; this.sequence=sequence; this.data=data; }
        public void fromBytes(ByteBuf b) { session=b.readInt(); sequence=b.readInt(); data=ByteBufUtils.readTag(b); }
        public void toBytes(ByteBuf b) { b.writeInt(session); b.writeInt(sequence); ByteBufUtils.writeTag(b,data); }
    }
    public static final class SkillRequestHandler implements IMessageHandler<SkillRequest,IMessage> {
        public IMessage onMessage(SkillRequest msg,MessageContext context) {
            EntityPlayerMP p=context.getServerHandler().player;
            if(p==null || !reserve(p)) return null;
            try {
                p.getServerWorld().addScheduledTask(() -> {
                    try {
                        if(p.hasDisconnected() || p.connection==null) return;
                        String result="";
                        if(msg.nodeIndex!=-1) {
                            if(msg.nodeIndex<0 || msg.nodeIndex>=SkillTree.allNodes().size()) result="Habilidade desconhecida.";
                            else result=SkillTreeService.allocate(p,SkillTree.allNodes().get(msg.nodeIndex).id);
                        }
                        NBTTagCompound data=SkillTreeService.snapshot(p);
                        data.setString("result",result);
                        CHANNEL.sendTo(new SkillSnapshot(msg.session,msg.sequence,data),p);
                    } finally { release(p); }
                });
            } catch (RuntimeException e) { release(p); throw e; }
            return null;
        }
    }
    public static final class SkillSnapshotHandler implements IMessageHandler<SkillSnapshot,IMessage> {
        public IMessage onMessage(SkillSnapshot msg,MessageContext context) { receiveSkills(msg,context); return null; }
        @SideOnly(Side.CLIENT) private static void receiveSkills(SkillSnapshot msg,MessageContext context) {
            net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
            net.minecraft.client.network.NetHandlerPlayClient connection=context.getClientHandler();
            mc.addScheduledTask(() -> {
                if(connection!=null && connection==mc.getConnection() && mc.player!=null && mc.currentScreen instanceof CharacterRpgScreen)
                    ((CharacterRpgScreen)mc.currentScreen).receiveSkills(msg.session,msg.sequence,msg.data);
            });
        }
    }
    public static final class SnapshotHandler implements IMessageHandler<Snapshot,IMessage> {
        public IMessage onMessage(Snapshot msg,MessageContext context) { receive(msg,context); return null; }
        @SideOnly(Side.CLIENT) private static void receive(Snapshot msg,MessageContext context) {
            net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getMinecraft();
            net.minecraft.client.network.NetHandlerPlayClient connection=context.getClientHandler();
            net.minecraft.entity.player.EntityPlayer player=mc.player;
            mc.addScheduledTask(() -> {
                if(connection!=null && connection==mc.getConnection() && player!=null && player==mc.player)
                    CharacterClient.receive(msg.data);
            });
        }
    }
}
