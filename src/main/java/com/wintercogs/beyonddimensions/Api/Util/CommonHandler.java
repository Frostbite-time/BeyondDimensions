package com.wintercogs.beyonddimensions.Api.Util;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface CommonHandler
{
    Object apply(StackTypedHandler us, CapCtx ctx);  // 唯一抽象方法

    // —— 对外暴露：是否带上下文 —— //
    default boolean isContextual()
    {              // 默认：带上下文
        return true;
    }

    // —— 工厂方法 —— //
    static CommonHandler contextual(BiFunction<StackTypedHandler, CapCtx, ?> f)
    {
        // 使用默认 isContextual()=true 即可
        return (us, ctx) -> f.apply(us, ctx);
    }

    static CommonHandler contextless(Function<StackTypedHandler, ?> f)
    {
        return new CommonHandler()
        {
            @Override
            public Object apply(StackTypedHandler us, CapCtx ctx)
            {
                return f.apply(us);               // 忽略 ctx
            }

            @Override
            public boolean isContextual()
            {
                return false;                     // 明确声明“无上下文”
            }
        };
    }
}