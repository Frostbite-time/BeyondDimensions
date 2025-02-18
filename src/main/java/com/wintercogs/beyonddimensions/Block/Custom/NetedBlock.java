package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.Block.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class NetedBlock extends Block
{

    public NetedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        // 空手右键可以设定网络接口所绑定的网络
        if(!player.getMainHandItem().isEmpty())
        {
            return InteractionResult.PASS;
        }
        if(!level.isClientSide())
        {
            if(level.getBlockEntity(pos) instanceof NetedBlockEntity blockEntity)
            {
                if(blockEntity.getNetId() == -1)
                {
                    DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
                    if(net != null)
                    {
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
                                blockEntity.setNetId(-1);
                            }
                        }
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

}
