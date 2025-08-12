package com.wintercogs.beyonddimensions.Api.Util;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface CommonHandler
{
    Object apply(StackTypedHandler us, @Nullable CapCtx ctx);

    // 是否是带上下文的版本
    default boolean isContextual() {
        return true;
    }

    // 构造带上下文版本
    static CommonHandler contextual(BiFunction<StackTypedHandler, CapCtx, ?> f) {
        return (us, ctx) -> f.apply(us, ctx);
    }

    // 不带上下文版本
    static CommonHandler contextless(Function<StackTypedHandler, ?> f) {
        return new CommonHandler() {
            @Override public Object apply(StackTypedHandler us, CapCtx ctx) {
                return f.apply(us);
            }
            @Override public boolean isContextual() {
                return false;
            }
        };
    }
}
