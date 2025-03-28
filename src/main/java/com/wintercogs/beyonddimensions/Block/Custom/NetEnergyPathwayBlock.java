package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;


public class NetEnergyPathwayBlock extends NetedBlock
{

    public NetEnergyPathwayBlock(Material materialIn) {
        super(materialIn);
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new NetEnergyPathwayBlockEntity();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        super.onBlockActivated(worldIn, pos, state, player, hand, facing, hitX, hitY, hitZ);
        if(!worldIn.isRemote&&!player.isSneaking())
        {
            // 打开ui的函数 暂时注释
//            player.openMenu(new SimpleMenuProvider(
//                    (containerId, playerInventory, _player) -> new NetEnergyMenu(containerId, _player.getInventory(), ((NetEnergyPathwayBlockEntity) level.getBlockEntity(pos)),new SimpleContainerData(0)),
//                    Component.translatable("menu.title.beyonddimensions.net_energy_menu")
//            ));
        }
        return true;
    }


}