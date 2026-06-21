package com.wintercogs.beyonddimensions.integration.module.create.block;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import com.wintercogs.beyonddimensions.integration.module.create.block.entity.SchematicannonPathWayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SchematicannonPathWayBlockEntity pathway)
        {
            pathway.updateCap();
        }
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
                pathway.invalidateCapabilities();
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState)
    {
        return new SchematicannonPathWayBlockEntity(blockPos, blockState);
    }
}
