package com.wintercogs.beyonddimensions.common.machine;

import net.minecraft.core.Direction;

/**
 * 机器方块相对正面的六个面。正面是水平方向，左右是站在正面前看过去的左右，顶面与底面总是朝上与朝下
 */
public enum RelativeSide
{
    FRONT,
    BACK,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM;

    /**
     * 正面朝 front 时，朝 side 的那一面
     */
    public static RelativeSide of(Direction front, Direction side)
    {
        if (side == Direction.UP)
            return TOP;
        if (side == Direction.DOWN)
            return BOTTOM;
        if (side == front)
            return FRONT;
        if (side == front.getOpposite())
            return BACK;
        return side == front.getClockWise() ? LEFT : RIGHT;
    }

    /**
     * 正面朝 front 时这一面的朝向
     */
    public Direction direction(Direction front)
    {
        return switch (this)
        {
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case LEFT -> front.getClockWise();
            case RIGHT -> front.getCounterClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }
}
