package com.wintercogs.beyonddimensions.api.storage.handler.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.item.MatterCompressionBall;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractUnorderedStackHandler implements IStackHandler
{
    /* ---------- 是否保留 amount==0 的键 ---------- */
    public enum ZeroPolicy
    {KEEP_ZERO, REMOVE_ON_ZERO}

    /* ---------- UI 时间戳维护策略 ---------- */
    public enum UiTimestampPolicy
    {NONE, AUTO} // NONE: 不主动维护；AUTO: 自动维护

    private @NotNull ZeroPolicy zeroPolicy;
    private @NotNull UiTimestampPolicy uiTimestampPolicy; // 默认自动维护（可通过 setter 修改）

    protected AbstractUnorderedStackHandler(ZeroPolicy policy, UiTimestampPolicy uiTimestampPolicy)
    {
        this.zeroPolicy = Objects.requireNonNull(policy);
        this.uiTimestampPolicy = Objects.requireNonNull(uiTimestampPolicy);
    }

    /* ---------- 内部存储 ---------- */
    protected final Map<IStackKey<?>, Long> storage = new HashMap<>();
    protected final ArrayList<IStackKey<?>> slotIndex = new ArrayList<>();
    protected final Map<IStackKey<?>, Integer> posMap = new HashMap<>();
    protected final Map<IStackKey<?>, Object> key2stackMap = new HashMap<>();
    protected final Map<ResourceLocation, TypeBucket> type2buckets = new HashMap<>();
    protected final Multimap<TagKey<?>, IStackKey<?>> tag2stackMap = HashMultimap.create();

    /* ---------- 仅供 UI 使用的时间表 ---------- */
    /**
     * 记录该 Key 最近一次“从无到有建槽位”的时间（毫秒时间戳）。仅供 UI 展示，无其他语义。
     */
    protected final Map<IStackKey<?>, Long> creationTimeMap = new HashMap<>();
    /**
     * 记录该 Key 最近一次“数量被修改”的时间（毫秒时间戳）。仅供 UI 展示，无其他语义。
     */
    protected final Map<IStackKey<?>, Long> lastModifiedTimeMap = new HashMap<>();

    /* ---------- 只读、动态的 KeyAmount 视图 ---------- */
    private final List<KeyAmount> entriesView = Collections.unmodifiableList(
            new AbstractList<>()
            {
                @Override
                public KeyAmount get(int index)
                {
                    IStackKey<?> key = slotIndex.get(index);
                    long amt = storage.getOrDefault(key, 0L);
                    return new KeyAmount(key, amt);
                }

                @Override
                public int size()
                {
                    return slotIndex.size();
                }
            }
    );

    /* ---------- 订阅：强/弱 + 增量上下文 ---------- */
    @FunctionalInterface
    public interface DeltaListener
    {
        void onDelta(IStackKey<?> key, long size, boolean insert);
    }

    @FunctionalInterface
    public interface AnyChangeListener
    {
        void onAnyChange();
    }

    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D>
    {
        void accept(A a, B b, C c, D d);
    }

    private static final class OwnerRef extends WeakReference<Object>
    {
        OwnerRef(Object owner, ReferenceQueue<Object> q)
        {
            super(owner, q);
        }
    }

    private static final class AnyEntry
    {
        final OwnerRef ownerRef;
        final AnyChangeListener listener;

        AnyEntry(OwnerRef ref, AnyChangeListener l)
        {
            this.ownerRef = ref;
            this.listener = l;
        }
    }

    private static final class DeltaEntry
    {
        final OwnerRef ownerRef;
        final DeltaListener listener;

        DeltaEntry(OwnerRef ref, DeltaListener l)
        {
            this.ownerRef = ref;
            this.listener = l;
        }
    }

    private final CopyOnWriteArrayList<AnyEntry> anyListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DeltaEntry> deltaListeners = new CopyOnWriteArrayList<>();
    private int deltaContextDepth = 0;

    private void beginDeltaContext()
    {
        deltaContextDepth++;
    }

    private void endDeltaContext()
    {
        deltaContextDepth = Math.max(0, deltaContextDepth - 1);
    }

    private boolean inDeltaContext()
    {
        return deltaContextDepth > 0;
    }

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    /* ---------- 可配置容量/槽位上限 ---------- */
    public long slotCapacity = Long.MAX_VALUE;
    public int slotMaxSize = Integer.MAX_VALUE;

    /* =================== UI 时间策略：工具 =================== */
    protected long nowMillis()
    {
        return System.currentTimeMillis();
    }

    /**
     * 主动覆写：设定某个 key 的“建槽位时间”
     */
    public void setCreationTime(IStackKey<?> key, long timeMillis)
    {
        if (key != null) creationTimeMap.put(key, timeMillis);
    }

    /**
     * 主动覆写：设定某个 key 的“最后修改时间”
     */
    public void setLastModifiedTime(IStackKey<?> key, long timeMillis)
    {
        if (key != null) lastModifiedTimeMap.put(key, timeMillis);
    }

    /**
     * 获取“建槽位时间表”的引用（按你的要求，返回真实引用）
     */
    public Map<IStackKey<?>, Long> getCreationTimeMap()
    {
        return creationTimeMap;
    }

    /**
     * 获取“最后修改时间表”的引用（按你的要求，返回真实引用）
     */
    public Map<IStackKey<?>, Long> getLastModifiedTimeMap()
    {
        return lastModifiedTimeMap;
    }

    /**
     * 切换 UI 时间戳策略
     */
    public void setUiTimestampPolicy(@NotNull UiTimestampPolicy policy)
    {
        Objects.requireNonNull(policy);
        if (this.uiTimestampPolicy == policy) return;

        this.uiTimestampPolicy = policy;
    }

    @NotNull
    public UiTimestampPolicy getUiTimestampPolicy()
    {
        return this.uiTimestampPolicy;
    }

    public void setZeroPolicy(@NotNull ZeroPolicy policy)
    {
        Objects.requireNonNull(policy);
        if (this.zeroPolicy == policy) return;

        this.zeroPolicy = policy;
        reconcileAfterZeroPolicyChange();
    }

    @NotNull
    public ZeroPolicy getZeroPolicy()
    {
        return this.zeroPolicy;
    }

    /**
     * 在状态切换到remove zero时，做一个零键清理
     */
    private void reconcileAfterZeroPolicyChange()
    {
        if (this.zeroPolicy != ZeroPolicy.REMOVE_ON_ZERO) return;

        boolean anyChange = false;
        for (Iterator<Map.Entry<IStackKey<?>, Long>> it = storage.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<IStackKey<?>, Long> e = it.next();
            if (e.getValue() <= 0L)
            {
                anyChange = true;
                IStackKey<?> key = e.getKey();
                it.remove();
                removeFromIndex(key);
            }
        }

        if (anyChange)
        {
            onChange();
        }
    }

    /* ================= 公共订阅 API ================= */
    public AutoCloseable subscribeAny(Object owner, AnyChangeListener onAny)
    {
        if (owner == null || onAny == null) throw new IllegalArgumentException();
        drainRefQueue();
        AnyEntry e = new AnyEntry(new OwnerRef(owner, refQueue), onAny);
        anyListeners.add(e);
        return () -> anyListeners.remove(e);
    }

    public AutoCloseable subscribeDelta(Object owner, DeltaListener onDelta)
    {
        if (owner == null || onDelta == null) throw new IllegalArgumentException();
        drainRefQueue();
        DeltaEntry e = new DeltaEntry(new OwnerRef(owner, refQueue), onDelta);
        deltaListeners.add(e);
        return () -> deltaListeners.remove(e);
    }

    public <T> AutoCloseable subscribeAnyWeak(T owner, java.util.function.Consumer<T> onAny)
    {
        if (owner == null || onAny == null) throw new IllegalArgumentException();
        drainRefQueue();
        OwnerRef ref = new OwnerRef(owner, refQueue);
        AnyEntry e = new AnyEntry(ref, () -> {
            @SuppressWarnings("unchecked") T o = (T) ref.get();
            if (o != null) onAny.accept(o);
            else drainRefQueue();
        });
        anyListeners.add(e);
        return () -> anyListeners.remove(e);
    }

    public <T> AutoCloseable subscribeDeltaWeak(T owner, QuadConsumer<T, IStackKey<?>, Long, Boolean> onDelta)
    {
        if (owner == null || onDelta == null) throw new IllegalArgumentException();
        drainRefQueue();
        OwnerRef ref = new OwnerRef(owner, refQueue);
        DeltaEntry e = new DeltaEntry(ref, (type, size, insert) -> {
            @SuppressWarnings("unchecked") T o = (T) ref.get();
            if (o != null) onDelta.accept(o, type, size, insert);
            else drainRefQueue();
        });
        deltaListeners.add(e);
        return () -> deltaListeners.remove(e);
    }

    protected void fireChange()
    {
        if (inDeltaContext()) return;
        drainRefQueue();
        for (AnyEntry e : anyListeners)
        {
            try
            {
                e.listener.onAnyChange();
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    protected void fireDelta(IStackKey<?> type, long size, boolean insert)
    {
        drainRefQueue();
        for (DeltaEntry e : deltaListeners)
        {
            try
            {
                e.listener.onDelta(type, size, insert);
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    private void drainRefQueue()
    {
        OwnerRef ref;
        while ((ref = (OwnerRef) refQueue.poll()) != null)
        {
            OwnerRef dead = ref;
            anyListeners.removeIf(e -> e.ownerRef == dead);
            deltaListeners.removeIf(e -> e.ownerRef == dead);
        }
    }

    /* ============== 生命周期：留给子类覆写的统一入口 ============== */
    @Override
    public void onChange()
    {
        fireChange();
    }

    protected final void onContentChanged(IStackKey<?> type, long size, boolean insert)
    {
        beginDeltaContext();
        try
        {
            onChange();
        }
        finally
        {
            endDeltaContext();
        }
        fireDelta(type, size, insert);
    }

    /* ================= IStackHandler 实现（通用） ================= */
    @Override
    public List<KeyAmount> getStorage()
    {
        return entriesView;
    }

    @Override
    public void clearStorage()
    {
        storage.clear();
        slotIndex.clear();
        posMap.clear();
        key2stackMap.clear();
        type2buckets.clear();
        tag2stackMap.clear();
        creationTimeMap.clear();
        lastModifiedTimeMap.clear();
        onChange();
    }

    @Override
    public @NotNull KeyAmount getStackBySlot(int slot)
    {
        if (slot < 0 || slot >= slotIndex.size()) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        IStackKey<?> key = slotIndex.get(slot);
        return new KeyAmount(key, storage.getOrDefault(key, 0L));
    }

    @Override
    public @NotNull KeyAmount getStackByKey(IStackKey<?> key)
    {
        if (key == null) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        return new KeyAmount(key, storage.getOrDefault(key, 0L));
    }

    @Override
    public boolean hasStack(IStackKey<?> key)
    {
        if (key == null) return false;
        if (this.zeroPolicy == ZeroPolicy.KEEP_ZERO)
        {
            return storage.containsKey(key);
        }
        return storage.getOrDefault(key, 0L) > 0L;
    }

    public long setAmountByKey(IStackKey<?> key, long amount)
    {
        if (key == null) return 0L;

        long current = storage.getOrDefault(key, 0L);
        long target = Math.max(0L, Math.min(amount, slotCapacity));
        if (target == current) return current;

        if (target == 0L)
        {
            if (zeroPolicy == ZeroPolicy.REMOVE_ON_ZERO)
            {
                if (current > 0L || posMap.containsKey(key))
                {
                    storage.remove(key);
                    removeFromIndex(key);
                    // 修改时间：数量变化
                    if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
                    {
                        lastModifiedTimeMap.put(key, nowMillis());
                    }
                    onContentChanged(key, current, false);
                }
            }
            else
            { // KEEP_ZERO
                storage.put(key, 0L);
                ensureInIndex(key); // ensureInIndex 内部会做“建槽位时间”记录（首次）
                if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
                {
                    lastModifiedTimeMap.put(key, nowMillis());
                }
                long delta = current; // 全部减少
                if (delta > 0L) onContentChanged(key, delta, false);
            }
            return 0L;
        }

        // target > 0
        boolean isNew = (current == 0L) && !posMap.containsKey(key);
        if (isNew && slotIndex.size() >= slotMaxSize)
        {
            return current;
        }
        storage.put(key, target);
        ensureInIndex(key);
        if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
        {
            lastModifiedTimeMap.put(key, nowMillis());
        }
        long delta = Math.abs(target - current);
        if (delta > 0L) onContentChanged(key, delta, target > current);
        return target;
    }

    @Override
    public void setStackDirectly(int slot, IStackKey<?> newKey, long amount)
    {
        if (slot < 0 || slot >= slotIndex.size()) return;

        IStackKey<?> oldKey = slotIndex.get(slot);
        long target = Math.max(0L, amount);

        if (Objects.equals(oldKey, newKey))
        {
            setAmountByKey(oldKey, target);
            return;
        }

        long oldAmt = storage.getOrDefault(oldKey, 0L);
        if (zeroPolicy == ZeroPolicy.REMOVE_ON_ZERO)
        {
            storage.remove(oldKey);
            removeFromIndex(oldKey);
        }
        else
        {
            storage.put(oldKey, 0L);
            // 保留索引
        }
        if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
        {
            lastModifiedTimeMap.put(oldKey, nowMillis());
        }
        if (oldAmt > 0L) onContentChanged(oldKey, oldAmt, false);

        if (newKey != null)
        {
            setAmountByKey(newKey, target);
            if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
            {
                lastModifiedTimeMap.put(newKey, nowMillis());
            }
        }
    }

    @Override
    public void addStackDirectly(IStackKey<?> key, long amount)
    {
        insert(key, amount, false);
    }

    @Override
    public @NotNull KeyAmount insert(int slot, IStackKey<?> key, long amount, boolean simulate)
    {
        return insert(key, amount, simulate);
    }

    @Override
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        if (key == null) return new KeyAmount(EmptyStackKey.INSTANCE, Math.max(0L, amount));
        long add = Math.max(0L, amount);
        if (add == 0L) return new KeyAmount(key, 0L);

        if (key instanceof ItemStackKey itemKey && itemKey.getSource() == BDItems.MATTER_COMPRESS_BALL.get())
        {
            return unzipMatterBall(itemKey, add, simulate);
        }

        long current = storage.getOrDefault(key, 0L);
        boolean needNewSlot = (current == 0L) && !posMap.containsKey(key);
        if (needNewSlot && slotIndex.size() >= slotMaxSize)
        {
            return new KeyAmount(key, add);
        }

        long cap = slotCapacity;
        long room = cap <= current ? 0L : (cap - current);
        if (room <= 0L) return new KeyAmount(key, add);

        long actual = Math.min(room, add);
        long leftover = add - actual;

        if (!simulate)
        {
            storage.put(key, current + actual);
            ensureInIndex(key);
            if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
            {
                lastModifiedTimeMap.put(key, nowMillis());
            }
            onContentChanged(key, actual, true);
        }
        return new KeyAmount(key, leftover);
    }

    protected KeyAmount unzipMatterBall(ItemStackKey ballKey, long ballCount, boolean simulate)
    {
        ItemStack ballStack = ballKey.copyStackWithCount(ballCount);
        if (ballStack.isEmpty() || !(ballStack.getItem() instanceof MatterCompressionBall))
        {
            return new KeyAmount(ballKey, ballCount);
        }

        // 1.20.1：从 NBT stack_list 读取 contents
        List<KeyAmount> contents;
        if (!MatterCompressionBall.hasIStackList(ballStack))
        {
            contents = Collections.emptyList();
        }
        else
        {
            try
            {
                contents = MatterCompressionBall.getIStackList(ballStack);
            }
            catch (Throwable t)
            {
                contents = Collections.emptyList();
            }
        }

        if (contents.isEmpty()) return new KeyAmount(ballKey, 0L);

        final Map<IStackKey<?>, Long> needMap = new HashMap<>();
        try
        {
            for (KeyAmount entry : contents)
            {
                if (entry.isEmpty()) continue;
                long scaled = Math.multiplyExact(entry.amount(), ballCount);
                needMap.merge(entry.key(), scaled, Math::addExact);
            }
        }
        catch (ArithmeticException e)
        {
            return new KeyAmount(ballKey, ballCount);
        }

        int freeSlots = Math.max(0, slotMaxSize - slotIndex.size());
        int newKeysNeeded = 0;
        for (Map.Entry<IStackKey<?>, Long> e : needMap.entrySet())
        {
            IStackKey<?> k = e.getKey();
            long need = e.getValue();
            long current = storage.getOrDefault(k, 0L);
            boolean isNew = (current == 0L) && !posMap.containsKey(k);
            if (isNew && ++newKeysNeeded > freeSlots) return new KeyAmount(ballKey, ballCount);
            long room = (slotCapacity <= current) ? 0L : (slotCapacity - current);
            if (need > room) return new KeyAmount(ballKey, ballCount);
        }

        if (simulate) return new KeyAmount(ballKey, 0L);

        final ArrayList<KeyAmount> applied = new ArrayList<>();
        for (KeyAmount entry : contents)
        {
            if (entry.isEmpty()) continue;
            long scaled;
            try
            {
                scaled = Math.multiplyExact(entry.amount(), ballCount);
            }
            catch (ArithmeticException e)
            {
                for (int i = applied.size() - 1; i >= 0; i--)
                {
                    KeyAmount a = applied.get(i);
                    extract(a.key(), a.amount(), false, false);
                }
                return new KeyAmount(ballKey, ballCount);
            }
            KeyAmount leftover = insert(entry.key(), scaled, false);
            long ok = scaled - leftover.amount();
            if (ok > 0L) applied.add(new KeyAmount(entry.key(), ok));
            if (leftover.amount() > 0L)
            {
                for (int i = applied.size() - 1; i >= 0; i--)
                {
                    KeyAmount a = applied.get(i);
                    extract(a.key(), a.amount(), false, false);
                }
                return new KeyAmount(ballKey, ballCount);
            }
        }
        return new KeyAmount(ballKey, 0L);
    }

    private @NotNull KeyAmount extractByKey(IStackKey<?> key, long count, boolean simulate)
    {
        long current = storage.getOrDefault(key, 0L);
        if (current <= 0L) return new KeyAmount(key, 0L);

        long take = Math.min(count, current);
        if (!simulate)
        {
            long left = current - take;
            if (left == 0L)
            {
                if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
                {
                    lastModifiedTimeMap.put(key, nowMillis());
                }
                if (zeroPolicy == ZeroPolicy.REMOVE_ON_ZERO)
                {
                    storage.remove(key);
                    removeFromIndex(key);
                }
                else
                {
                    storage.put(key, 0L);
                    ensureInIndex(key);
                }
            }
            else
            {
                storage.put(key, left);
                if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
                {
                    lastModifiedTimeMap.put(key, nowMillis());
                }
            }
            onContentChanged(key, take, false);
        }
        return new KeyAmount(key, take);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long count, boolean simulate)
    {
        if (slot < 0 || slot >= slotIndex.size() || count <= 0L)
        {
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        }
        IStackKey<?> key = slotIndex.get(slot);
        return extractByKey(key, count, simulate);
    }

    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate, boolean fuzzy)
    {
        if (fuzzy)
        {
            var fuzzyKey = key;
            key = slotIndex.stream().filter(x -> x.isSame(fuzzyKey)).findFirst().orElse(null);
        }
        if (key == null || amount <= 0L) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        return extractByKey(key, amount, simulate);
    }

    public @NotNull KeyAmount extract(TagKey<?> tagKey, long amount, boolean simulate)
    {
        var key = tag2stackMap.get(tagKey).stream().findFirst();
        if (key.isEmpty() || amount <= 0L) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        return extractByKey(key.get(), amount, simulate);
    }

    @Override
    public long getSlotCapacity(int slot)
    {
        return slotCapacity;
    }

    @Override
    public boolean isStackValid(int slot, IStackKey<?> key)
    {
        return true;
    }

    @Override
    public boolean isEmpty()
    {
        return slotIndex.isEmpty();
    }

    /* ---------------- 索引维护：O(1) 换尾 ---------------- */
    protected void ensureInIndex(IStackKey<?> key)
    {
        if (posMap.containsKey(key)) return;
        int idx = slotIndex.size();
        slotIndex.add(key);
        posMap.put(key, idx);
        bucketOf(key.getTypeId()).add(key);
        if (!key2stackMap.containsKey(key)) key2stackMap.put(key, key.copyStack());
        key.getTags().forEach(tag -> {
            tag2stackMap.put(tag, key);
        });
        // 新建槽位时间
        if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
        {
            creationTimeMap.put(key, nowMillis());
        }
    }

    protected void removeFromIndex(IStackKey<?> key)
    {
        Integer pos = posMap.remove(key);
        if (pos == null) return;
        int last = slotIndex.size() - 1;
        if (pos != last)
        {
            IStackKey<?> tail = slotIndex.get(last);
            slotIndex.set(pos, tail);
            posMap.put(tail, pos);
        }
        slotIndex.remove(last);
        bucketOf(key.getTypeId()).remove(key);
        key2stackMap.remove(key);
        key.getTags().forEach(tag -> {
            tag2stackMap.remove(tag, key);
        });
        // 为避免无上限增长，这里选择在移除槽位时一并清理时间记录
        creationTimeMap.remove(key);
        lastModifiedTimeMap.remove(key);
    }

    /* ---------------- 类型桶 ---------------- */
    public static final class TypeBucket
    {
        final ArrayList<IStackKey<?>> keys = new ArrayList<>();
        final Map<IStackKey<?>, Integer> pos = new HashMap<>();

        void add(IStackKey<?> k)
        {
            if (pos.containsKey(k)) return;
            int i = keys.size();
            keys.add(k);
            pos.put(k, i);
        }

        void remove(IStackKey<?> k)
        {
            Integer p = pos.remove(k);
            if (p == null) return;
            int last = keys.size() - 1;
            if (p != last)
            {
                IStackKey<?> tail = keys.get(last);
                keys.set(p, tail);
                pos.put(tail, p);
            }
            keys.remove(last);
        }

        public int size()
        {
            return keys.size();
        }

        public IStackKey<?> get(int i)
        {
            return keys.get(i);
        }
    }

    protected TypeBucket bucketOf(ResourceLocation type)
    {
        return type2buckets.computeIfAbsent(type, t -> new TypeBucket());
    }

    public Optional<TypeBucket> getBucket(ResourceLocation type)
    {
        return Optional.ofNullable(type2buckets.get(type));
    }

    public Object getOutStackByKey(IStackKey<?> key)
    {
        return key2stackMap.get(key);
    }

    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("slotCapacity", this.slotCapacity);
        tag.putInt("slotMaxSize", this.slotMaxSize);

        ListTag stacksTag = new ListTag();
        final boolean writeZero = (zeroPolicy == ZeroPolicy.KEEP_ZERO);

        for (Map.Entry<IStackKey<?>, Long> e : storage.entrySet())
        {
            IStackKey<?> key = e.getKey();
            long amount = e.getValue() == null ? 0L : e.getValue();
            if (key == null || key.isEmpty()) continue;
            if (!writeZero && amount <= 0L) continue;

            CompoundTag one = new CompoundTag();
            one.put("key", IStackKey.serializeNBTCommon(key));
            one.putLong("amount", amount);
            stacksTag.add(one);
        }

        tag.put("stacks", stacksTag);
        return tag;
    }

    /**
     * 读取：
     * - 新格式优先：stacks
     * - 否则旧格式回退：Stacks
     */
    public void deserializeNBT(CompoundTag tag)
    {
        clearStorage();
        if (tag == null) return;

        slotCapacity = tag.contains("slotCapacity", Tag.TAG_LONG) ? tag.getLong("slotCapacity") : Long.MAX_VALUE;
        slotMaxSize = tag.contains("slotMaxSize", Tag.TAG_INT) ? tag.getInt("slotMaxSize") : Integer.MAX_VALUE;

        // 1) 新格式
        if (tag.contains("stacks", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("stacks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag one = list.getCompound(i);

                if (!one.contains("key", Tag.TAG_COMPOUND)) continue;
                IStackKey<?> key;
                try
                {
                    key = IStackKey.deserializeNBTCommon(one.getCompound("key"));
                }
                catch (Throwable t)
                {
                    BeyondDimensions.LOGGER.warn("反序列化 stacks[{}].key 失败: {}", i, t.toString());
                    continue;
                }

                long amount = one.contains("amount", Tag.TAG_LONG) ? one.getLong("amount") : one.getLong("Amount");
                acceptEntry(key, amount);
            }
            return;
        }

        // 2) 旧格式回退：Stacks
        if (tag.contains("Stacks", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Stacks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag entry = list.getCompound(i);
                String typeStr = entry.getString("Type");
                if (typeStr.isEmpty()) continue;

                Tag typedNode = entry.get("TypedStack");
                if (!(typedNode instanceof CompoundTag typed)) continue;

                KeyAmount ka;
                try
                {
                    // 交给KA统一解析
                    ka = KeyAmount.deserializeNBT(typed);
                }
                catch (Throwable t)
                {
                    continue;
                }
                acceptEntry(ka.key(), ka.amount());
            }
        }
    }

    /**
     * 将解码得到的 (key, amount) 写入当前结构（遵守 zeroPolicy 与 UI 时间策略）
     */
    protected void acceptEntry(IStackKey<?> key, long amount)
    {
        if (key == null || key.isEmpty()) return;

        if (uiTimestampPolicy == UiTimestampPolicy.AUTO)
        {
            long now = nowMillis();
            creationTimeMap.put(key, now);
            lastModifiedTimeMap.put(key, now);
        }

        if (amount <= 0L)
        {
            if (zeroPolicy == ZeroPolicy.KEEP_ZERO)
            {
                storage.put(key, 0L);
                ensureInIndex(key);
            }
            return;
        }

        insert(key, amount, false);
    }


    /* ---------------- 便捷设置 ---------------- */
    public void setSlotCapacity(long capacity)
    {
        this.slotCapacity = capacity;
        onChange();
    }

    public void setSlotMaxSize(int maxSize)
    {
        this.slotMaxSize = maxSize;
        onChange();
    }

    public boolean isFullSlotsSize()
    {
        return slotIndex.size() >= slotMaxSize;
    }
}
