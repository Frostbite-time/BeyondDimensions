package com.wintercogs.beyonddimensions.api.capability.helper;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.util.CapCtx;
import com.wintercogs.beyonddimensions.api.util.CommonHandler;
import com.wintercogs.beyonddimensions.api.util.USHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

// 记载所有可用能力的类，用于动态地为维度方块注册能力
public class CapabilityHelper
{
    // 自行保证类型安全
    public static final Map<ResourceLocation, Capability<? extends Object>> BlockCapabilityMap = new HashMap<>();
    public static final Map<ResourceLocation, Capability<? extends Object>> ItemCapabilityMap = new HashMap<>();

    public static final Map<ResourceLocation, USHandler> USHandlerMap = new HashMap<>();

    public static final Map<ResourceLocation, CommonHandler> CommonHandlerMap = new HashMap<>();

    public static <T> void registerUSHandler(IStackKey<?> type, Function<UnifiedStorage, T> handler)
    {
        if (USHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的统一存储分化表被重复注册：" + type.getTypeId());
        USHandlerMap.put(type.getTypeId(), USHandler.contextless(handler));
    }

    public static <T> void registerUSHandler(IStackKey<?> type, BiFunction<UnifiedStorage, CapCtx, T> handler)
    {
        if (USHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的统一存储分化表被重复注册：" + type.getTypeId());
        USHandlerMap.put(type.getTypeId(), USHandler.contextual(handler));
    }

    public static <T> void registerStackTypedHandler(IStackKey<?> type, Function<StackHandler, T> handler)
    {
        if (CommonHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的通用存储分化表已被注册：" + type.getTypeId());
        CommonHandlerMap.put(type.getTypeId(), CommonHandler.contextless(handler));
    }

    public static <T> void registerStackTypedHandler(IStackKey<?> type, BiFunction<StackHandler, CapCtx, T> handler)
    {
        if (CommonHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的通用存储分化表已被注册：" + type.getTypeId());
        CommonHandlerMap.put(type.getTypeId(), CommonHandler.contextual(handler));
    }

}
