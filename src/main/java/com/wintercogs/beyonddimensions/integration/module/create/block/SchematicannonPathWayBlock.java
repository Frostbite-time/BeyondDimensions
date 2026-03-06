package com.wintercogs.beyonddimensions.integration.module.create.block;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import com.wintercogs.beyonddimensions.integration.module.create.block.entity.SchematicannonPathWayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 专用于蓝图大炮的网络通道方块
 */
public class SchematicannonPathWayBlock extends NetedBlock implements EntityBlock
{
    public SchematicannonPathWayBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType)
    {
        if (level.isClientSide())
            return null;

        return (level1, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof SchematicannonPathWayBlockEntity machine)
            {
                SchematicannonPathWayBlockEntity.tick(level1, blockPos, blockState, machine);
            }
        };
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving)
    {
        super.onRemove(state, level, pos, newState, isMoving);
        if (!level.isClientSide && !state.is(newState.getBlock()))
        {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SchematicannonPathWayBlockEntity pathway)
            {
                // 清一手能力，防止残留
                pathway.clearCap();
                pathway.invalidateCaps();
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState)
    {
        return new SchematicannonPathWayBlockEntity(blockPos, blockState);
    }
}