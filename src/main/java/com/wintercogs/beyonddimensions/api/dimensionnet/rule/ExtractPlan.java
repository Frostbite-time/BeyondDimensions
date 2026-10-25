package com.wintercogs.beyonddimensions.api.dimensionnet.rule;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;

/**
 * 提取规则的计划。交出的永远是网络里现成的请求资源，规则只决定给不给、最多给多少、每个额外扣什么；
 * 额外扣除的资源会再经过提取规则。
 * <p>
 * 提取不分份：模拟给出 n 个时，同样状态下不超过 n 个的实际提取总能足量交出
 * <pre>{@code
 * ExtractPlan.reject()                          // 不给
 * ExtractPlan.allow().limit(request.stored(request.key()) - 1000) // 保留 1000 个
 * ExtractPlan.allow().take(energy, 10)          // 每取出一个扣 10 能量，不够就不给
 * }</pre>
 */
public final class ExtractPlan extends StoragePlan
{
    private ExtractPlan(boolean rejected)
    {
        super(rejected, 1L);
    }

    /**
     * 不给
     */
    public static ExtractPlan reject()
    {
        return new ExtractPlan(true);
    }

    /**
     * 照常交出
     */
    public static ExtractPlan allow()
    {
        return new ExtractPlan(false);
    }

    /**
     * 每交出一个额外扣除 amount 个 key
     */
    public ExtractPlan take(IStackKey<?> key, long amount)
    {
        addChange(key, amount, false);
        return this;
    }

    /**
     * 最多交出 max 个，负数按 0 处理
     */
    public ExtractPlan limit(long max)
    {
        setLimit(max);
        return this;
    }

    /**
     * 提交后执行，参数为实际交出的数量
     */
    public ExtractPlan onCommit(StorageEffect effect)
    {
        addEffect(effect);
        return this;
    }
}
