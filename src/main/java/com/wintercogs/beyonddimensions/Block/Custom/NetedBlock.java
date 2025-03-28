package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NetedBlock extends Block
{

    public NetedBlock(Material materialIn) {
        super(materialIn);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        super.onBlockActivated(worldIn, pos, state, player, hand, facing, hitX, hitY, hitZ);
        // 空手右键可以设定网络接口所绑定的网络
        if(!player.getHeldItemMainhand().isEmpty()||!player.isSneaking())
        {
            return false;
        }
        if(!worldIn.isRemote)
        {
            if(worldIn.getTileEntity(pos) instanceof NetedBlockEntity blockEntity)
            {
                if(blockEntity.getNetId() == -1)
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net != null)
                    {
                        // 成功设置网络id
                        blockEntity.setNetId(net.getId());
                    }
                }
                else
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net != null)
                    {
                        if(net.getId() == blockEntity.getNetId())
                        {
                            if(net.isManager(player))
                            {
                                // 成功清除网络id
                                blockEntity.setNetId(-1);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

}
