package com.wintercogs.beyonddimensions.api.dimensionnet;

import com.wintercogs.beyonddimensions.api.dimensionnet.rule.UnifiedStorageRules;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.util.ResourceTransfer;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;


/**
 * 维度网络的统一存储：
 * - 策略采用“到0即删除”（继承 UnorderedStackHandlerRemoveZero）
 * - 在 onChange() 时对接 DimensionsNet.setDirty()，再广播变更（调用 super.onChange()）
 * - 资源进出都经过 {@link UnifiedStorageRules}：规则只给出计划，这里在一次事务里算完，接收的整单位一起提交，
 * 余量总是调用方交来的那种资源
 * - 注意：此存储仅用于维度网络，维度网络也仅使用此存储，以便存放一些维度网络专用特性
 */
public class UnifiedStorage extends UnorderedStackHandlerRemoveZero
{
    // 规则计算计划期间，同一线程不允许真正改动任何统一存储
    private static final ThreadLocal<int[]> PLANNING = ThreadLocal.withInitial(() -> new int[1]);

    /**
     * 对应的维度网络，仅用于持久化脏标记
     */
    private final DimensionsNet net;

    public UnifiedStorage(DimensionsNet net, UiTimestampPolicy uiTimestampPolicy)
    {
        super(uiTimestampPolicy);
        this.net = net;
    }

    public UnifiedStorage(DimensionsNet net, UiTimestampPolicy uiTimestampPolicy, long slotCapacity, int slotMaxSize)
    {
        super(uiTimestampPolicy, slotCapacity, slotMaxSize);
        this.net = net;
    }

    /**
     * 返回一个“完全不可用”的空实现（0容量、0槽位），对外表现为全空容器且不会做脏标记
     */
    public static UnifiedStorage getEmpty()
    {
        return new UnifiedStorage(null, UiTimestampPolicy.NONE, 0, 0)
        {
            @Override
            public void onChange()
            {
                // no-op：空实现不做脏标记也不广播
            }

            @Override
            public long getSlotCapacity(int slot)
            {
                return 0L;
            }

            @Override
            public boolean isEmpty()
            {
                return true;
            }
        };
    }

    @Nullable
    public DimensionsNet getNet()
    {
        return net;
    }

    @Override
    public void onChange()
    {
        if (net != null)
        {
            net.setDirty();
        }
        // 调用基类的广播逻辑（Any/Delta 订阅）
        super.onChange();
    }

    @Override
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        return insert(key, amount, simulate, null);
    }

    /**
     * 插入，并且只接收最终加入网络的每种资源都满足 admits 的部分，规则转换出的资源同样要满足
     *
     * @param admits null 表示不限
     * @return 余量，总是传入的那种资源
     */
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate, @Nullable Predicate<IStackKey<?>> admits)
    {
        if (!simulate)
            checkNotPlanning();
        if (key == null || key.isEmpty() || amount <= 0L)
            return new KeyAmount(Objects.requireNonNullElse(key, EmptyStackKey.INSTANCE), Math.max(0L, amount));
        // 没有网络的空存储不走规则，免得销毁类规则在没有网络时吞掉东西
        if (net == null || !StorageTransaction.hasInsertRule(key))
        {
            if (admits != null && !admits.test(key))
                return new KeyAmount(key, amount);
            return super.insert(key, amount, simulate);
        }
        StorageTransaction transaction = new StorageTransaction(this, admits);
        long accepted;
        try
        {
            accepted = transaction.insert(key, amount);
        }
        catch (StorageTransaction.RuleFailed failed)
        {
            return new KeyAmount(key, amount);
        }
        if (!simulate)
            transaction.commit();
        return new KeyAmount(key, amount - accepted);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long amount, boolean simulate)
    {
        if (amount <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);

        KeyAmount stack = getStackBySlot(slot);
        if (stack.isEmpty())
            return stack;

        return extract(stack.key(), amount, simulate, false);
    }

    /**
     * 交出的总是网络里现成的资源；模糊匹配时先定下数量最多的变体，规则看到的就是它。
     * 模拟给出 n 个时，状态不变的情况下不超过 n 个的实际提取总能足量交出
     */
    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate, boolean fuzzy)
    {
        if (!simulate)
            checkNotPlanning();
        if (key == null || amount <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        IStackKey<?> target = fuzzy ? mostAbundantVariant(key) : key;
        if (target == null)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        if (net == null || !StorageTransaction.hasExtractRule(target))
            return super.extract(target, amount, simulate, false);
        StorageTransaction transaction = new StorageTransaction(this, null);
        long delivered;
        try
        {
            delivered = transaction.extract(target, amount);
        }
        catch (StorageTransaction.RuleFailed failed)
        {
            return new KeyAmount(target, 0L);
        }
        if (!simulate)
            transaction.commit();
        return new KeyAmount(target, delivered);
    }

    /**
     * 取出资源放进别处，不需要先取后放回：先模拟能给出什么、对方能收下多少，再只取出收得下的数量。
     * 模糊匹配只用来选定变体，之后按这个变体精确取出
     *
     * @param accepts 对方能收下多少，只模拟，不改动对方
     * @param place   把取出的资源放进对方，返回没放下的数量
     * @return 放进对方的资源
     */
    public @NotNull KeyAmount extractInto(IStackKey<?> key, long amount, boolean fuzzy, ToLongFunction<KeyAmount> accepts, ToLongFunction<KeyAmount> place)
    {
        return ResourceTransfer.move(key, amount, (k, n, simulate) -> extract(k, n, simulate, simulate && fuzzy), this::insert, accepts, place);
    }

    @Override
    public @NotNull KeyAmount extract(TagKey<?> tagKey, long amount, boolean simulate)
    {
        var key = tagIndex().get(tagKey).stream().findFirst();
        if (key.isEmpty() || amount <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);

        return extract(key.get(), amount, simulate, false);
    }

    /* ---------- 供事务使用 ---------- */

    static void beginPlanning()
    {
        PLANNING.get()[0]++;
    }

    static void endPlanning()
    {
        PLANNING.get()[0]--;
    }

    private static void checkNotPlanning()
    {
        if (PLANNING.get()[0] > 0)
            throw new IllegalStateException("A storage rule tried to modify a network storage while planning; put the change in its plan instead");
    }

    long storedAmount(IStackKey<?> key)
    {
        return storage.getOrDefault(key, 0L);
    }

    int slotsInUse()
    {
        return slotIndex.size();
    }

    /**
     * 不经过规则加入，返回没放下的数量，仅供事务内部处理
     */
    long addWithoutRules(IStackKey<?> key, long amount)
    {
        return super.insert(key, amount, false).amount();
    }

    /**
     * 不经过规则扣除，返回扣除的数量，仅供事务内部处理
     */
    long removeWithoutRules(IStackKey<?> key, long amount)
    {
        return super.extract(key, amount, false, false).amount();
    }

}
