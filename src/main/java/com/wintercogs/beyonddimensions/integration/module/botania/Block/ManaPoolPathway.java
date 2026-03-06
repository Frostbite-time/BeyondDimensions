package com.wintercogs.beyonddimensions.integration.module.botania.Block;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManaPoolPathway extends NetedBlock implements EntityBlock
{
    private static final VoxelShape SHAPE;
    private static final VoxelShape SHAPE_INTERACT;

    static
    {
        SHAPE_INTERACT = box(0, 0, 0, 16, 8, 16);
        VoxelShape cutout = box(2, 2, 2, 14, 16, 14);
        SHAPE = Shapes.join(SHAPE_INTERACT, cutout, BooleanOp.ONLY_FIRST);
    }


    public ManaPoolPathway(Properties properties)
    {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext ctx)
    {
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos)
    {
        return SHAPE;
    }


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        return (level1, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof ManaPoolPathwayBlockEntity pool)
            {
                if (level1.isClientSide())
                    ManaPoolPathwayBlockEntity.clientTick(level1, blockPos, blockState, pool);
                else if (!level1.isClientSide())
                    ManaPoolPathwayBlockEntity.serverTick(level1, blockPos, blockState, pool);
            }
        };
    }

    // 调用collideEntityItem来合成配方
    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity)
    {
        if (entity instanceof ItemEntity item)
        {
            if (level.getBlockEntity(pos) instanceof ManaPoolPathwayBlockEntity manaBe)
                manaBe.collideEntityItem(item);
        }
    }

    // NetedBlock忘记加事件触发了，暂时不改，这里手写一下
    @Override
    protected boolean triggerEvent(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, int id, int param)
    {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.triggerEvent(id, param);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState)
    {
        return new ManaPoolPathwayBlockEntity(blockPos, blockState);
    }
}
