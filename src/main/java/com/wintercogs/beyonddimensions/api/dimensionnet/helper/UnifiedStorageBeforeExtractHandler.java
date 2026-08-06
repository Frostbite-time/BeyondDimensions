package com.wintercogs.beyonddimensions.api.dimensionnet.helper;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一些辅助工具，让UnifiedStorage可以在提取操作真实执行前进行一些调整
 */
public final class UnifiedStorageBeforeExtractHandler
{
    private static final List<BeforeExtractHandler> handlers = new ArrayList<>();

    private UnifiedStorageBeforeExtractHandler()
    {
    }

    @FunctionalInterface
    public interface BeforeExtractHandler
    {
        /**
         * @param originalExtract 本次提取最原始的堆叠
         * @param tryExtract      当前调用链上传递的堆叠
         * @param net             网络信息，可为空
         */
        @NotNull
        BeforeExtractHandlerReturnInfo beforeExtract(
                @NotNull KeyAmount originalExtract,
                @NotNull KeyAmount tryExtract,
                @Nullable DimensionsNet net
        );
    }

    public record BeforeExtractHandlerReturnInfo(@NotNull KeyAmount beforeExtract, boolean cancel)
    {
    }

    /**
     * 调用此函数以添加处理
     */
    public static void addHandler(BeforeExtractHandler handler)
    {
        handlers.add(handler);
    }

    /**
     * @param tryExtract 本次尝试提取的原始堆叠
     * @param net        携带的网络信息，可为空
     * @return 维度网络最终实际处理的堆叠
     * <p>
     * 会对handlers表进行链式调用，每一次处理完的extract会被传递给下一次调用。
     * 最终返回时，如果cancel，则网络拒绝此次提取；如果不为cancel，
     * 则尝试从维度网络提取最后一次调用得到的内容。
     */
    @NotNull
    public static BeforeExtractHandlerReturnInfo onBeforeExtract(@Nullable KeyAmount tryExtract, @Nullable DimensionsNet net)
    {
        final KeyAmount original = (tryExtract == null)
                ? new KeyAmount(EmptyStackKey.INSTANCE, 0)
                : tryExtract;

        if (tryExtract == null)
            return new BeforeExtractHandlerReturnInfo(original, true);

        if (tryExtract.isEmpty())
            return new BeforeExtractHandlerReturnInfo(tryExtract, true);

        KeyAmount current = tryExtract;

        for (var handler : handlers)
        {
            if (handler == null)
                continue;

            var ret = handler.beforeExtract(original, current, net);
            current = ret.beforeExtract();

            if (ret.cancel())
                return new BeforeExtractHandlerReturnInfo(current, true);

            if (current.isEmpty())
                return new BeforeExtractHandlerReturnInfo(current, false);
        }

        return new BeforeExtractHandlerReturnInfo(current, false);
    }
}
