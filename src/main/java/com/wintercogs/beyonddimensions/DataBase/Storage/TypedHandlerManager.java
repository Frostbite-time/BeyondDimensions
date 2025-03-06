package com.wintercogs.beyonddimensions.DataBase.Storage;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TypedHandlerManager
{
    public static final Map<BlockCapability<?, Direction>, Function<?, ?>> CapHandlerMap = new HashMap<>();

    public static <T, A> void register(
            BlockCapability<T, Direction> capability,
            Class<A> argumentType,
            Function<A, T> handler
    ) {
        CapHandlerMap.put(capability, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T, A> Function<A, T> getHandler(
            BlockCapability<T, Direction> capability,
            Class<A> argumentType
    ) {
        Function<?, ?> handler = CapHandlerMap.get(capability);
        if (handler != null) {
            // 验证参数类型兼容性
//            if (!handler.getClass().getTypeParameters()[0].equals(argumentType)) {
//                throw new ClassCastException("参数类型不匹配: 需要 " + argumentType.getName());
//            }
            return (Function<A, T>) handler;
        }
        return null;
    }
}
