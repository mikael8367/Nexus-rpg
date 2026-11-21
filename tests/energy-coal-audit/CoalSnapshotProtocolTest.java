package com.nexusrpg.energy;

import java.lang.reflect.Field;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.util.BlockSnapshot;
import sun.misc.Unsafe;

/** Actual Forge BlockSnapshot in a simulated one-cell world, not an integration test. */
public final class CoalSnapshotProtocolTest {
    static final class Cell extends World {
        IBlockState state; TileEntity tile; GameRules rules;
        private Cell(ISaveHandler s, WorldInfo i, WorldProvider p, Profiler r) { super(s,i,p,r,false); }
        protected IChunkProvider createChunkProvider() { return null; }
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        public IBlockState getBlockState(BlockPos p) { return state; }
        public TileEntity getTileEntity(BlockPos p) { return tile; }
        public void removeTileEntity(BlockPos p) { tile = null; }
        public GameRules getGameRules() { return rules; }
        public void notifyBlockUpdate(BlockPos p, IBlockState old, IBlockState next, int flags) { }
        public boolean setBlockState(BlockPos p, IBlockState next, int flags) {
            IBlockState old = state;
            if (old.getBlock() == next.getBlock()) return false;
            state = next; old.getBlock().breakBlock(this,p,old);
            tile = next.getBlock() instanceof BlockPowerMachine ? new TileCoalGenerator() : null;
            return true;
        }
    }
    static void check(boolean b, String msg) { if (!b) throw new AssertionError(msg); }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        Bootstrap.register(); TileEntity.register("nexusrpg:coal_snapshot_protocol", TileCoalGenerator.class);
        Field f=Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        Cell w=(Cell)((Unsafe)f.get(null)).allocateInstance(Cell.class);
        Field provider=World.class.getDeclaredField("provider"); provider.setAccessible(true); provider.set(w,new WorldProviderSurface());
        w.rules=new GameRules();
        BlockPowerMachine block=new BlockPowerMachine("coal_snapshot_protocol",false);
        Field cf=Block.class.getDeclaredField("captureDrops"); cf.setAccessible(true);
        Field df=Block.class.getDeclaredField("capturedDrops"); df.setAccessible(true);
        ThreadLocal<Boolean> capture=(ThreadLocal<Boolean>)cf.get(null);
        ThreadLocal<NonNullList<ItemStack>> drops=(ThreadLocal<NonNullList<ItemStack>>)df.get(null);
        capture.set(true); drops.set(NonNullList.create());
        try {
            for (boolean rollback : new boolean[]{true,false}) {
                w.state=block.getDefaultState(); TileCoalGenerator tile=new TileCoalGenerator();
                NBTTagCompound input=new NBTTagCompound(); input.setString("extra", "keep");
                tile.readFromNBT(input); tile.inventory.insertItem(0,new ItemStack(Items.COAL,10),false); w.tile=tile;
                NBTTagCompound expected=tile.writeToNBT(new NBTTagCompound());
                BlockSnapshot snap=BlockSnapshot.getBlockSnapshot(w, BlockPos.ORIGIN);
                check(expected.equals(snap.getNbt()), "snapshot failed to capture complete NBT");
                w.captureBlockSnapshots=true; w.setBlockState(BlockPos.ORIGIN,Blocks.AIR.getDefaultState(),3); w.captureBlockSnapshots=false;
                check(w.tile==null && drops.get().isEmpty(), "capture emitted a drop or retained tile");
                if (rollback) {
                    w.restoringBlockSnapshots=true;
                    try { check(snap.restore(true), "restore returned false"); } finally { w.restoringBlockSnapshots=false; }
                    check(w.tile instanceof TileCoalGenerator, "rollback tile missing");
                    check(expected.equals(w.tile.writeToNBT(new NBTTagCompound())), "rollback changed NBT");
                    check(drops.get().isEmpty(), "rollback emitted a duplicate");
                } else {
                    check(w.state.getBlock()==Blocks.AIR && w.tile==null && drops.get().isEmpty(), "commit unexpectedly recovered data");
                }
            }
            System.out.println("PASS real BlockSnapshot with simulated cell: rollback restores NBT without drops; commit demonstrates loss");
        } finally { capture.remove(); drops.remove(); }
    }
}
