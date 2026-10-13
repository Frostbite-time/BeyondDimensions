package com.wintercogs.beyonddimensions.common.block;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

/**
 * 方块的朝向：正面与顶面各朝哪个方向，两者互相垂直
 */
public record Orientation(Direction front, Direction top)
{
    public static final Orientation NORTH_UP = new Orientation(Direction.NORTH, Direction.UP);

    public Orientation
    {
        if (front.getAxis() == top.getAxis())
            throw new IllegalArgumentException("Front " + front + " and top " + top + " are not perpendicular");
    }

    /**
     * 站在正面前、头朝顶面时左手的方向
     */
    public Direction left()
    {
        Vec3i left = front.getNormal().cross(top.getNormal());
        return Direction.fromDelta(left.getX(), left.getY(), left.getZ());
    }

    /**
     * 从正面看绕正面顺时针转 90° 后的朝向：顶面转到原来右手的方向
     */
    public Orientation turned()
    {
        return new Orientation(front, left().getOpposite());
    }

    public Orientation rotated(Rotation rotation)
    {
        return new Orientation(rotation.rotate(front), rotation.rotate(top));
    }

    public Orientation mirrored(Mirror mirror)
    {
        return new Orientation(mirror.mirror(front), mirror.mirror(top));
    }
}
