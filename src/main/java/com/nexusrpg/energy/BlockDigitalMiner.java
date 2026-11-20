package com.nexusrpg.energy;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Right-click transfers as much as possible of the first output stack to the player.
 * A hopper or item pipe may extract output from any side. FE enters any side.
 * The placing player's identity is used for Forge break-protection events. */
public class BlockDigitalMiner extends Block {
    public BlockDigitalMiner() {
        super(Material.IRON);
        setUnlocalizedName("nexusrpg.digital_miner");
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(4.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
    }

    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new TileDigitalMiner(); }

    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                          EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote && placer instanceof EntityPlayer) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileDigitalMiner) ((TileDigitalMiner) tile).setOwner((EntityPlayer) placer);
        }
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote && hand == EnumHand.MAIN_HAND) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileDigitalMiner) ((TileDigitalMiner) tile).giveFirstStack(player);
        }
        return true;
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileDigitalMiner) ((TileDigitalMiner) tile).dropInventory();
        }
        super.breakBlock(world, pos, state);
    }
}
