package com.wintercogs.beyonddimensions.Registry;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class StackTypeRegistry {

    private static final Map<ResourceLocation, IStackType<?>> TYPES = new HashMap<>();

    public static <T> void registerType(IStackType<T> type) {
        if (TYPES.containsKey(type.getTypeId())) {
            throw new IllegalStateException("Duplicate stack type registration: " + type.getTypeId());
        }
        TYPES.put(type.getTypeId(), type);
    }

    @SuppressWarnings("unchecked")
    public static <T> IStackType<T> getType(ResourceLocation id) {
        IStackType<?> type = TYPES.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown stack type: " + id);
        }
        return (IStackType<T>) type;
    }
}

