package com.wintercogs.beyonddimensions.api.storage.key;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackKeyRegistry
{
    private static final Map<ResourceLocation, IStackKey<?>> TYPES = new HashMap<>();

    public static <T> void registerType(IStackKey<T> type)
    {
        if (TYPES.containsKey(type.getTypeId()))
        {
            throw new IllegalStateException("Stack key type is already registered: " + type.getTypeId());
        }
        TYPES.put(type.getTypeId(), type);
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull IStackKey<T> getType(ResourceLocation id)
    {
        IStackKey<?> type = TYPES.get(id);
        if (type == null)
        {
            throw new IllegalArgumentException("Stack key type is not registered; register it before use: " + id);
        }
        return (IStackKey<T>) type;
    }

    public static List<IStackKey<?>> getAllTypes()
    {
        return List.copyOf(TYPES.values());
    }
}
