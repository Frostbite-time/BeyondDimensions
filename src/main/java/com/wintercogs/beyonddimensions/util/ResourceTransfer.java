package com.wintercogs.beyonddimensions.util;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;

import java.util.function.ToLongFunction;

/**
 * 从一处取出资源放进另一处，不需要先取后放回：先模拟来源能给出什么、对方能收下多少，再只取出收得下的数量
 */
public final class ResourceTransfer
{
    /**
     * 按资源与数量取出或存入，simulate 为真时只计算
     */
    @FunctionalInterface
    public interface Operation
    {
        KeyAmount apply(IStackKey<?> key, long amount, boolean simulate);
    }

    private ResourceTransfer()
    {
    }

    /**
     * 来源第一次模拟定下交出的具体资源（例如模糊匹配选中的变体），之后按这种资源精确取出
     *
     * @param extract 从来源取出
     * @param restore 存回来源，只在对方实际收下的比模拟少时使用
     * @param accepts 对方能收下多少，只模拟，不改动对方
     * @param place   把取出的资源放进对方，返回没放下的数量
     * @return 放进对方的资源
     */
    public static KeyAmount move(IStackKey<?> key, long amount, Operation extract, Operation restore,
                                 ToLongFunction<KeyAmount> accepts, ToLongFunction<KeyAmount> place)
    {
        KeyAmount offer = extract.apply(key, amount, true);
        if (offer.isEmpty())
            return offer;
        long fits = Math.min(offer.amount(), accepts.applyAsLong(offer));
        if (fits <= 0L)
            return new KeyAmount(offer.key(), 0L);
        KeyAmount taken = extract.apply(offer.key(), fits, false);
        if (taken.isEmpty())
            return taken;
        long left = place.applyAsLong(taken);
        // 对方的模拟与实际不一致时才会放不下，这部分存回来源
        if (left > 0L)
            restore.apply(taken.key(), left, false);
        return new KeyAmount(taken.key(), taken.amount() - left);
    }
}
