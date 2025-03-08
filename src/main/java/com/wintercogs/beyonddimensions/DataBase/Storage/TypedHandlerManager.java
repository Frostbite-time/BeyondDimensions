package com.wintercogs.beyonddimensions.DataBase.Storage;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TypedHandlerManager
{
    public static final Map<BlockCapability<?, Direction>, Function<?, ?>> BlockCapHandlerMap = new HashMap<>();

    // 注册参数，T为由IStacktypedHandler转换到的其他Handler接口实现
    // A为一个类，用于提供转换数据
    public static <T, A> void register(
            BlockCapability<T, Direction> capability,
            Class<A> argumentType,
            Function<A, T> handler
    ) {
        BlockCapHandlerMap.put(capability, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T, A> Function<A, T> getHandler(
            BlockCapability<T, Direction> capability,
            Class<A> argumentType
    ) {
        Function<?, ?> handler = BlockCapHandlerMap.get(capability);
        if (handler != null) {
            return (Function<A, T>) handler;
        }
        return null;
    }
}
