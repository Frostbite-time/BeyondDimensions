package com.wintercogs.beyonddimensions.integration;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import org.jetbrains.annotations.Nullable;

public final class OptionalClassLoader
{
    private OptionalClassLoader()
    {
    }

    @Nullable
    public static <T> T instantiate(String className, Class<T> expectedType)
    {
        try
        {
            Class<?> clazz = Class.forName(className, false, OptionalClassLoader.class.getClassLoader());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (expectedType.isInstance(instance))
            {
                return expectedType.cast(instance);
            }

            BeyondDimensions.LOGGER.warn("集成模块没有实现 {}：{}", expectedType.getSimpleName(), className);
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("集成模块加载失败：{}", className, t);
        }

        return null;
    }
}
