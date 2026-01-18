package com.wintercogs.beyonddimensions.Api.Util;

import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface USHandler
{
    Object apply(UnifiedStorage us, @Nullable CapCtx ctx);

    // 是否是带上下文的版本
    default boolean isContextual()
    {
        return true;
    }

    // 构造带上下文版本
    static USHandler contextual(BiFunction<UnifiedStorage, CapCtx, ?> f)
    {
        return (us, ctx) -> f.apply(us, ctx);
    }

    // 不带上下文版本
    static USHandler contextless(Function<UnifiedStorage, ?> f)
    {
        return new USHandler()
        {
            @Override
            public Object apply(UnifiedStorage us, CapCtx ctx)
            {
                return f.apply(us);
            }

            @Override
            public boolean isContextual()
            {
                return false;
            }
        };
    }
}
