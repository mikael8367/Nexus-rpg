package com.nexusrpg.energy;

import java.lang.reflect.Field;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import sun.misc.Unsafe;

/** Headless regression for data carried by breakBlock's captured drops. */
public final class CoalBreakPreservationTest {
    static final class TestWorld extends World {
        TileEntity tile;
        GameRules rules;
        private TestWorld(ISaveHandler save, WorldInfo info, WorldProvider provider, Profiler profiler) {
            super(save, info, provider, profiler, false);
        }
        protected IChunkProvider createChunkProvider() { return null; }
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
        @Override public TileEntity getTileEntity(BlockPos pos) { return tile; }
        @Override public void removeTileEntity(BlockPos pos) { tile = null; }
        @Override public GameRules getGameRules() { return rules; }
    }
    static boolean contains(NBTBase tree, NBTBase expected) {
        if (expected.equals(tree)) return true;
        if (tree instanceof NBTTagCompound) {
            NBTTagCompound c = (NBTTagCompound) tree;
            for (String key : c.getKeySet()) if (contains(c.getTag(key), expected)) return true;
        } else if (tree instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tree;
            for (int i = 0; i < list.tagCount(); i++) if (contains(list.get(i), expected)) return true;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        TileEntity.register("nexusrpg:coal_break_audit", TileCoalGenerator.class);
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        TestWorld world = (TestWorld) ((Unsafe) f.get(null)).allocateInstance(TestWorld.class);
        world.rules = new GameRules();
        TileCoalGenerator tile = new TileCoalGenerator();
        NBTTagCompound input = new NBTTagCompound();
        input.setString("FuelInventory", "opaque recovery data");
        NBTBase raw = input.getTag("FuelInventory").copy();
        tile.readFromNBT(input); world.tile = tile;
        BlockPowerMachine block = new BlockPowerMachine("coal_break_audit", false);
        Field captureField = Block.class.getDeclaredField("captureDrops"); captureField.setAccessible(true);
        Field dropsField = Block.class.getDeclaredField("capturedDrops"); dropsField.setAccessible(true);
        ThreadLocal<Boolean> capture = (ThreadLocal<Boolean>) captureField.get(null);
        ThreadLocal<NonNullList<ItemStack>> captured = (ThreadLocal<NonNullList<ItemStack>>) dropsField.get(null);
        capture.set(true); captured.set(NonNullList.create());
        try {
            block.breakBlock(world, BlockPos.ORIGIN, block.getDefaultState());
            boolean recovered = world.tile instanceof TileCoalGenerator
                && contains(world.tile.writeToNBT(new NBTTagCompound()), raw);
            for (ItemStack stack : captured.get())
                if (stack.hasTagCompound() && contains(stack.getTagCompound(), raw)) recovered = true;
            if (!recovered) throw new AssertionError("DATA LOSS: breakBlock removed opaque archive without a recovery carrier");
            System.out.println("PASS opaque archive survives breakBlock");
        } finally { capture.remove(); captured.remove(); }
    }
}
