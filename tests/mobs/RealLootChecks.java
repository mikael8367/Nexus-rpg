import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;

/** Requires a bootstrappable Forge runtime; not a substitute for gameplay tests. */
public final class RealLootChecks {
    public static void main(String[] args) {
        Bootstrap.register();
        LootTableManager manager = new LootTableManager(null);
        check(manager, "bramble_hound", "bramble_common", "bramble_extra");
        check(manager, "cavern_crawler", "cavern_common", "cavern_extra");
        check(manager, "basalt_guardian", "basalt_common", "basalt_extra");
        check(manager, "reed_stilt", "reed_stilt_common", "reed_stilt_extra");
        check(manager, "prism_tortoise", "prism_tortoise_common", "prism_tortoise_extra");
        check(manager, "lumen_moth", "lumen_moth_common", "lumen_moth_extra");
        check(manager, "dune_mole", "dune_mole_common", "dune_mole_extra");
        check(manager, "bellback", "bellback_common", "bellback_extra");
        check(manager, "dune_burrower", "dune_burrower_common", "dune_burrower_extra");
        check(manager, "root_warden", "root_warden_common", "root_warden_extra");
        System.out.println("PASS: real loot table loader found all twenty pools");
    }
    private static void check(LootTableManager m, String id, String first, String second) {
        LootTable t = m.getLootTableFromLocation(new ResourceLocation("nexusrpg", "entities/" + id));
        if (t == LootTable.EMPTY_LOOT_TABLE || t.getPool(first) == null || t.getPool(second) == null)
            throw new AssertionError("loot not loaded: " + id);
    }
}
