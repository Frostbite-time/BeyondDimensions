package com.wintercogs.beyonddimensions.integration.module.create.block;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import com.wintercogs.beyonddimensions.common.block.orientation.FacingRule;
import com.wintercogs.beyonddimensions.integration.module.create.block.entity.SchematicannonPathWayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 专用于蓝图大炮的网络通道方块。正面是蓝色的图纸面，放置时朝向玩家
 */
public class SchematicannonPathWayBlock extends NetedBlock implements EntityBlock
{
    public SchematicannonPathWayBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(FacingRule.HORIZONTAL.defaultState(stateDefinition.any()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        FacingRule.HORIZONTAL.addProperties(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
    {
        return FacingRule.HORIZONTAL.placed(defaultBlockState(), context);
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation)
    {
        return FacingRule.HORIZONTAL.rotate(state, rotation);
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror)
    {
        return FacingRule.HORIZONTAL.mirror(state, mirror);
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
