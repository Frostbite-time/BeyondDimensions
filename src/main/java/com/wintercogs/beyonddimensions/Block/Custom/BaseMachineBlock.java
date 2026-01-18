package com.wintercogs.beyonddimensions.Block.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.BaseMachineBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public abstract class BaseMachineBlock extends NetedBlock implements EntityBlock
{
    public BaseMachineBlock(Properties properties)
    {
        super(properties);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide())
            return null;

        return (level1, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof BaseMachineBlockEntity machine)
            {
                BaseMachineBlockEntity.tick(level1, blockPos, blockState, machine);
            }
        };
    }
}

