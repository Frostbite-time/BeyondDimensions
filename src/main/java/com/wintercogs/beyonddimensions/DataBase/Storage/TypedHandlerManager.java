package com.wintercogs.beyonddimensions.DataBase.Storage;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

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
            return (Function<A, T>) handler;
        }
        return null;
    }
}
