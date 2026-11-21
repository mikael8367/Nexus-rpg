package com.nexusrpg.compass;

import com.nexusrpg.NexusRpg;
import com.nexusrpg.init.ModBiomes;
import com.nexusrpg.init.ModItems;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Server-authoritative, bounded search; never requests or generates chunks. */
public final class CompassNetwork {
    public static final String[] IDS = {"enchanted_grove", "frost_crown", "sunken_empire", "witching_mire", "cinder_heart", "rosa_sanctum", "umbral_reach", "titan_canopy", "magma_wastes"};
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("nexusrpg_compass");
    private static final int RADIUS = 2048, STEP = 128, BUDGET = 12, MAX_JOBS = 4;
    private static final List<int[]> OFFSETS = offsets();
    private static final List<Job> JOBS = new ArrayList<>();
    private CompassNetwork() {}
    public static void init() {
        CHANNEL.registerMessage(RequestHandler.class, Request.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ReplyHandler.class, Reply.class, 1, Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(new CompassNetwork());
    }
    private static List<int[]> offsets() {
        List<int[]> result = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x += STEP)
            for (int z = -RADIUS; z <= RADIUS; z += STEP)
                if ((long)x*x + (long)z*z <= (long)RADIUS*RADIUS) result.add(new int[] {x,z});
        result.sort(Comparator.comparingLong(a -> (long)a[0]*a[0] + (long)a[1]*a[1]));
        return result;
    }
    public static void clearJobs() { JOBS.clear(); }
    private static Biome biome(int id) {
        switch (id) {
            case 0: return ModBiomes.ENCHANTED_GROVE; case 1: return ModBiomes.FROST_CROWN;
            case 2: return ModBiomes.SUNKEN_EMPIRE; case 3: return ModBiomes.WITCHING_MIRE;
            case 4: return ModBiomes.CINDER_HEART; case 5: return ModBiomes.ROSA_SANCTUM;
            case 6: return ModBiomes.UMBRAL_REACH; case 7: return ModBiomes.TITAN_CANOPY;
            case 8: return ModBiomes.MAGMA_WASTES;
            default: return null;
        }
    }
    private static boolean held(EntityPlayerMP p) {
        return p.getHeldItemMainhand().getItem() == ModItems.BIOME_COMPASS || p.getHeldItemOffhand().getItem() == ModItems.BIOME_COMPASS;
    }
    private static void reply(EntityPlayerMP p, int id, int seq, int status, int x, int z) {
        CHANNEL.sendTo(new Reply(id, seq, status, x, z), p);
    }
    public static final class Request implements IMessage {
        int id, seq;
        public Request() {} public Request(int id, int seq) { this.id=id; this.seq=seq; }
        public void fromBytes(ByteBuf b) { id=b.readInt(); seq=b.readInt(); }
        public void toBytes(ByteBuf b) { b.writeInt(id); b.writeInt(seq); }
    }
    public static final class Reply implements IMessage {
        int id, seq, status, x, z;
        public Reply() {} Reply(int id, int seq, int status, int x, int z) { this.id=id;this.seq=seq;this.status=status;this.x=x;this.z=z; }
        public void fromBytes(ByteBuf b) { id=b.readInt();seq=b.readInt();status=b.readInt();x=b.readInt();z=b.readInt(); }
        public void toBytes(ByteBuf b) { b.writeInt(id);b.writeInt(seq);b.writeInt(status);b.writeInt(x);b.writeInt(z); }
    }
    public static final class RequestHandler implements IMessageHandler<Request, IMessage> {
        public IMessage onMessage(Request m, MessageContext ctx) {
            EntityPlayerMP p=ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> {
                if (p.connection == null || p.hasDisconnected() || biome(m.id)==null) return;
                if (!held(p) || p.isDead) { reply(p,m.id,m.seq,6,0,0); return; }
                if (p.dimension != 0) { reply(p,m.id,m.seq,5,0,0); return; }
                if (p.getCooldownTracker().hasCooldown(ModItems.BIOME_COMPASS)) { reply(p,m.id,m.seq,3,0,0); return; }
                if (JOBS.size() >= MAX_JOBS) { reply(p,m.id,m.seq,4,0,0); return; }
                for (Job job : JOBS) if (job.player == p) { reply(p,m.id,m.seq,4,0,0); return; }
                p.getCooldownTracker().setCooldown(ModItems.BIOME_COMPASS, 100);
                JOBS.add(new Job(p,m.id,m.seq));
            });
            return null;
        }
    }
    public static final class ReplyHandler implements IMessageHandler<Reply, IMessage> {
        public IMessage onMessage(Reply m, MessageContext ctx) { receiveClient(m); return null; }
        @SideOnly(Side.CLIENT) private void receiveClient(Reply m) { CompassClient.receive(m); }
    }
    private static final class Job {
        final EntityPlayerMP player; final WorldServer world; final int id, seq, x, z; int cursor;
        Job(EntityPlayerMP p,int id,int seq) { player=p;world=p.getServerWorld();this.id=id;this.seq=seq; x=(int)Math.floor(p.posX);z=(int)Math.floor(p.posZ); }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Keep the work budget global, even with several simultaneous players.
        int perJob = Math.max(1, BUDGET / Math.max(1, JOBS.size()));
        Iterator<Job> it=JOBS.iterator();
        while (it.hasNext()) {
            Job j=it.next(); EntityPlayerMP p=j.player;
            if (p.connection == null || p.hasDisconnected()) { it.remove(); continue; }
            if (p.isDead || p.dimension != 0 || p.getServerWorld()!=j.world || !held(p)) {
                reply(p,j.id,j.seq,6,0,0); it.remove(); continue;
            }
            boolean done=false;
            for (int n=0; n<perJob && j.cursor<OFFSETS.size(); n++) {
                int[] d=OFFSETS.get(j.cursor++); int x=j.x+d[0], z=j.z+d[1];
                if (x < -29999984 || x > 29999984 || z < -29999984 || z > 29999984) continue;
                BlockPos pos=new BlockPos(x,64,z);
                Chunk chunk=j.world.getChunkProvider().getLoadedChunk(x >> 4,z >> 4);
                // Reading the stored byte directly avoids Chunk#getBiome mutating an unset biome entry.
                int stored=chunk == null ? 255 : chunk.getBiomeArray()[((z & 15) << 4) | (x & 15)] & 255;
                boolean confirmed=stored != 255;
                Biome at=confirmed ? Biome.getBiome(stored) : j.world.getBiomeProvider().getBiome(pos);
                if (at == biome(j.id)) { reply(p,j.id,j.seq,confirmed ? 0 : 1,x,z); done=true; break; }
            }
            if (!done && j.cursor>=OFFSETS.size()) { reply(p,j.id,j.seq,2,0,0); done=true; }
            if (done) it.remove();
        }
    }
}
