package com.wintercogs.beyonddimensions.Api.Registry;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
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

/***
 * 记载所有可用能力的类，用于动态地为维度方块注册能力
 * <p>
 * 注册BlockCapabilityMap和ItemCapabilityMap的意思是向超越维度声明：超越维度可以操作此能力，且超越维度的网络接口和维度网络通道可以被其他模组通过此能力操作
 * <p>
 * 如果注册了BlockCapabilityMap则必须注册USHandlerMap和CommonHandlerMap，这两个Map让UnifiedStorage和StackTypedHandler可以在需要时转为对应接口的包装实现，用于为方块动态注册不同的能力
 * <p>
 * USHandlerMap是用于UnifiedStorage的，UnifiedStorage是一个无序的容器，所有包装需要以无序的前提来写
 * <p>
 * CommonHandlerMap是用于StackTypedHandler的，StackTypedHandler是一个有序容器，类似箱子，包装方法按照有序前提来写
 */
public class CapabilityHelper
{
    // 自行保证类型安全

    /**
     * 存储类型 -> 方块能力
     */
    public static final Map<ResourceLocation, BlockCapability<?, Direction>> BlockCapabilityMap = new HashMap<>();

    /**
     * 存储类型 -> 物品能力
     */
    public static final Map<ResourceLocation, ItemCapability<?, Void>> ItemCapabilityMap = new HashMap<>();

    /**
     * 存储类型 -> 分化包装
     */
    public static final Map<ResourceLocation, USHandler> USHandlerMap = new HashMap<>();

    /**
     * 存储类型 -> 分化包装
     */
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
