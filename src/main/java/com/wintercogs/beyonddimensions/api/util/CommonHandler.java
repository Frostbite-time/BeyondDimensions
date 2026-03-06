package com.wintercogs.beyonddimensions.api.util;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface CommonHandler
{
    Object apply(StackHandler us, @Nullable CapCtx ctx);

    // 是否是带上下文的版本
    default boolean isContextual()
    {
        return true;
    }

    // 构造带上下文版本
    static CommonHandler contextual(BiFunction<StackHandler, CapCtx, ?> f)
    {
        return (us, ctx) -> f.apply(us, ctx);
    }

    // 不带上下文版本
    static CommonHandler contextless(Function<StackHandler, ?> f)
    {
        return new CommonHandler()
        {
            @Override
            public Object apply(StackHandler us, CapCtx ctx)
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
