package com.wintercogs.beyonddimensions.common.block.orientation;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 方块的正面能朝哪些方向，以及朝向存在哪些方块状态属性里。放置时正面朝向玩家；正面能朝上朝下的方块
 * 被俯视或仰视放下时正面朝上或朝下，顶面按 {@link #topFor} 定。
 * <ul>
 * <li>HORIZONTAL：正面只朝水平四个方向，顶面朝上，共 4 种；存在 facing（水平）里。</li>
 * <li>UPRIGHT：正面六个方向，方块不会侧倒：正面水平时顶面朝上，正面朝上或朝下时顶面朝四个水平方向之一，共 12 种；
 * 存在 facing 与 top 里。</li>
 * <li>FREE：正面六个方向，每个方向再绕正面转四种，共 24 种；存在 facing 与 spin 里。</li>
 * </ul>
 * 能绕正面转的朝向用扳手转，见 {@link #turned}
 */
public enum FacingRule
{
    HORIZONTAL,
    UPRIGHT,
    FREE;

    /**
     * UPRIGHT 的方块正面朝上或朝下时顶面的朝向；正面水平时不起作用
     */
    public static final DirectionProperty TOP = DirectionProperty.create("top", Direction.Plane.HORIZONTAL);

    /**
     * FREE 的方块从正面看绕正面顺时针转了几个 90°：正面水平时从顶面朝上算起，正面朝上或朝下时从顶面朝北算起
     */
    public static final IntegerProperty SPIN = IntegerProperty.create("spin", 0, 3);

    public void addProperties(StateDefinition.Builder<Block, BlockState> builder)
    {
        switch (this)
        {
            case HORIZONTAL -> builder.add(BlockStateProperties.HORIZONTAL_FACING);
            case UPRIGHT -> builder.add(BlockStateProperties.FACING, TOP);
            case FREE -> builder.add(BlockStateProperties.FACING, SPIN);
        }
    }

    /**
     * 正面朝北；UPRIGHT 的方块正面朝上或朝下时顶面缺省朝南
     */
    public BlockState defaultState(BlockState state)
    {
        return switch (this)
        {
            case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            case UPRIGHT -> state.setValue(BlockStateProperties.FACING, Direction.NORTH).setValue(TOP, Direction.SOUTH);
            case FREE -> state.setValue(BlockStateProperties.FACING, Direction.NORTH).setValue(SPIN, 0);
        };
    }

    public BlockState placed(BlockState state, BlockPlaceContext context)
    {
        Direction front = this == HORIZONTAL
                ? context.getHorizontalDirection().getOpposite()
                : context.getNearestLookingDirection().getOpposite();
        return with(state, new Orientation(front, topFor(front, context)));
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

    /**
     * 从正面看绕正面顺时针转 90° 后的状态；这种朝向转不了时为 null：HORIZONTAL 都转不了，UPRIGHT 只有正面朝上或朝下时能转
     */
    public @Nullable BlockState turned(BlockState state)
    {
        Orientation orientation = orientation(state);
        boolean turnable = switch (this)
        {
            case HORIZONTAL -> false;
            case UPRIGHT -> orientation.front().getAxis().isVertical();
            case FREE -> true;
        };
        return turnable ? with(state, orientation.turned()) : null;
    }

    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return with(state, orientation(state).rotated(rotation));
    }

    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return with(state, orientation(state).mirrored(mirror));
    }

    /**
     * 把朝向写进方块状态；HORIZONTAL 只取正面，UPRIGHT 只在正面朝上或朝下时取顶面
     */
    private BlockState with(BlockState state, Orientation orientation)
    {
        Direction front = orientation.front();
        return switch (this)
        {
            case HORIZONTAL -> state.setValue(BlockStateProperties.HORIZONTAL_FACING, front);
            case UPRIGHT ->
            {
                BlockState facing = state.setValue(BlockStateProperties.FACING, front);
                yield front.getAxis().isVertical() ? facing.setValue(TOP, orientation.top()) : facing;
            }
            case FREE -> state.setValue(BlockStateProperties.FACING, front).setValue(SPIN, spinOf(orientation));
        };
    }

    /**
     * 方块此刻的朝向；没有朝向的方块按正面朝北、顶面朝上算
     */
    public static Orientation orientation(BlockState state)
    {
        if (state.hasProperty(BlockStateProperties.FACING))
        {
            Direction front = state.getValue(BlockStateProperties.FACING);
            if (state.hasProperty(SPIN))
            {
                Orientation orientation = new Orientation(front, spinStart(front));
                for (int turn = 0; turn < state.getValue(SPIN); turn++)
                    orientation = orientation.turned();
                return orientation;
            }
            Direction top = front.getAxis().isVertical() ? state.getOptionalValue(TOP).orElse(Direction.SOUTH) : Direction.UP;
            return new Orientation(front, top);
        }
        Direction front = state.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING).orElse(Direction.NORTH);
        return new Orientation(front, Direction.UP);
    }

    private static Direction spinStart(Direction front)
    {
        return front.getAxis().isVertical() ? Direction.NORTH : Direction.UP;
    }

    private static int spinOf(Orientation orientation)
    {
        Orientation turned = new Orientation(orientation.front(), spinStart(orientation.front()));
        for (int spin = 0; spin < 4; spin++, turned = turned.turned())
            if (turned.top() == orientation.top())
                return spin;
        throw new IllegalStateException("No spin turns " + orientation.front() + " to top " + orientation.top());
    }
}
