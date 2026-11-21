import com.nexusrpg.world.settlement.SettlementPrototype;
import com.nexusrpg.world.settlement.SettlementPrototype.BiomeStyle;
import com.nexusrpg.world.settlement.SettlementPrototype.Pos;
import java.util.Map;

public final class SettlementPrototypeTest {
    private static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
    private static void absent(Map<Pos, String> p, int x, int y, int z) {
        check(!p.containsKey(new Pos(x, y, z)), "Expected space at " + x + "," + y + "," + z);
    }
    private static void cabinGeometry(Map<Pos, String> p, int cx, BiomeStyle s) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            check(s.floor.equals(p.get(new Pos(cx + x, 0, z))), "floor");
            check(s.roof.equals(p.get(new Pos(cx + x, 4 + (3 - Math.abs(x)) / 2, z))), "roof");
            if (Math.abs(x) < 3 && Math.abs(z) < 3) for (int y = 1; y <= 3; y++) absent(p, cx + x, y, z);
        }
        for (int y = 1; y <= 2; y++) { absent(p, cx, y, -3); absent(p, cx, y, -4); }
        check(s.wall.equals(p.get(new Pos(cx, 3, -3))), "lintel");
        absent(p, cx - 3, 2, 0); absent(p, cx + 3, 2, 0);
    }
    private static void badBiome(String id) {
        try { BiomeStyle.fromRegistryName(id); throw new AssertionError("accepted bad biome"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) {
        badBiome(null); badBiome(""); badBiome("enchanted_grove"); badBiome("other:enchanted_grove");
        badBiome("nexusrpg:ENCHANTED_GROVE");
        try { SettlementPrototype.cabin(null); throw new AssertionError("null style"); }
        catch (IllegalArgumentException expected) { }
        try { SettlementPrototype.village(null); throw new AssertionError("null style"); }
        catch (IllegalArgumentException expected) { }
        for (BiomeStyle style : BiomeStyle.values()) {
            check(BiomeStyle.fromRegistryName("nexusrpg:" + style.id) == style, "biome registry id");
            Map<Pos, String> cabin = SettlementPrototype.cabin(style);
            Map<Pos, String> village = SettlementPrototype.village(style);
            check(cabin.equals(SettlementPrototype.cabin(style)), "deterministic cabin");
            check(village.equals(SettlementPrototype.village(style)), "deterministic village");
            check(village.size() == cabin.size() * 3 + 25, "unique village cells");
            cabinGeometry(cabin, 0, style);
            for (int x : new int[] {-9, 0, 9}) cabinGeometry(village, x, style);
            for (int x = -12; x <= 12; x++) {
                check(style.floor.equals(village.get(new Pos(x, 0, -4))), "path gap");
                absent(village, x, 1, -4); absent(village, x, 2, -4);
            }
            for (Map.Entry<Pos, String> e : village.entrySet()) {
                Pos p = e.getKey();
                check(p.x >= -12 && p.x <= 12 && p.z >= -4 && p.z <= 3 && p.y >= 0 && p.y <= 5, "bounds");
                check(e.getValue().equals(style.wall) || e.getValue().equals(style.floor) || e.getValue().equals(style.roof), "palette");
                check(e.getValue().startsWith("minecraft:"), "vanilla IDs only");
            }
            try { village.put(new Pos(100, 100, 100), "x"); throw new AssertionError("mutable plan"); }
            catch (UnsupportedOperationException expected) { }
            try { cabin.clear(); throw new AssertionError("mutable cabin"); }
            catch (UnsupportedOperationException expected) { }
            check(new Pos(1, 2, 3).equals(new Pos(1, 2, 3)), "pos equality");
            check(new Pos(1, 2, 3).hashCode() == new Pos(1, 2, 3).hashCode(), "pos hash");
            check(!new Pos(1, 2, 3).equals(new Pos(1, 3, 2)), "pos distinct");
        }
        System.out.println("Settlement prototype: 8 styles, cabin and village geometry, immutability and rejection OK");
    }
}
