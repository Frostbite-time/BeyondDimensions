package com.wintercogs.beyonddimensions.Api.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record CapCtx(Level level, BlockPos pos, Direction direction, BlockEntity be)
{
}
