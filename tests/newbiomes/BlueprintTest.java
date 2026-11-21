import com.nexusrpg.world.newbiomes.FeatureBlueprint;
import com.nexusrpg.world.newbiomes.FeatureBlueprint.*;
import java.util.*;

public class BlueprintTest {
    static int checks;
    static void check(boolean b, String message) { checks++; if (!b) throw new AssertionError(message); }
    static class Fake implements Probe {
        boolean loaded=true, border=true, biome=true, cancel=false;
        int cancelAfter=Integer.MAX_VALUE, calls, occupiedAt=-1, tilesAt=-1, writes;
        public boolean cancelled() { return cancel || calls>=cancelAfter; }
        public boolean sameLoadedChunk(int x,int z) { return loaded; }
        public boolean withinBorder(Pos p) { return border; }
        public boolean intendedBiome(Pos p) { return biome; }
        public boolean airWithoutTile(Pos p) { int n=calls++; return n!=occupiedAt && n!=tilesAt; }
    }
    public static void main(String[] args) {
        long[] seeds={0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE,91823983,123456789};
        int[] chunks={-1875000,-31,-2,-1,0,1,2,31,1874999};
        Set<String> layouts=new HashSet<String>();
        for(int style=0;style<6;style++) for(long seed:seeds) for(int cx:chunks) for(int cz:chunks) {
            Map<Pos,Material> p=FeatureBlueprint.plan(style,seed,cx,cz,64);
            check(p.equals(FeatureBlueprint.plan(style,seed,cx,cz,64)),"deterministic");
            check(p.size()>=10 && p.size()<=150,"cardinality " + p.size());
            Set<Material> types=new HashSet<Material>();
            for(Map.Entry<Pos,Material> e:p.entrySet()) {
                Pos q=e.getKey(); types.add(e.getValue());
                check((q.x>>4)==cx && (q.z>>4)==cz,"chunk boundary");
                check((q.x&15)>=1 && (q.x&15)<=14 && (q.z&15)>=1 && (q.z&15)<=14,"margin");
                check(q.y>=64 && q.y<256,"height");
            }
            check(types.size()==3,"tree and structure materials");
            Fake f=new Fake(); check(FeatureBlueprint.preflight(p,cx,cz,f),"empty space");
            check(f.writes==0,"read only");
            if(cx==0 && cz==0 && seed==0) layouts.add(types.toString());
        }
        check(layouts.size()==6,"six distinct palettes");
        Random samples=new Random(534945L);
        for(int s=0;s<6;s++) for(int n=0;n<256;n++) {
            long seed=samples.nextLong();
            int cx=chunks[n%chunks.length], cz=chunks[(n/3)%chunks.length];
            int y=n%2==0?1:240;
            Map<Pos,Material> p=FeatureBlueprint.plan(s,seed,cx,cz,y);
            check(FeatureBlueprint.preflight(p,cx,cz,new Fake()),"edge-height random seed");
            for(Pos q:p.keySet()) check(q.y>=y && q.y<256 && (q.x>>4)==cx && (q.z>>4)==cz,"extreme placement");
        }
        for(int s=0;s<6;s++) {
            Map<Pos,Material> p=FeatureBlueprint.plan(s,3,-1,0,64);
            for(int slot=0;slot<p.size();slot++) {
                Fake occupied=new Fake(); occupied.occupiedAt=slot;
                check(!FeatureBlueprint.preflight(p,-1,0,occupied),"occupied position rejected");
                Fake tile=new Fake(); tile.tilesAt=slot;
                check(!FeatureBlueprint.preflight(p,-1,0,tile),"tile rejected");
                Fake cancelled=new Fake(); cancelled.cancelAfter=slot;
                check(!FeatureBlueprint.preflight(p,-1,0,cancelled),"cancellation rejected");
                check(occupied.writes+tile.writes+cancelled.writes==0,"no mutation");
            }
            Fake unloaded=new Fake(); unloaded.loaded=false;
            check(!FeatureBlueprint.preflight(p,-1,0,unloaded),"not loaded");
            Fake wrong=new Fake(); wrong.biome=false;
            check(!FeatureBlueprint.preflight(p,-1,0,wrong),"foreign biome");
            Fake out=new Fake(); out.border=false;
            check(!FeatureBlueprint.preflight(p,-1,0,out),"border");
            Fake cancel=new Fake(); cancel.cancel=true;
            check(!FeatureBlueprint.preflight(p,-1,0,cancel),"pre-cancel");
            check(!FeatureBlueprint.preflight(p,0,0,new Fake()),"wrong chunk");
            try { p.clear(); throw new AssertionError("mutable"); } catch (UnsupportedOperationException expected) { checks++; }
        }
        Map<Pos,Material> malicious=new HashMap<Pos,Material>();
        malicious.put(new Pos(16,64,0),Material.ICE);
        check(!FeatureBlueprint.preflight(malicious,0,0,new Fake()),"cross-border forged plan");
        malicious.clear(); malicious.put(new Pos(0,256,0),Material.ICE);
        check(!FeatureBlueprint.preflight(malicious,0,0,new Fake()),"out of height");
        for(int value:new int[]{-1,6}) try { FeatureBlueprint.plan(value,0,0,0,64); throw new AssertionError("style"); }
        catch(IllegalArgumentException expected) { checks++; }
        for(int y:new int[]{0,241}) try { FeatureBlueprint.plan(0,0,0,0,y); throw new AssertionError("y"); }
        catch(IllegalArgumentException expected) { checks++; }
        System.out.println("BlueprintTest: " + checks + " checks passed");
    }
}
