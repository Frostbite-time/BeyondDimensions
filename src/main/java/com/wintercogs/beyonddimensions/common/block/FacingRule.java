package com.wintercogs.beyonddimensions.common.block;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 方块的正面能朝哪些方向，以及朝向存在哪些方块状态属性里。
 * HORIZONTAL：正面只朝水平四个方向，存在 facing（水平）里，顶面总是朝上。
 * ALL：正面六个方向都可以，存在 facing 里；正面朝上或朝下时 top 记下顶面朝哪个水平方向
 */
public enum FacingRule
{
    HORIZONTAL,
    ALL;

    /**
     * 正面朝上或朝下时顶面的朝向；正面水平时不起作用
     */
    public static final DirectionProperty TOP = DirectionProperty.create("top", Direction.Plane.HORIZONTAL);

    public void addProperties(StateDefinition.Builder<Block, BlockState> builder)
    {
        switch (this)
        {
            case HORIZONTAL -> builder.add(BlockStateProperties.HORIZONTAL_FACING);
            case ALL -> builder.add(BlockStateProperties.FACING, TOP);
        }
    }

    /**
     * 正面朝北；正面朝上或朝下时顶面缺省朝南
     */
    public BlockState defaultState(BlockState state)
    {
        return switch (this)
        {
            case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            case ALL -> state.setValue(BlockStateProperties.FACING, Direction.NORTH).setValue(TOP, Direction.SOUTH);
        };
    }

    /**
     * 放置时正面朝向玩家；六个方向都可以时，俯视或仰视放下的方块正面朝上或朝下，顶面按 {@link #topFor} 定
     */
    public BlockState placed(BlockState state, BlockPlaceContext context)
    {
        return switch (this)
        {
            case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
            case ALL ->
            {
                Direction front = context.getNearestLookingDirection().getOpposite();
                yield state.setValue(BlockStateProperties.FACING, front).setValue(TOP, topFor(front, context));
            }
        };
    }

    /**
     * 朝 face 的一面在玩家看来是正的时候，它的顶边朝哪个方向：朝上的面顶边背离玩家，朝下的面顶边朝向玩家；
     * 水平的面顶边朝上
     */
    public static Direction topFor(Direction face, BlockPlaceContext context)
    {
        return switch (face)
        {
            case UP -> context.getHorizontalDirection();
            case DOWN -> context.getHorizontalDirection().getOpposite();
            default -> Direction.UP;
        };
    }

    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return switch (this)
        {
            case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
            case ALL -> state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)))
                    .setValue(TOP, rotation.rotate(state.getValue(TOP)));
        };
    }

    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return switch (this)
        {
            case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, mirror.mirror(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
            case ALL -> state.setValue(BlockStateProperties.FACING, mirror.mirror(state.getValue(BlockStateProperties.FACING)))
                    .setValue(TOP, mirror.mirror(state.getValue(TOP)));
        };
    }

    /**
     * 方块此刻的正面与顶面；没有朝向的方块按正面朝北、顶面朝上算
     */
    public static FrontAndTop orientation(BlockState state)
    {
        if (state.hasProperty(BlockStateProperties.FACING))
        {
            Direction front = state.getValue(BlockStateProperties.FACING);
            Direction top = front.getAxis().isVertical() ? state.getOptionalValue(TOP).orElse(Direction.SOUTH) : Direction.UP;
            return FrontAndTop.fromFrontAndTop(front, top);
        }
        Direction front = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH);
        return FrontAndTop.fromFrontAndTop(front, Direction.UP);
    }
}
