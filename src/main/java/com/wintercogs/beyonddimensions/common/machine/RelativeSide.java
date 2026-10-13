package com.wintercogs.beyonddimensions.common.machine;

import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Vec3i;

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
    public static RelativeSide of(FrontAndTop orientation, Direction side)
    {
        Direction front = orientation.front();
        Direction top = orientation.top();
        if (side == front)
            return FRONT;
        if (side == front.getOpposite())
            return BACK;
        if (side == top)
            return TOP;
        if (side == top.getOpposite())
            return BOTTOM;
        return side == left(front, top) ? LEFT : RIGHT;
    }

    /**
     * 方块朝向为 orientation 时这一面的朝向
     */
    public Direction direction(FrontAndTop orientation)
    {
        Direction front = orientation.front();
        Direction top = orientation.top();
        return switch (this)
        {
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case LEFT -> left(front, top);
            case RIGHT -> left(front, top).getOpposite();
            case TOP -> top;
            case BOTTOM -> top.getOpposite();
        };
    }

    private static Direction left(Direction front, Direction top)
    {
        Vec3i left = front.getNormal().cross(top.getNormal());
        return Direction.fromDelta(left.getX(), left.getY(), left.getZ());
    }
}
