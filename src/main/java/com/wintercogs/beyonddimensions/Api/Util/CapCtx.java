package com.wintercogs.beyonddimensions.Api.Util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public record CapCtx(Level level, BlockPos pos, @Nullable Direction direction, BlockEntity be)
{
}
