package com.nexusrpg.world.structures;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Additive single-chunk tower plan. The generator owns safety, loot and writes. */
public final class WizardTower {
    private static final Logger LOG = Logger.getLogger("NexusRpg.WizardTower");
    private static volatile SpawnHandler spawnHandler;

    /** Register at startup. Called once after a tower and its chest are committed.
     * The feet position is on the upper floor; this is not a persistent spawner.
     * Handler must only spawn server-side and should report a failed spawn itself.
     */
    public interface SpawnHandler { void spawn(World world, BlockPos feet); }
    public static void setSpawnHandler(SpawnHandler handler) { spawnHandler = handler; }
    private WizardTower() { }

    static boolean supports(int style) { return style == 0 || style == 3 || style == 6; }

    static void build(Map<BlockPos, IBlockState> p, int x, int y, int z, int style) {
        if (!supports(style)) throw new IllegalArgumentException("Unsupported tower biome");
        Block wall = style == 3 ? Blocks.MOSSY_COBBLESTONE : style == 6 ? Blocks.END_BRICKS : Blocks.STONEBRICK;
        Block accent = style == 6 ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
        // Square shell, with a two-block-high north doorway and narrow glazed windows.
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (Math.abs(dx) != 3 && Math.abs(dz) != 3) continue;
            for (int h = 1; h <= 8; h++) {
                if (dx == 0 && dz == -3 && h <= 2) continue;
                boolean window = (h == 3 || h == 4 || h == 6 || h == 7) &&
                    ((Math.abs(dx) == 3 && dz == 0) || (Math.abs(dz) == 3 && dx == 0));
                put(p,x,y,z,dx,h,dz,window ? Blocks.GLASS_PANE :
                    (h == 8 || (Math.abs(dx) == 3 && Math.abs(dz) == 3) ? accent : wall));
            }
            put(p,x,y,z,dx,9,dz,accent);
            if ((dx + dz) % 2 == 0) put(p,x,y,z,dx,10,dz,wall);
        }
        // Four jumpable full-block steps rise to an upper gallery. The chest and
        // lid remain clear at the center of the lower floor.
        put(p,x,y,z,1,1,-1,Blocks.PLANKS);
        put(p,x,y,z,2,2,-1,Blocks.PLANKS);
        put(p,x,y,z,2,3,0,Blocks.PLANKS);
        put(p,x,y,z,2,4,1,Blocks.PLANKS);
        for (int dx = -2; dx <= 1; dx++) for (int dz = 0; dz <= 2; dz++)
            put(p,x,y,z,dx,4,dz,Blocks.PLANKS);
        // Solid roof and central arcane lantern; a 3-block-tall gallery below.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
            put(p,x,y,z,dx,9,dz,dx == 0 && dz == 0 ? Blocks.GLOWSTONE : wall);
    }
    static BlockPos spawnFeet(int x, int y, int z) { return new BlockPos(x - 1, y + 5, z + 1); }
    static void afterGenerated(World world, BlockPos feet) {
        SpawnHandler handler = spawnHandler;
        if (handler == null) return;
        try { handler.spawn(world, feet); }
        catch (RuntimeException e) { LOG.log(Level.WARNING, "Wizard tower spawn handler failed at " + feet, e); }
    }
    private static void put(Map<BlockPos,IBlockState> p, int x,int y,int z,int dx,int dy,int dz,Block b) {
        p.put(new BlockPos(x + dx, y + dy, z + dz), b.getDefaultState());
    }
}
