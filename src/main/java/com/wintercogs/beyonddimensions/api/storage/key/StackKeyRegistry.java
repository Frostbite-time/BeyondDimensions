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
            throw new IllegalStateException("尝试注册重复类型的Key: " + type.getTypeId());
        }
        TYPES.put(type.getTypeId(), type);
    }

    @SuppressWarnings("unchecked")
    public static <T> @NotNull IStackKey<T> getType(ResourceLocation id)
    {
        IStackKey<?> type = TYPES.get(id);
        if (type == null)
        {
            throw new IllegalArgumentException("注册表中不存在此类型的Key，请先注册再使用: " + id);
        }
        return (IStackKey<T>) type;
    }

    public static List<IStackKey<?>> getAllTypes()
    {
        return List.copyOf(TYPES.values());
    }
}