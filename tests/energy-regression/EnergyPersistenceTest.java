package com.nexusrpg.energy;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/** Runs against the real mapped Minecraft/Forge classes, not energy stubs. */
public final class EnergyPersistenceTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        TileEntity.register("nexusrpg:test_solar", TileSolarPanel.class);
        TileEntity.register("nexusrpg:test_miner", TileDigitalMiner.class);
        if (args.length == 0 || args[0].equals("solar")) {
        BlockPos origin = new BlockPos(10, 65, 20);
        UUID id = UUID.fromString("e6763094-e162-45c1-8c57-355844ec6378");
        TileSolarPanel server = new TileSolarPanel();
        server.setPos(origin);
        server.configure(origin, id);
        NBTTagCompound initial = server.getUpdateTag();
        TileSolarPanel fromChunk = new TileSolarPanel();
        fromChunk.handleUpdateTag(initial);
        check(fromChunk.belongsTo(origin, id), "chunk sync lost solar identity");
        SPacketUpdateTileEntity packet = server.getUpdatePacket();
        check(packet != null, "no solar tile update packet");
        TileSolarPanel fromPacket = new TileSolarPanel();
        fromPacket.onDataPacket(null, packet);
        check(fromPacket.belongsTo(origin, id), "packet sync lost solar identity");
        NBTTagCompound persisted = server.writeToNBT(new NBTTagCompound());
        TileSolarPanel loaded = new TileSolarPanel();
        loaded.readFromNBT(persisted);
        check(loaded.belongsTo(origin, id), "solar identity did not persist");
        }
        if (args.length == 0 || args[0].equals("miner")) for (int size : new int[] { 0, 1, 27, 1000000, -1 }) {
            TileDigitalMiner miner = new TileDigitalMiner();
            NBTTagCompound tag = new NBTTagCompound();
            NBTTagCompound output = new NBTTagCompound();
            output.setInteger("Size", size);
            tag.setTag("Output", output);
            miner.readFromNBT(tag);
            check(miner.guiInventory().getSlots() == 27, "invalid output size " + size);
            miner.guiInventory().getStackInSlot(26);
            check(tag.getCompoundTag("Output").getInteger("Size") == size, "input NBT was mutated");
            NBTTagCompound saved = miner.writeToNBT(new NBTTagCompound());
            if (size != 27) {
                check(saved.hasKey("NexusRpgOriginalOutput", 10), "missing original output backup");
                check(saved.getCompoundTag("NexusRpgOriginalOutput").getInteger("Size") == size, "backup changed");
                TileDigitalMiner reloaded = new TileDigitalMiner();
                reloaded.readFromNBT(saved);
                check(reloaded.writeToNBT(new NBTTagCompound()).getCompoundTag("NexusRpgOriginalOutput").getInteger("Size") == size, "backup lost on reload");
            }
        }
        System.out.println("PASS solar chunk/packet/save identity; miner 5 serialized sizes and input immutability");
    }
}
