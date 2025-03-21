package com.wintercogs.beyonddimensions.Unit;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.HashMap;
import java.util.Map;

// 记载所有可用能力的类，用于动态地为维度方块注册能力
public class CapabilityHelper
{
    // 自行保证类型安全
    public static final Map<ResourceLocation, BlockCapability<? extends Object, Direction>> BlockCapabilityMap = new HashMap<>();

}
