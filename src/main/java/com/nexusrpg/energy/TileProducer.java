package com.nexusrpg.energy;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

public abstract class TileProducer extends TileEntity implements ITickable {
    protected static final int CAPACITY = 100000;
    protected int energy;
    // Prevent a receiver from pulling the FE currently being pushed to it.
    private boolean emitting;
    private final IEnergyStorage output = new IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) { return 0; }
        public int extractEnergy(int amount, boolean simulate) {
            if (emitting) return 0;
            int taken = Math.min(Math.max(0, amount), energy);
            if (!simulate && taken > 0) { energy -= taken; markDirty(); }
            return taken;
        }
        public int getEnergyStored() { return energy; }
        public int getMaxEnergyStored() { return CAPACITY; }
        public boolean canExtract() { return !emitting; }
        public boolean canReceive() { return false; }
    };
    int guiEnergy() { return energy; }
    protected void addEnergy(int amount) { energy = Math.min(CAPACITY, energy + amount); markDirty(); }
    protected void emit(int max) {
        if (emitting || world == null || world.isRemote || isInvalid()) return;
        emitting = true;
        try {
            int remaining = Math.min(Math.max(0, max), energy);
            for (EnumFacing side : EnumFacing.VALUES) {
                if (remaining <= 0) break;
                BlockPos target = pos.offset(side);
                if (!world.isBlockLoaded(target)) continue;
                TileEntity neighbor = world.getTileEntity(target);
                if (neighbor == null) continue;
                IEnergyStorage sink = neighbor.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
                if (sink == null || !sink.canReceive()) continue;
                int accepted = sink.receiveEnergy(remaining, false);
                accepted = Math.max(0, Math.min(remaining, accepted));
                energy -= accepted;
                remaining -= accepted;
                if (accepted > 0) markDirty();
            }
        } finally { emitting = false; }
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }
    @Override @Nullable public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(output);
        return super.getCapability(capability, facing);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); tag.setInteger("Energy", energy); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); energy = Math.max(0, Math.min(CAPACITY, tag.getInteger("Energy"))); }
}
