package com.wintercogs.beyonddimensions.common.machine;

import com.wintercogs.beyonddimensions.common.block.orientation.Orientation;
import net.minecraft.core.Direction;

/**
 * 机器方块相对正面与顶面的六个面；左右是站在正面前、头朝顶面看过去的左右
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
     * 方块朝向为 orientation 时，朝 side 的那一面
     */
    public static RelativeSide of(Orientation orientation, Direction side)
    {
        if (side == orientation.front())
            return FRONT;
        if (side == orientation.front().getOpposite())
            return BACK;
        if (side == orientation.top())
            return TOP;
        if (side == orientation.top().getOpposite())
            return BOTTOM;
        return side == orientation.left() ? LEFT : RIGHT;
    }

    /**
     * 方块朝向为 orientation 时这一面的朝向
     */
    public Direction direction(Orientation orientation)
    {
        return switch (this)
        {
            case FRONT -> orientation.front();
            case BACK -> orientation.front().getOpposite();
            case LEFT -> orientation.left();
            case RIGHT -> orientation.left().getOpposite();
            case TOP -> orientation.top();
            case BOTTOM -> orientation.top().getOpposite();
        };
    }
}
