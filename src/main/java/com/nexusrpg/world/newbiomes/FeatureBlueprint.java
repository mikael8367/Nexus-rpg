package com.nexusrpg.world.newbiomes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Pure blueprints; not connected to any world or decoration callback. */
public final class FeatureBlueprint {
    public enum Material {
        ACACIA_LOG, ACACIA_LEAVES, BIRCH_LOG, BIRCH_LEAVES, SPRUCE_LOG, SPRUCE_LEAVES,
        JUNGLE_LOG, JUNGLE_LEAVES, DARK_OAK_LOG, DARK_OAK_LEAVES, OAK_LOG, OAK_LEAVES,
        SANDSTONE, MOSSY_STONE, ICE, RED_SANDSTONE, PRISMARINE, NETHER_BRICK
    }
    public static final class Pos {
        public final int x, y, z;
        public Pos(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
        @Override public int hashCode() { return (x * 31 + y) * 31 + z; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Pos)) return false;
            Pos p=(Pos)o; return x==p.x && y==p.y && z==p.z;
        }
    }
    /** Read-only adapter contract. Checking never authorizes a write. */
    public interface Probe {
        boolean cancelled();
        boolean sameLoadedChunk(int chunkX, int chunkZ);
        boolean withinBorder(Pos p);
        boolean intendedBiome(Pos p);
        boolean airWithoutTile(Pos p);
    }
    private FeatureBlueprint() {}
    private static final Material[] LOG = {Material.ACACIA_LOG,Material.BIRCH_LOG,Material.SPRUCE_LOG,
        Material.JUNGLE_LOG,Material.DARK_OAK_LOG,Material.OAK_LOG};
    private static final Material[] LEAF = {Material.ACACIA_LEAVES,Material.BIRCH_LEAVES,Material.SPRUCE_LEAVES,
        Material.JUNGLE_LEAVES,Material.DARK_OAK_LEAVES,Material.OAK_LEAVES};
    private static final Material[] STONE = {Material.SANDSTONE,Material.MOSSY_STONE,Material.ICE,
        Material.RED_SANDSTONE,Material.PRISMARINE,Material.NETHER_BRICK};

    /** y is first air above a uniformly flat surface, never modifies ground. */
    public static Map<Pos,Material> plan(int style, long seed, int chunkX, int chunkZ, int y) {
        if (style < 0 || style >= 6 || y < 1 || y > 240 ||
            chunkX < -1875000 || chunkX > 1874999 || chunkZ < -1875000 || chunkZ > 1874999)
            throw new IllegalArgumentException();
        int ox=chunkX*16, oz=chunkZ*16;
        Map<Pos,Material> m=new LinkedHashMap<Pos,Material>();
        Random r=new Random(seed ^ ((long)chunkX * 341873128712L) ^ ((long)chunkZ * 132897987541L) ^ style);
        int h=4+r.nextInt(3);
        int tx=ox+3, tz=oz+3;
        for (int i=0; i<h; i++) put(m,tx,y+i,tz,LOG[style]);
        switch(style) {
            case 0: // wide, flat acacia crown and asymmetrical branch
                put(m,tx+1,y+h-2,tz,LOG[style]); put(m,tx+2,y+h-2,tz,LOG[style]);
                canopy(m,tx+1,y+h-1,tz,2,LEAF[style]); break;
            case 1: // birch with narrow pointed crown
                canopy(m,tx,y+h-2,tz,1,LEAF[style]); canopy(m,tx,y+h-1,tz,1,LEAF[style]);
                put(m,tx,y+h,tz,LEAF[style]); break;
            case 2: // tiered spruce cone
                canopy(m,tx,y+h-3,tz,2,LEAF[style]); canopy(m,tx,y+h-1,tz,1,LEAF[style]);
                put(m,tx,y+h,tz,LEAF[style]); break;
            case 3: // palm-like jungle, four fronds
                for (int d=1;d<=2;d++) { put(m,tx+d,y+h-1,tz,LEAF[style]); put(m,tx-d,y+h-1,tz,LEAF[style]);
                    put(m,tx,y+h-1,tz+d,LEAF[style]); put(m,tx,y+h-1,tz-d,LEAF[style]); }
                put(m,tx,y+h,tz,LEAF[style]); break;
            case 4: // dark oak umbrella with dangling foliage
                canopy(m,tx,y+h-1,tz,2,LEAF[style]);
                put(m,tx-2,y+h-2,tz-2,LEAF[style]); put(m,tx+2,y+h-2,tz+2,LEAF[style]); break;
            case 5: // sparse surviving oak
                put(m,tx+1,y+h-2,tz,LOG[style]);
                put(m,tx+2,y+h-2,tz,LEAF[style]); put(m,tx-1,y+h-1,tz,LEAF[style]);
                put(m,tx,y+h,tz,LEAF[style]); break;
            default: throw new AssertionError();
        }
        int x=ox+10,z=oz+10; Material b=STONE[style];
        switch(style) {
            case 0: // standing stones
                for(int a=-2;a<=2;a+=4) for(int i=0;i<3;i++) put(m,x+a,y+i,z,b);
                put(m,x,y,z+2,b); break;
            case 1: // broken arched gate
                for(int i=0;i<4;i++) { put(m,x-2,y+i,z,b); put(m,x+2,y+i,z,b); }
                for(int a=-1;a<=1;a++) put(m,x+a,y+3,z,b); break;
            case 2: // crystalline spire with four lower buttresses
                for(int i=0;i<6;i++) put(m,x,y+i,z,b);
                for(int i=0;i<2;i++) { put(m,x-1,y+i,z,b); put(m,x+1,y+i,z,b);
                    put(m,x,y+i,z-1,b); put(m,x,y+i,z+1,b); } break;
            case 3: // four pillars and crossbeam
                for(int a=-2;a<=2;a+=4) for(int c=-2;c<=2;c+=4)
                    for(int i=0;i<3;i++) put(m,x+a,y+i,z+c,b);
                for(int a=-1;a<=1;a++) put(m,x+a,y+2,z-2,b); break;
            case 4: // stepped cross-shaped altar, without chest
                for(int a=-2;a<=2;a++) { put(m,x+a,y,z,b); put(m,x,y,z+a,b); }
                for(int i=1;i<4;i++) put(m,x,y+i,z,b); break;
            case 5: // angular ruined chimney
                for(int i=0;i<5;i++) { put(m,x-1,y+i,z,b); put(m,x+1,y+i,z,b); }
                for(int a=-1;a<=1;a++) put(m,x+a,y+4,z,b);
                put(m,x-1,y,z+1,b); put(m,x+1,y,z+1,b); break;
            default: throw new AssertionError();
        }
        return Collections.unmodifiableMap(m);
    }
    private static void canopy(Map<Pos,Material> m,int x,int y,int z,int radius,Material leaf) {
        for(int a=-radius;a<=radius;a++) for(int b=-radius;b<=radius;b++) {
            if (Math.abs(a)+Math.abs(b)<=radius+1) {
                Pos p=new Pos(x+a,y,z+b);
                if (!m.containsKey(p)) m.put(p,leaf);
            }
        }
    }
    private static void put(Map<Pos,Material> m,int x,int y,int z,Material b) {
        Pos p=new Pos(x,y,z); Material old=m.put(p,b);
        if (old != null && old != b) throw new IllegalStateException("blueprint overlap");
    }
    /** Strict read-only rejection of non-air, tile entities, cancellation or unloaded chunks. */
    public static boolean preflight(Map<Pos,Material> plan, int cx, int cz, Probe probe) {
        if (plan == null || plan.isEmpty() || probe == null || probe.cancelled() || !probe.sameLoadedChunk(cx,cz)) return false;
        for (Map.Entry<Pos,Material> e: plan.entrySet()) {
            Pos p=e.getKey();
            if (p==null || e.getValue()==null || (p.x >> 4)!=cx || (p.z >> 4)!=cz || p.y<1 || p.y>=256 ||
                probe.cancelled() || !probe.sameLoadedChunk(cx,cz) || !probe.withinBorder(p) ||
                !probe.intendedBiome(p) || !probe.airWithoutTile(p)) return false;
        }
        return !probe.cancelled() && probe.sameLoadedChunk(cx,cz);
    }
}
