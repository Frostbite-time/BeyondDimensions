package com.wintercogs.beyonddimensions.api.dimensionnet.helper;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一些辅助工具，让UnifiedStorage可以在插入操作真实执行前进行一些调整
 */
public final class UnifiedStorageBeforeInsertHandler
{
    private static final List<BeforeInsertHandler> handlers = new ArrayList<>();

    @FunctionalInterface
    public interface BeforeInsertHandler
    {
        /**
         * @param originalInsert 本次插入最原始的堆叠
         * @param tryInsert      当前调用链上传递的堆叠
         * @param net            网络信息，可为空
         */
        @NotNull
        BeforeInsertHandlerReturnInfo beforeInsert(
                @NotNull KeyAmount originalInsert,
                @NotNull KeyAmount tryInsert,
                @Nullable DimensionsNet net
        );
    }

    public record BeforeInsertHandlerReturnInfo(@NotNull KeyAmount beforeInsert, boolean cancel)
    {
    }

    /**
     * 调用此函数以添加处理
     */
    public static void addHandler(BeforeInsertHandler handler)
    {
        handlers.add(handler);
    }

    /**
     * @param tryInsert 本次尝试插入的原始堆叠
     * @param net       携带的网络信息，可为空
     * @return 维度网络最终实际处理的堆叠
     * <p>
     * 会对handlers表进行链式调用，每一次处理完的insert会被传递给下一次调用，
     * 最终返回时，如果cancel，则网络不接受此次任何输入，将原始堆叠返回给玩家或机器，
     * 如果不为cancel，则尝试将最后一次调用得到的输入给维度网络
     */
    @NotNull
    public static BeforeInsertHandlerReturnInfo onBeforeInsert(@Nullable KeyAmount tryInsert, @Nullable DimensionsNet net)
    {
        final KeyAmount original = (tryInsert == null)
                ? new KeyAmount(EmptyStackKey.INSTANCE, 0)
                : tryInsert;

        if (tryInsert == null)
            return new BeforeInsertHandlerReturnInfo(original, true);

        if (tryInsert.isEmpty())
            return new BeforeInsertHandlerReturnInfo(tryInsert, true);

        KeyAmount current = tryInsert;

        for (var handler : handlers)
        {
            if (handler == null)
                continue;

            var ret = handler.beforeInsert(original, current, net);
            current = ret.beforeInsert();

            if (ret.cancel())
                return new BeforeInsertHandlerReturnInfo(current, true);

            if (current.isEmpty())
                return new BeforeInsertHandlerReturnInfo(current, false);
        }

        return new BeforeInsertHandlerReturnInfo(current, false);
    }

}