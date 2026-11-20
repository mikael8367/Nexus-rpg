package com.nexusrpg.energy;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/** Stateless FE conduit. Traverses loaded cables only, with bounded cycle detection. */
public final class TileEnergyCable extends TileEntity {
    private static final int MAX_CABLES = 4096;
    private static final ThreadLocal<Boolean> TRANSFERRING = ThreadLocal.withInitial(() -> false);
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }
    @Override @Nullable public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return CapabilityEnergy.ENERGY.cast(new Port(facing));
        return super.getCapability(capability, facing);
    }
    private final class Port implements IEnergyStorage {
        private final EnumFacing inputSide;
        private Port(@Nullable EnumFacing inputSide) { this.inputSide = inputSide; }
        public int receiveEnergy(int requested, boolean simulate) {
            if (requested <= 0 || world == null || world.isRemote || isInvalid() || TRANSFERRING.get()) return 0;
            TRANSFERRING.set(true);
            try { return distribute(requested, simulate, inputSide == null ? null : pos.offset(inputSide)); }
            finally { TRANSFERRING.remove(); }
        }
        public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        public int getEnergyStored() { return 0; }
        public int getMaxEnergyStored() { return 0; }
        public boolean canExtract() { return false; }
        public boolean canReceive() { return true; }
    }
    private int distribute(int amount, boolean simulate, @Nullable BlockPos source) {
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> endpoints = new HashSet<>();
        pending.add(pos); visited.add(pos);
        int acceptedTotal = 0;
        while (!pending.isEmpty() && acceptedTotal < amount) {
            BlockPos current = pending.removeFirst();
            for (EnumFacing side : EnumFacing.VALUES) {
                BlockPos next = current.offset(side);
                if (next.equals(source) || !world.isBlockLoaded(next)) continue;
                TileEntity tile = world.getTileEntity(next);
                if (tile == null) continue;
                if (tile instanceof TileEnergyCable) {
                    if (visited.size() < MAX_CABLES && visited.add(next)) pending.addLast(next);
                    continue;
                }
                if (endpoints.contains(next)) continue;
                IEnergyStorage sink = tile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
                if (sink == null || !sink.canReceive()) continue;
                int remaining = amount - acceptedTotal;
                int accepted = sink.receiveEnergy(remaining, simulate);
                accepted = Math.max(0, Math.min(remaining, accepted));
                // A different face may accept energy after this face refused it.
                // Once one accepts, never count the same storage twice in a simulation.
                if (accepted > 0) endpoints.add(next);
                acceptedTotal += accepted;
                if (acceptedTotal == amount) break;
            }
        }
        return acceptedTotal;
    }
}
