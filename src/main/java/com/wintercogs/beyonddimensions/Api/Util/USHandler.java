package com.wintercogs.beyonddimensions.Api.Util;

import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface USHandler
{
    Object apply(UnifiedStorage us, @Nullable CapCtx ctx);  // 唯一抽象方法

    // —— 对外暴露：是否带上下文 —— //
    default boolean isContextual() {              // 默认：带上下文
        return true;
    }

    // —— 工厂方法 —— //
    static USHandler contextual(BiFunction<UnifiedStorage, CapCtx, ?> f) {
        // 使用默认 isContextual()=true 即可
        return (us, ctx) -> f.apply(us, ctx);
    }

    static USHandler contextless(Function<UnifiedStorage, ?> f) {
        return new USHandler() {
            @Override public Object apply(UnifiedStorage us, CapCtx ctx) {
                return f.apply(us);               // 忽略 ctx
            }
            @Override public boolean isContextual() {
                return false;                     // 明确声明“无上下文”
            }
        };
    }
}
