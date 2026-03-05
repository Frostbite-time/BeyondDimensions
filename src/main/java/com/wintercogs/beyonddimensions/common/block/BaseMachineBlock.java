package com.wintercogs.beyonddimensions.common.block;

import com.wintercogs.beyonddimensions.common.block.entity.BaseMachineBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
