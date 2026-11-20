package com.nexusrpg.energy;

import com.nexusrpg.NexusRpg;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

public final class BlockEnergyCable extends Block {
    public static final PropertyBool DOWN = PropertyBool.create("down"), UP = PropertyBool.create("up"),
        NORTH = PropertyBool.create("north"), SOUTH = PropertyBool.create("south"),
        WEST = PropertyBool.create("west"), EAST = PropertyBool.create("east");
    private static final PropertyBool[] SIDES = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
    public BlockEnergyCable() {
        super(Material.IRON);
        setRegistryName(NexusRpg.MODID, "energy_cable"); setUnlocalizedName(NexusRpg.MODID + ".energy_cable");
        setCreativeTab(CreativeTabs.REDSTONE); setHardness(1.5F); setHarvestLevel("pickaxe", 0);
        IBlockState state = blockState.getBaseState();
        for (PropertyBool property : SIDES) state = state.withProperty(property, false);
        setDefaultState(state);
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, DOWN, UP, NORTH, SOUTH, WEST, EAST); }
    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        for (EnumFacing side : EnumFacing.VALUES) {
            BlockPos neighbor = pos.offset(side);
            boolean connected = false;
            if (!(world instanceof World) || ((World) world).isBlockLoaded(neighbor)) {
                TileEntity tile = world.getTileEntity(neighbor);
                connected = tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            }
            state = state.withProperty(SIDES[side.getIndex()], connected);
        }
        return state;
    }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new TileEnergyCable(); }
}
