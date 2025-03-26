package com.wintercogs.beyonddimensions.Unit;


import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import java.util.HashMap;
import java.util.Map;

// 记载所有可用能力的类，用于动态地为维度方块注册能力
public class CapabilityHelper
{
    // 自行保证类型安全
    public static final Map<ResourceLocation, Capability<? extends Object>> BlockCapabilityMap = new HashMap<>();

}
