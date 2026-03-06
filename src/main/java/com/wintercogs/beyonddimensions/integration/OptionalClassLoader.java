package com.wintercogs.beyonddimensions.integration;

import com.wintercogs.beyonddimensions.BeyondDimensions;

public final class OptionalClassLoader
{
    private OptionalClassLoader()
    {
    }

    public static <T> T instantiate(String className, Class<T> expectedType)
    {
        try
        {
            Class<?> raw = Class.forName(className, true, OptionalClassLoader.class.getClassLoader());
            if (!expectedType.isAssignableFrom(raw))
            {
                BeyondDimensions.LOGGER.warn("Ignore optional class {}, type mismatch for {}", className, expectedType.getName());
                return null;
            }
            return expectedType.cast(raw.getDeclaredConstructor().newInstance());
        }
        catch (Throwable throwable)
        {
            BeyondDimensions.LOGGER.warn("Failed to load optional integration class {}", className, throwable);
            return null;
        }
    }
}
