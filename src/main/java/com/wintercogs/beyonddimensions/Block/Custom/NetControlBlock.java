package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
            UIRegister.Factory_NetControlGUI.open((EntityPlayerMP)playerIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

}
