package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.ExtractPlan;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.ExtractRule;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.InsertPlan;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.InsertRule;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.StorageEffect;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.StoragePlan;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.StorageRequest;
import com.wintercogs.beyonddimensions.api.dimensionnet.rule.UnifiedStorageRules;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * 统一存储的一次操作：按规则把请求算成网络的变化，先记在这里，不碰真实存储。
 * 模拟时直接丢弃；执行时一次性写入存储，再运行各计划的效果
 */
final class StorageTransaction
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 规则出错，整个操作作废；日志已由规则表记录
     */
    static final class RuleFailed extends RuntimeException
    {
        private RuleFailed()
        {
            super(null, null, false, false);
        }
    }

    private static final RuleFailed RULE_FAILED = new RuleFailed();

    private record Undo(IStackKey<?> key, long previous, boolean existed)
    {
    }

    private record Effect(UnifiedStorageRules.Entry<?> entry, StorageEffect effect, StorageRequest request, long amount)
    {
    }

    /**
     * 一条转换链上已经生效的规则
     */
    private record Path(UnifiedStorageRules.Entry<?> entry, @Nullable Path parent)
    {
        static boolean contains(@Nullable Path path, UnifiedStorageRules.Entry<?> entry)
        {
            for (Path p = path; p != null; p = p.parent)
                if (p.entry == entry) return true;
            return false;
        }
    }

    private final class Request implements StorageRequest
    {
        private final IStackKey<?> key;
        private final long amount;

        Request(IStackKey<?> key, long amount)
        {
            this.key = key;
            this.amount = amount;
        }

        @Override
        public IStackKey<?> key()
        {
            return key;
        }

        @Override
        public long amount()
        {
            return amount;
        }

        @Override
        public DimensionsNet net()
        {
            return storage.getNet();
        }

        @Override
        public long stored(IStackKey<?> key)
        {
            return amountOf(key);
        }
    }

    private final UnifiedStorage storage;
    private final @Nullable Predicate<IStackKey<?>> admits;
    private final UnifiedStorageRules.Table<InsertRule> insertRules = UnifiedStorageRules.insertTable();
    private final UnifiedStorageRules.Table<ExtractRule> extractRules = UnifiedStorageRules.extractTable();
    // 相对存储现有数量的增减，按首次变化的顺序
    private final Map<IStackKey<?>, Long> changes = new LinkedHashMap<>();
    private final List<Undo> journal = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private int slotsInUse;

    /**
     * @param admits 只接收最终加入的每种资源都满足它的部分，null 表示不限
     */
    StorageTransaction(UnifiedStorage storage, @Nullable Predicate<IStackKey<?>> admits)
    {
        this.storage = storage;
        this.admits = admits;
        this.slotsInUse = storage.slotsInUse();
    }

    static boolean hasInsertRule(IStackKey<?> key)
    {
        return UnifiedStorageRules.insertTable().next(key, null) != null;
    }

    static boolean hasExtractRule(IStackKey<?> key)
    {
        return UnifiedStorageRules.extractTable().next(key, null) != null;
    }

    /**
     * 加入 amount 个 key，返回接收的数量
     */
    long insert(IStackKey<?> key, long amount)
    {
        return insert(key, amount, null);
    }

    /**
     * 交出 amount 个 key，返回交出的数量
     */
    long extract(IStackKey<?> key, long amount)
    {
        return extract(key, amount, null);
    }

    private long insert(IStackKey<?> key, long amount, @Nullable Path path)
    {
        for (var entry = insertRules.next(key, null); entry != null; entry = insertRules.next(key, entry))
        {
            if (Path.contains(path, entry))
                continue;
            Request request = new Request(key, amount);
            InsertPlan plan;
            UnifiedStorage.beginPlanning();
            try
            {
                plan = entry.rule().plan(request);
            }
            catch (RuntimeException failure)
            {
                entry.report("planning an insert of " + amount + " " + key, failure);
                throw RULE_FAILED;
            }
            finally
            {
                UnifiedStorage.endPlanning();
            }
            if (plan != null)
                return apply(entry, plan, request, new Path(entry, path));
        }
        return add(key, amount);
    }

    private long extract(IStackKey<?> key, long amount, @Nullable Path path)
    {
        for (var entry = extractRules.next(key, null); entry != null; entry = extractRules.next(key, entry))
        {
            if (Path.contains(path, entry))
                continue;
            Request request = new Request(key, amount);
            ExtractPlan plan;
            UnifiedStorage.beginPlanning();
            try
            {
                plan = entry.rule().plan(request);
            }
            catch (RuntimeException failure)
            {
                entry.report("planning an extraction of " + amount + " " + key, failure);
                throw RULE_FAILED;
            }
            finally
            {
                UnifiedStorage.endPlanning();
            }
            if (plan != null)
                return apply(entry, plan, request, new Path(entry, path));
        }
        return remove(key, amount);
    }

    /**
     * 按计划处理请求，返回接收或交出的请求数量
     */
    private long apply(UnifiedStorageRules.Entry<?> entry, StoragePlan plan, Request request, Path path)
    {
        if (plan.rejected())
            return 0L;
        long unit = plan.unit();
        long units = request.amount() / unit;
        if (plan.limit() >= 0L)
            units = Math.min(units, plan.limit() / unit);
        long done = fit(units, k -> applyUnits(plan, request, k, path)) * unit;
        if (done > 0L)
            for (StorageEffect effect : plan.effects())
                effects.add(new Effect(entry, effect, request, done));
        return done;
    }

    /**
     * k 个单位的变化是否全部成立；不成立时可能留下部分变化，由调用方退回
     */
    private boolean applyUnits(StoragePlan plan, Request request, long k, Path path)
    {
        long amount = k * plan.unit();
        // 请求的资源只由这一条规则决定，直接存入或扣出；计划里列出的变化才再经过规则
        if (plan instanceof InsertPlan insertPlan)
        {
            if (insertPlan.keepsInput() && add(request.key(), amount) < amount)
                return false;
        }
        else if (remove(request.key(), amount) < amount)
        {
            return false;
        }
        for (StoragePlan.Change change : plan.changes())
        {
            long total;
            try
            {
                total = Math.multiplyExact(change.amount(), k);
            }
            catch (ArithmeticException overflow)
            {
                return false;
            }
            long done = change.add() ? insert(change.key(), total, path) : extract(change.key(), total, path);
            if (done < total)
                return false;
        }
        return true;
    }

    /**
     * 能完整成立的最大单位数：先试全部，不行再二分。成立的那份变化留在事务里
     */
    private long fit(long units, LongPredicate apply)
    {
        if (units <= 0L)
            return 0L;
        int mark = journal.size();
        int effectMark = effects.size();
        if (apply.test(units))
            return units;
        rollback(mark, effectMark);
        long fits = 0L, fails = units;
        while (fails - fits > 1L)
        {
            long middle = fits + (fails - fits) / 2;
            boolean ok = apply.test(middle);
            rollback(mark, effectMark);
            if (ok)
                fits = middle;
            else
                fails = middle;
        }
        if (fits > 0L && !apply.test(fits))
        {
            // 同样的状态给出了不同的计划，放弃这一步
            rollback(mark, effectMark);
            return 0L;
        }
        return fits;
    }

    private long add(IStackKey<?> key, long amount)
    {
        if (amount <= 0L || admits != null && !admits.test(key))
            return 0L;
        long current = amountOf(key);
        if (current <= 0L && slotsInUse >= storage.slotMaxSize)
            return 0L;
        long room = current >= storage.slotCapacity ? 0L : storage.slotCapacity - current;
        long added = Math.min(room, amount);
        if (added > 0L)
            change(key, added);
        return added;
    }

    private long remove(IStackKey<?> key, long amount)
    {
        long removed = Math.min(Math.max(0L, amountOf(key)), amount);
        if (removed > 0L)
            change(key, -removed);
        return removed;
    }

    long amountOf(IStackKey<?> key)
    {
        return storage.storedAmount(key) + changes.getOrDefault(key, 0L);
    }

    private void change(IStackKey<?> key, long delta)
    {
        long before = amountOf(key);
        Long previous = changes.get(key);
        journal.add(new Undo(key, previous == null ? 0L : previous, previous != null));
        changes.put(key, (previous == null ? 0L : previous) + delta);
        countSlot(before, before + delta);
    }

    private void rollback(int mark, int effectMark)
    {
        for (int i = journal.size() - 1; i >= mark; i--)
        {
            Undo undo = journal.remove(i);
            long before = amountOf(undo.key());
            if (undo.existed())
                changes.put(undo.key(), undo.previous());
            else
                changes.remove(undo.key());
            countSlot(before, amountOf(undo.key()));
        }
        while (effects.size() > effectMark)
            effects.remove(effects.size() - 1);
    }

    private void countSlot(long before, long after)
    {
        if (before <= 0L && after > 0L)
            slotsInUse++;
        else if (before > 0L && after <= 0L)
            slotsInUse--;
    }

    /**
     * 写入存储，再按顺序运行效果
     */
    void commit()
    {
        // 先扣后加，种类上限不会在中途被占满
        for (Map.Entry<IStackKey<?>, Long> change : changes.entrySet())
        {
            long amount = -change.getValue();
            if (amount > 0L && storage.removeWithoutRules(change.getKey(), amount) != amount)
                LOGGER.error("Storage changed during a transaction; could not remove {} {}", amount, change.getKey());
        }
        for (Map.Entry<IStackKey<?>, Long> change : changes.entrySet())
        {
            long amount = change.getValue();
            if (amount > 0L && storage.addWithoutRules(change.getKey(), amount) != 0L)
                LOGGER.error("Storage changed during a transaction; could not add all of {} {}", amount, change.getKey());
        }
        changes.clear();
        journal.clear();
        for (Effect effect : List.copyOf(effects))
        {
            try
            {
                effect.effect().run(effect.request(), effect.amount());
            }
            catch (RuntimeException failure)
            {
                effect.entry().report("running an effect", failure);
            }
        }
        effects.clear();
    }
}
