package com.wintercogs.beyonddimensions.Util;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public record SidedCapId(Capability<?> cap, @Nullable Direction sided)
{
}
