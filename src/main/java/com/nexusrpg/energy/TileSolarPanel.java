package com.nexusrpg.energy;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
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
    }
    boolean belongsTo(BlockPos origin, UUID structure) {
        return this.origin != null && this.origin.equals(origin) && structure != null && structure.equals(this.structure);
    }
    BlockPos origin() { return origin; }
    UUID structure() { return structure; }
    private boolean controller() { return origin != null && origin.equals(pos) && structure != null; }

    /** Do not load chunks and do not generate or expose stored energy for a damaged panel. */
    private boolean complete() {
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
    @Override public boolean onlyOpsCanSetNbt() { return true; }
}
