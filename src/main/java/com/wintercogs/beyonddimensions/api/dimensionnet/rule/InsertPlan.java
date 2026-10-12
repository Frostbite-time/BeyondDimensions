package com.wintercogs.beyonddimensions.api.dimensionnet.rule;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;

/**
 * 插入规则的计划。请求的资源只由给出计划的这条规则决定；{@link #add}、{@link #take} 列出的资源会再经过规则，
 * 但同一条转换链上每条规则最多生效一次。
 * <pre>{@code
 * InsertPlan.convert(1).add(emptyBucket, 1).add(water, 1000) // 每个水桶拆成空桶和一桶水
 * InsertPlan.convert(10).add(gold, 64)                        // 每 10 个换 64 个，零头退回
 * InsertPlan.store().take(energy, 10)                         // 照常存入，每个扣 10 能量，不够就不收
 * }</pre>
 */
public final class InsertPlan extends StoragePlan
{
    private final boolean keepsInput;

    private InsertPlan(boolean rejected, long unit, boolean keepsInput)
    {
        super(rejected, unit);
        this.keepsInput = keepsInput;
    }

    /**
     * 不接收
     */
    public static InsertPlan reject()
    {
        return new InsertPlan(true, 1L, false);
    }

    /**
     * 原样存入，不再经过后面的规则；可以再附加变化、限量或效果
     */
    public static InsertPlan store()
    {
        return new InsertPlan(false, 1L, true);
    }

    /**
     * 接收但不存入任何东西
     */
    public static InsertPlan discard()
    {
        return new InsertPlan(false, 1L, false);
    }

    /**
     * 每 unit 个请求的资源换成之后 {@link #add}、{@link #take} 列出的变化，自身不存入
     */
    public static InsertPlan convert(long unit)
    {
        return new InsertPlan(false, unit, false);
    }

    /**
     * 每单位加入 amount 个 key
     */
    public InsertPlan add(IStackKey<?> key, long amount)
    {
        addChange(key, amount, true);
        return this;
    }

    /**
     * 每单位扣除 amount 个 key
     */
    public InsertPlan take(IStackKey<?> key, long amount)
    {
        addChange(key, amount, false);
        return this;
    }

    /**
     * 最多接收 max 个请求的资源，负数按 0 处理
     */
    public InsertPlan limit(long max)
    {
        setLimit(max);
        return this;
    }

    /**
     * 提交后执行，参数为实际接收的数量
     */
    public InsertPlan onCommit(StorageEffect effect)
    {
        addEffect(effect);
        return this;
    }

    /**
     * 请求的资源本身是否存入
     */
    public boolean keepsInput()
    {
        return keepsInput;
    }
}
