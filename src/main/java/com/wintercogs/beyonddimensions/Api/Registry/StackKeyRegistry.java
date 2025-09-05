package com.wintercogs.beyonddimensions.Api.Registry;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackKeyRegistry
{
    private static final Map<ResourceLocation, IStackKey<?>> TYPES = new HashMap<>();

    public static <T> void registerType(IStackKey<T> type) {
        if (TYPES.containsKey(type.getTypeId())) {
            throw new IllegalStateException("Duplicate stack type registration: " + type.getTypeId());
        }
        TYPES.put(type.getTypeId(), type);
    }

    @SuppressWarnings("unchecked")
    public static <T> IStackKey<T> getType(ResourceLocation id) {
        IStackKey<?> type = TYPES.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown stack type: " + id);
        }
        return (IStackKey<T>) type;
    }

    public static List<IStackKey<?>> getAllTypes()
    {
        return List.copyOf(TYPES.values());
    }
}
