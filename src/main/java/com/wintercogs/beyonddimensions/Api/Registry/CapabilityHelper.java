package com.wintercogs.beyonddimensions.Api.Registry;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.Api.Util.CommonHandler;
import com.wintercogs.beyonddimensions.Api.Util.USHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

// 记载所有可用能力的类，用于动态地为维度方块注册能力
public class CapabilityHelper
{
    // 自行保证类型安全
    public static final Map<ResourceLocation, BlockCapability<? extends Object, Direction>> BlockCapabilityMap = new HashMap<>();

    public static final Map<ResourceLocation, ItemCapability<? extends Object, Void>> ItemCapabilityMap = new HashMap<>();

    public static final Map<ResourceLocation, USHandler> USHandlerMap = new HashMap<>();

    public static final Map<ResourceLocation, CommonHandler> CommonHandlerMap = new HashMap<>();

    public static <T> void registerUSHandler(IStackType<?> type, Function<UnifiedStorage, T> handler)
    {
        if(USHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的统一存储分化表被重复注册：" + type.getTypeId());
        USHandlerMap.put(type.getTypeId(), USHandler.contextless(handler));
    }

    public static <T> void registerUSHandler(IStackType<?> type, BiFunction<UnifiedStorage, CapCtx, T> handler)
    {
        if(USHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的统一存储分化表被重复注册：" + type.getTypeId());
        USHandlerMap.put(type.getTypeId(), USHandler.contextual(handler));
    }

    public static <T> void registerStackTypedHandler(IStackType<?> type, Function<StackTypedHandler, T> handler)
    {
        if(CommonHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的通用存储分化表已被注册：" + type.getTypeId());
        CommonHandlerMap.put(type.getTypeId(), CommonHandler.contextless(handler));
    }

    public static <T> void registerStackTypedHandler(IStackType<?> type, BiFunction<StackTypedHandler, CapCtx, T> handler)
    {
        if(CommonHandlerMap.containsKey(type.getTypeId()))
            throw new RuntimeException("此类型的通用存储分化表已被注册：" + type.getTypeId());
        CommonHandlerMap.put(type.getTypeId(), CommonHandler.contextual(handler));
    }

}
