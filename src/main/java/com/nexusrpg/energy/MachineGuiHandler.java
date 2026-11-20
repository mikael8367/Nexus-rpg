package com.nexusrpg.energy;

import com.nexusrpg.NexusRpg;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The server always resolves and validates the actual tile, never client-provided state. */
public final class MachineGuiHandler implements IGuiHandler {
    static final int GUI = 0;
    static void open(EntityPlayer player, World world, BlockPos pos) {
        if (!world.isRemote && world.isBlockLoaded(pos) && player.getDistanceSq(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64.0) {
            Object mod = Loader.instance().getIndexedModList().get(NexusRpg.MODID).getMod();
            player.openGui(mod, GUI, world, pos.getX(), pos.getY(), pos.getZ());
        }
    }
    private static TileEntity tile(int id, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (id != GUI || !world.isBlockLoaded(pos)) return null;
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileCoalGenerator && world.getBlockState(pos).getBlock() == EnergyRegistry.COAL_GENERATOR) return tile;
        if (tile instanceof TileDigitalMiner && world.getBlockState(pos).getBlock() == EnergyRegistry.MINER) return tile;
        if (tile instanceof TileSolarPanel && world.getBlockState(pos).getBlock() == EnergyRegistry.SOLAR_PANEL) return tile;
        return null;
    }
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = tile(id, world, x, y, z);
        if (tile == null || player.getDistanceSq(x + .5, y + .5, z + .5) > 64.0) return null;
        if (tile instanceof TileSolarPanel) {
            TileSolarPanel solar = (TileSolarPanel) tile;
            if (!tile.getPos().equals(solar.origin()) || solar.structure() == null) return null;
        }
        return new MachineContainer(player.inventory, tile);
    }
    @SideOnly(Side.CLIENT)
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = tile(id, world, x, y, z);
        return tile == null ? null : new MachineScreen(new MachineContainer(player.inventory, tile));
    }
}
