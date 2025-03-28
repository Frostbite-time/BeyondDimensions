package com.wintercogs.beyonddimensions.Block.Custom;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class NetControlBlock extends Block
{
    public NetControlBlock(Material materialIn)
    {
        super(materialIn);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
        if(!worldIn.isRemote)
        {
            // 打开ui逻辑 暂时注释
//            player.openMenu(new SimpleMenuProvider(
//                    (containerId, playerInventory, _player) -> new NetControlMenu(containerId,playerInventory),
//                    Component.translatable("menu.title.beyonddimensions.net_control_menu")
//            ));
        }
        return true;
    }

}
