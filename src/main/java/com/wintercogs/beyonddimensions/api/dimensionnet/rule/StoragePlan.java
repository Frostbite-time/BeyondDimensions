package com.wintercogs.beyonddimensions.api.dimensionnet.rule;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 规则给出的计划：按“单位”描述网络的变化。只有能完整处理的整单位会被接收，凑不满一个单位的部分退回给调用方
 */
public abstract sealed class StoragePlan permits InsertPlan, ExtractPlan
{
    /**
     * 每单位让网络加入（add 为真）或扣除的一份资源
     */
    public record Change(@NotNull IStackKey<?> key, long amount, boolean add)
    {
    }

    private final boolean rejected;
    private final long unit;
    private long limit = -1L;
    private final List<Change> changes = new ArrayList<>();
    private final List<StorageEffect> effects = new ArrayList<>();

    StoragePlan(boolean rejected, long unit)
    {
        if (unit <= 0L)
            throw new IllegalArgumentException("A plan unit must be positive, got " + unit);
        this.rejected = rejected;
        this.unit = unit;
    }

    void addChange(IStackKey<?> key, long amount, boolean add)
    {
        Objects.requireNonNull(key);
        if (key.isEmpty() || amount <= 0L)
            throw new IllegalArgumentException("A plan change needs a resource and a positive amount, got " + amount + " of " + key);
        changes.add(new Change(key, amount, add));
    }

    void setLimit(long max)
    {
        limit = Math.max(0L, max);
    }

    void addEffect(StorageEffect effect)
    {
        effects.add(Objects.requireNonNull(effect));
    }

    /**
     * 是否整个拒绝
     */
    public boolean rejected()
    {
        return rejected;
    }

    /**
     * 每单位包含的请求数量
     */
    public long unit()
    {
        return unit;
    }

    /**
     * 最多处理的请求数量，负数表示不限
     */
    public long limit()
    {
        return limit;
    }

    /**
     * 每单位的附加变化，按加入计划的顺序
     */
    public List<Change> changes()
    {
        return Collections.unmodifiableList(changes);
    }

    /**
     * 提交后执行的效果
     */
    public List<StorageEffect> effects()
    {
        return Collections.unmodifiableList(effects);
    }
}
