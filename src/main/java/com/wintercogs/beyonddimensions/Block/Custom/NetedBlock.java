package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Unit.CapabilityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class NetedBlock extends Block
{

    public NetedBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        // 空手右键可以设定网络接口所绑定的网络
        if(!player.getMainHandItem().isEmpty()||!player.isShiftKeyDown())
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
                        // 成功设置网络id
                        blockEntity.setNetId(net.getId());
                        blockEntity.invalidateCaps();
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
                                blockEntity.invalidateCaps();
                            }
                        }
                    }
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

}
