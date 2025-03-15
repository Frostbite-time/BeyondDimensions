package com.wintercogs.beyonddimensions.Common.InterfaceHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface BlockCapabilityFunction<T>
{
    T get(Level level, BlockPos targetPos, Direction direction);
}
