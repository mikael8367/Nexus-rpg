package com.nexusrpg.energy;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;

/** Nine cells, one controller. Cells are deliberately kept in a single chunk. */
public final class TileSolarPanel extends TileProducer {
    private BlockPos origin;
    private UUID structure;

    void configure(BlockPos origin, UUID structure) {
        this.origin = origin.toImmutable();
        this.structure = structure;
        markDirty();
        // Block placement sends the tile before this post-placement configuration.
        // Explicitly send its identity to already watching clients as well.
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }
    boolean belongsTo(BlockPos origin, UUID structure) {
        return this.origin != null && this.origin.equals(origin) && structure != null && structure.equals(this.structure);
    }
    BlockPos origin() { return origin; }
    UUID structure() { return structure; }
    private boolean controller() { return origin != null && origin.equals(pos) && structure != null; }

    /** Do not load chunks and do not generate or expose stored energy for a damaged panel. */
    boolean complete() {
        if (world == null || origin == null || structure == null) return false;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos p = origin.add(x, 0, z);
            if (!world.isBlockLoaded(p)) return false;
            TileEntity tile = world.getTileEntity(p);
            if (world.getBlockState(p).getBlock() != getBlockType() || !(tile instanceof TileSolarPanel)
                    || !((TileSolarPanel) tile).belongsTo(origin, structure)) return false;
        }
        return true;
    }
    @Override public void update() {
        if (world == null || world.isRemote || !controller() || !complete()) return;
        if (world.isDaytime() && !world.isRaining() && world.canSeeSky(pos.up()) && energy <= CAPACITY - 40) addEnergy(40);
        emit(256);
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && (!controller() || !complete())) return false;
        return super.hasCapability(capability, facing);
    }
    @Override @Nullable public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY && (!controller() || !complete())) return null;
        return super.getCapability(capability, facing);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (origin != null && structure != null) {
            tag.setLong("SolarOrigin", origin.toLong());
            tag.setUniqueId("SolarStructure", structure);
        }
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        origin = null;
        structure = null;
        if (tag.hasKey("SolarOrigin") && tag.hasUniqueId("SolarStructure")) {
            origin = BlockPos.fromLong(tag.getLong("SolarOrigin"));
            structure = tag.getUniqueId("SolarStructure");
        }
    }
    @Override public NBTTagCompound getUpdateTag() {
        // TileEntity's default update tag contains only vanilla bookkeeping.
        return writeToNBT(new NBTTagCompound());
    }
    @Override public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }
    @Override public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
        if (world != null && world.isRemote) {
            // A cable beside any of the cells may now connect to the controller.
            world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
        }
    }
    @Override public boolean onlyOpsCanSetNbt() { return true; }
}
