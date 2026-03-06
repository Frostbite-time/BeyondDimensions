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

            BeyondDimensions.LOGGER.warn("Integration module does not implement {}: {}", expectedType.getSimpleName(), className);
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("Failed to load integration module: {}", className, t);
        }

        return null;
    }
}
