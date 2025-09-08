package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.Custom.MatterCompressionBall;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractUnorderedStackHandler implements IStackHandler {

    /* ---------- 策略：是否保留 amount==0 的键 ---------- */
    protected enum ZeroPolicy { KEEP_ZERO, REMOVE_ON_ZERO }

    private final ZeroPolicy zeroPolicy;

    protected AbstractUnorderedStackHandler(ZeroPolicy policy) {
        this.zeroPolicy = Objects.requireNonNull(policy);
    }

    /* ---------- 内部存储 ---------- */
    /** 存储：key -> amount；KeepZero 时允许 0；RemoveZero 时保证 >0 才存在 */
    protected final Map<IStackKey<?>, Long> storage = new HashMap<>();
    /** 非空键（以及 KeepZero 时可能包含 amount==0 的键）的紧凑槽位索引（换尾删除） */
    protected final ArrayList<IStackKey<?>> slotIndex = new ArrayList<>();
    /** key -> 槽位位置（仅记录存在于 slotIndex 的键） */
    protected final Map<IStackKey<?>, Integer> posMap = new HashMap<>();
    /** 分化包装：key -> 具体 stack 副本（只维护类型，不维护数量） */
    protected final Map<IStackKey<?>, Object> key2stackMap = new HashMap<>();
    /** typeId -> key 列表的对照（换尾删除） */
    protected final Map<ResourceLocation, TypeBucket> type2buckets = new HashMap<>();

    /* ---------- 只读、动态的 KeyAmount 视图 ---------- */
    private final List<KeyAmount> entriesView = Collections.unmodifiableList(
            new AbstractList<KeyAmount>() {
                @Override
                public KeyAmount get(int index) {
                    IStackKey<?> key = slotIndex.get(index);
                    long amt = storage.getOrDefault(key, 0L);
                    return new KeyAmount(key, amt);
                }
                @Override
                public int size() {
                    return slotIndex.size();
                }
            }
    );

    /* ---------- 订阅：强/弱 + 增量上下文 ---------- */
    @FunctionalInterface public interface DeltaListener { void onDelta(IStackKey<?> key, long size, boolean insert); }
    @FunctionalInterface public interface AnyChangeListener { void onAnyChange(); }
    @FunctionalInterface public interface QuadConsumer<A,B,C,D> { void accept(A a, B b, C c, D d); }

    private static final class OwnerRef extends WeakReference<Object>
    {
        OwnerRef(Object owner, ReferenceQueue<Object> q) { super(owner, q); }
    }
    private static final class AnyEntry {
        final OwnerRef ownerRef; final AnyChangeListener listener;
        AnyEntry(OwnerRef ref, AnyChangeListener l) { this.ownerRef = ref; this.listener = l; }
    }
    private static final class DeltaEntry {
        final OwnerRef ownerRef; final DeltaListener listener;
        DeltaEntry(OwnerRef ref, DeltaListener l) { this.ownerRef = ref; this.listener = l; }
    }

    private final CopyOnWriteArrayList<AnyEntry> anyListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DeltaEntry> deltaListeners = new CopyOnWriteArrayList<>();
    private int deltaContextDepth = 0;
    private void beginDeltaContext() { deltaContextDepth++; }
    private void endDeltaContext()   { deltaContextDepth = Math.max(0, deltaContextDepth - 1); }
    private boolean inDeltaContext() { return deltaContextDepth > 0; }
    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    /* ---------- 可配置容量/槽位上限 ---------- */
    public long slotCapacity = Long.MAX_VALUE;
    public int  slotMaxSize  = Integer.MAX_VALUE;

    /* ================= 公共订阅 API（放在抽象基类里） ================= */
    public AutoCloseable subscribeAny(Object owner, AnyChangeListener onAny) {
        if (owner == null || onAny == null) throw new IllegalArgumentException();
        drainRefQueue();
        AnyEntry e = new AnyEntry(new OwnerRef(owner, refQueue), onAny);
        anyListeners.add(e);
        return () -> anyListeners.remove(e);
    }
    public AutoCloseable subscribeDelta(Object owner, DeltaListener onDelta) {
        if (owner == null || onDelta == null) throw new IllegalArgumentException();
        drainRefQueue();
        DeltaEntry e = new DeltaEntry(new OwnerRef(owner, refQueue), onDelta);
        deltaListeners.add(e);
        return () -> deltaListeners.remove(e);
    }
    public <T> AutoCloseable subscribeAnyWeak(T owner, java.util.function.Consumer<T> onAny) {
        if (owner == null || onAny == null) throw new IllegalArgumentException();
        drainRefQueue();
        OwnerRef ref = new OwnerRef(owner, refQueue);
        AnyEntry e = new AnyEntry(ref, () -> {
            @SuppressWarnings("unchecked") T o = (T) ref.get();
            if (o != null) onAny.accept(o); else drainRefQueue();
        });
        anyListeners.add(e);
        return () -> anyListeners.remove(e);
    }
    public <T> AutoCloseable subscribeDeltaWeak(T owner, QuadConsumer<T, IStackKey<?>, Long, Boolean> onDelta) {
        if (owner == null || onDelta == null) throw new IllegalArgumentException();
        drainRefQueue();
        OwnerRef ref = new OwnerRef(owner, refQueue);
        DeltaEntry e = new DeltaEntry(ref, (type, size, insert) -> {
            @SuppressWarnings("unchecked") T o = (T) ref.get();
            if (o != null) onDelta.accept(o, type, size, insert); else drainRefQueue();
        });
        deltaListeners.add(e);
        return () -> deltaListeners.remove(e);
    }

    protected void fireChange() {
        if (inDeltaContext()) return;
        drainRefQueue();
        for (AnyEntry e : anyListeners) {
            try { e.listener.onAnyChange(); } catch (Throwable ignored) {}
        }
    }
    protected void fireDelta(IStackKey<?> type, long size, boolean insert) {
        drainRefQueue();
        for (DeltaEntry e : deltaListeners) {
            try { e.listener.onDelta(type, size, insert); } catch (Throwable ignored) {}
        }
    }
    private void drainRefQueue() {
        OwnerRef ref;
        while ((ref = (OwnerRef) refQueue.poll()) != null) {
            OwnerRef dead = ref;
            anyListeners.removeIf(e -> e.ownerRef == dead);
            deltaListeners.removeIf(e -> e.ownerRef == dead);
        }
    }

    /* ============== 生命周期：留给子类覆写的统一入口 ============== */
    /** 统一触发点：子类可覆写添加自定义行为（例如 UnifiedStorage 中的 net.setDirty()）。 */
    @Override
    public void onChange() {
        fireChange();
    }

    protected final void onContentChanged(IStackKey<?> type, long size, boolean insert) {
        beginDeltaContext();
        try { onChange(); } finally { endDeltaContext(); }
        fireDelta(type, size, insert);
    }

    /* ================= IStackHandler 实现（通用） ================= */
    @Override public List<KeyAmount> getStorage() { return entriesView; }

    @Override
    public void clearStorage() {
        storage.clear();
        slotIndex.clear();
        posMap.clear();
        key2stackMap.clear();
        type2buckets.clear();
        onChange();
    }

    @Override
    public @NotNull KeyAmount getStackBySlot(int slot) {
        if (slot < 0 || slot >= slotIndex.size()) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        IStackKey<?> key = slotIndex.get(slot);
        return new KeyAmount(key, storage.getOrDefault(key, 0L));
    }

    @Override
    public @NotNull KeyAmount getStackByKey(IStackKey<?> key) {
        if (key == null) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        return new KeyAmount(key, storage.getOrDefault(key, 0L));
    }

    @Override
    public boolean hasStack(IStackKey<?> key) {
        return key != null && storage.getOrDefault(key, 0L) > 0L;
    }

    /**
     * 语义：
     * - amount <= 0 ：KEEP_ZERO => 置 0 且保留索引；REMOVE_ON_ZERO => 移除
     * - 否则：将其数量设置为 min(amount, slotCapacity)，必要时建立槽位（看上限）
     * 返回：最终存储的数量
     */
    public long setAmountByKey(IStackKey<?> key, long amount) {
        if (key == null) return 0L;

        long current = storage.getOrDefault(key, 0L);
        long target  = Math.max(0L, Math.min(amount, slotCapacity));
        if (target == current) return current;

        if (target == 0L) {
            if (zeroPolicy == ZeroPolicy.REMOVE_ON_ZERO) {
                if (current > 0L || posMap.containsKey(key)) {
                    storage.remove(key);
                    removeFromIndex(key);
                    onContentChanged(key, current, false);
                }
                return 0L;
            } else { // KEEP_ZERO
                storage.put(key, 0L);
                ensureInIndex(key);
                long delta = current; // 全部减少
                if (delta > 0L) onContentChanged(key, delta, false);
                return 0L;
            }
        }

        // target > 0
        boolean isNew = (current == 0L) && !posMap.containsKey(key);
        if (isNew && slotIndex.size() >= slotMaxSize) {
            // 槽位不足：对 KEEP_ZERO 如果当前已有 0（posMap.contains 为 false/true?）已处理在 isNew 里
            return current;
        }
        storage.put(key, target);
        ensureInIndex(key);
        long delta = Math.abs(target - current);
        if (delta > 0L) onContentChanged(key, delta, target > current);
        return target;
    }

    @Override
    public void setStackDirectly(int slot, IStackKey<?> newKey, long amount) {
        if (slot < 0 || slot >= slotIndex.size()) return;

        IStackKey<?> oldKey = slotIndex.get(slot);
        long target = Math.max(0L, amount);

        if (Objects.equals(oldKey, newKey)) {
            // 修改老键的数量/或清空
            setAmountByKey(oldKey, target);
            return;
        }

        // 移除旧
        long oldAmt = storage.getOrDefault(oldKey, 0L);
        if (zeroPolicy == ZeroPolicy.REMOVE_ON_ZERO) {
            storage.remove(oldKey);
            removeFromIndex(oldKey);
        } else {
            storage.put(oldKey, 0L);
            // 保留索引（不调用 removeFromIndex）
        }
        if (oldAmt > 0L) onContentChanged(oldKey, oldAmt, false);

        // 写入新
        if (newKey != null) setAmountByKey(newKey, target);
    }

    @Override public void addStackDirectly(IStackKey<?> key, long amount) { insert(key, amount, false); }
    @Override public @NotNull KeyAmount insert(int slot, IStackKey<?> key, long amount, boolean simulate) {
        return insert(key, amount, simulate);
    }

    @Override
    public KeyAmount insert(IStackKey<?> key, long amount, boolean simulate) {
        if (key == null) return new KeyAmount(EmptyStackKey.INSTANCE, Math.max(0L, amount));
        long add = Math.max(0L, amount);
        if (add == 0L) return new KeyAmount(key, 0L);

        // 物质压缩球：全或无
        if (key instanceof ItemStackKey itemKey && itemKey.getSource() == ModItems.MATTER_COMPRESS_BALL.get()) {
            return unzipMatterBall(itemKey, add, simulate);
        }

        long current = storage.getOrDefault(key, 0L);
        boolean needNewSlot = (current == 0L) && !posMap.containsKey(key);
        if (needNewSlot && slotIndex.size() >= slotMaxSize) {
            return new KeyAmount(key, add);
        }

        long cap = slotCapacity;
        long room = cap <= current ? 0L : (cap - current);
        if (room <= 0L) return new KeyAmount(key, add);

        long actual = Math.min(room, add);
        long leftover = add - actual;

        if (!simulate && actual > 0L) {
            storage.put(key, current + actual);
            ensureInIndex(key);
            onContentChanged(key, actual, true);
        }
        return new KeyAmount(key, leftover);
    }

    protected KeyAmount unzipMatterBall(ItemStackKey ballKey, long ballCount, boolean simulate) {
        ItemStack ballStack = ballKey.copyStackWithCount(ballCount);
        if (ballStack.isEmpty() || !(ballStack.getItem() instanceof MatterCompressionBall)) {
            return new KeyAmount(ballKey, ballCount);
        }
        List<KeyAmount> contents = ballStack.getOrDefault(ModDataComponents.ISTACK_SLOTS, new ArrayList<>());
        if (contents == null || contents.isEmpty()) return new KeyAmount(ballKey, 0L);

        // 预检：聚合同 key
        final Map<IStackKey<?>, Long> needMap = new HashMap<>();
        try {
            for (KeyAmount entry : contents) {
                if (entry == null || entry.key() == null || entry.amount() <= 0L) continue;
                long scaled = Math.multiplyExact(entry.amount(), ballCount);
                needMap.merge(entry.key(), scaled, Math::addExact);
            }
        } catch (ArithmeticException e) {
            return new KeyAmount(ballKey, ballCount);
        }

        int freeSlots = Math.max(0, slotMaxSize - slotIndex.size());
        int newKeysNeeded = 0;
        for (Map.Entry<IStackKey<?>, Long> e : needMap.entrySet()) {
            IStackKey<?> k = e.getKey();
            long need = e.getValue();
            long current = storage.getOrDefault(k, 0L);
            boolean isNew = (current == 0L) && !posMap.containsKey(k);
            if (isNew && ++newKeysNeeded > freeSlots) return new KeyAmount(ballKey, ballCount);
            long room = (slotCapacity <= current) ? 0L : (slotCapacity - current);
            if (need > room) return new KeyAmount(ballKey, ballCount);
        }

        if (simulate) return new KeyAmount(ballKey, 0L);

        // 真实阶段 + 回滚
        final ArrayList<KeyAmount> applied = new ArrayList<>();
        for (KeyAmount entry : contents) {
            if (entry == null || entry.key() == null || entry.amount() <= 0L) continue;
            long scaled;
            try { scaled = Math.multiplyExact(entry.amount(), ballCount); }
            catch (ArithmeticException e) {
                // 回滚
                for (int i = applied.size()-1; i>=0; i--) {
                    KeyAmount a = applied.get(i);
                    extract(a.key(), a.amount(), false);
                }
                return new KeyAmount(ballKey, ballCount);
            }
            KeyAmount leftover = insert(entry.key(), scaled, false);
            long ok = scaled - leftover.amount();
            if (ok > 0L) applied.add(new KeyAmount(entry.key(), ok));
            if (leftover.amount() > 0L) {
                for (int i = applied.size()-1; i>=0; i--) {
                    KeyAmount a = applied.get(i);
                    extract(a.key(), a.amount(), false);
                }
                return new KeyAmount(ballKey, ballCount);
            }
        }
        return new KeyAmount(ballKey, 0L);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long count, boolean simulate) {
        if (slot < 0 || slot >= slotIndex.size() || count <= 0L) {
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        }
        IStackKey<?> key = slotIndex.get(slot);
        long current = storage.getOrDefault(key, 0L);
        if (current <= 0L) return new KeyAmount(key, 0L);

        long take = Math.min(count, current);
        if (!simulate) {
            long left = current - take;
            if (left == 0L) {
                if (zeroPolicy == ZeroPolicy.REMOVE_ON_ZERO) {
                    storage.remove(key);
                    removeFromIndex(key);
                } else {
                    storage.put(key, 0L);
                    ensureInIndex(key);
                }
            } else {
                storage.put(key, left);
            }
            onContentChanged(key, take, false);
        }
        return new KeyAmount(key, take);
    }

    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate) {
        if (key == null || amount <= 0L) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        long current = storage.getOrDefault(key, 0L);
        if (current <= 0L) return new KeyAmount(key, 0L);

        long take = Math.min(amount, current);
        if (!simulate) {
            long left = current - take;
            if (left == 0L) {
                if (zeroPolicy == ZeroPolicy.REMOVE_ON_ZERO) {
                    storage.remove(key);
                    removeFromIndex(key);
                } else {
                    storage.put(key, 0L);
                    ensureInIndex(key);
                }
            } else {
                storage.put(key, left);
            }
            onContentChanged(key, take, false);
        }
        return new KeyAmount(key, take);
    }

    @Override public long  getSlotCapacity(int slot) { return slotCapacity; }
    @Override public boolean isStackValid(int slot, IStackKey<?> key) { return true; }
    @Override public boolean isEmpty() { return slotIndex.isEmpty(); }

    /* ---------------- 索引维护：O(1) 换尾 ---------------- */
    protected void ensureInIndex(IStackKey<?> key) {
        if (posMap.containsKey(key)) return;
        int idx = slotIndex.size();
        slotIndex.add(key);
        posMap.put(key, idx);
        bucketOf(key.getTypeId()).add(key);
        if (!key2stackMap.containsKey(key)) key2stackMap.put(key, key.copyStack());
    }
    protected void removeFromIndex(IStackKey<?> key) {
        Integer pos = posMap.remove(key);
        if (pos == null) return;
        int last = slotIndex.size() - 1;
        if (pos != last) {
            IStackKey<?> tail = slotIndex.get(last);
            slotIndex.set(pos, tail);
            posMap.put(tail, pos);
        }
        slotIndex.remove(last);
        bucketOf(key.getTypeId()).remove(key);
        key2stackMap.remove(key);
    }

    /* ---------------- 类型桶 ---------------- */
    public static final class TypeBucket {
        final ArrayList<IStackKey<?>> keys = new ArrayList<>();
        final Map<IStackKey<?>, Integer> pos = new HashMap<>();
        void add(IStackKey<?> k) {
            if (pos.containsKey(k)) return;
            int i = keys.size(); keys.add(k); pos.put(k, i);
        }
        void remove(IStackKey<?> k) {
            Integer p = pos.remove(k);
            if (p == null) return;
            int last = keys.size() - 1;
            if (p != last) {
                IStackKey<?> tail = keys.get(last);
                keys.set(p, tail);
                pos.put(tail, p);
            }
            keys.remove(last);
        }
        public int size() { return keys.size(); }
        public IStackKey<?> get(int i) { return keys.get(i); }
    }
    protected TypeBucket bucketOf(ResourceLocation type) {
        return type2buckets.computeIfAbsent(type, t -> new TypeBucket());
    }
    public Optional<TypeBucket> getBucket(ResourceLocation type) {
        return Optional.ofNullable(type2buckets.get(type));
    }
    public Object getOutStackByKey(IStackKey<?> key) {
        return key2stackMap.get(key);
    }

    /* ---------------- NBT 序列化（按策略处理 0） ---------------- */
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("slotCapacity", this.slotCapacity);
        tag.putInt("slotMaxSize", this.slotMaxSize);

        ListTag stacksTag = new ListTag();
        boolean writeZero = (zeroPolicy == ZeroPolicy.KEEP_ZERO);

        // 与反序列化保持一致：使用带注册表上下文的 Ops
        final DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);

        for (Map.Entry<IStackKey<?>, Long> entry : storage.entrySet()) {
            IStackKey<?> key = entry.getKey();
            long value = entry.getValue();
            if (key == null || key.isEmpty()) continue;
            if (!writeZero && value <= 0L) continue;

            // 用 CODEC 编码 key -> Tag（期望是 CompoundTag）
            DataResult<Tag> enc = IStackKey.CODEC.encodeStart(ops, key);

            Tag encoded = enc.resultOrPartial(err -> {
                // 记录一次警告，但不中断后续条目
                BeyondDimensions.LOGGER.warn("编码 IStackKey 失败：{} | key={}", err, key);
            }).orElse(null);

            if (!(encoded instanceof CompoundTag)) {
                // 理论上不会发生；若发生，为保证与解码端一致（那里要求 CompoundTag），这里跳过该条目
                if (encoded != null) {
                    BeyondDimensions.LOGGER.warn("IStackKey 编码结果不是 CompoundTag：{} | key={}", encoded.getClass().getName(), key);
                }
                continue;
            }

            CompoundTag stackTag = new CompoundTag();
            stackTag.put("key", (CompoundTag) encoded);
            stackTag.putLong("amount", value);
            stacksTag.add(stackTag);
        }

        tag.put("stacks", stacksTag);
        return tag;
    }

    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        clearStorage();

        slotCapacity = tag.contains("slotCapacity", Tag.TAG_LONG) ? tag.getLong("slotCapacity") : Long.MAX_VALUE;
        slotMaxSize  = tag.contains("slotMaxSize",  Tag.TAG_INT)  ? tag.getInt("slotMaxSize")  : Integer.MAX_VALUE;

        ListTag stacksTag = tag.getList("stacks", Tag.TAG_COMPOUND);
        final DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);

        for (int i = 0; i < stacksTag.size(); i++) {
            Tag el = stacksTag.get(i);
            if (!(el instanceof CompoundTag stackTag)) continue;

            long amount = stackTag.getLong("amount");
            Tag rawKeyTag = stackTag.get("key");
            if (!(rawKeyTag instanceof CompoundTag keyTag)) continue;

            try {
                DataResult<IStackKey<?>> res = IStackKey.CODEC.parse(ops, keyTag);
                res.resultOrPartial(err -> {
                    BeyondDimensions.LOGGER.warn("解码 IStackKey 失败，原因：{}", err);
                }).ifPresent(key -> {
                    if (key.isEmpty()) return;
                    if (amount <= 0L) {
                        if (zeroPolicy == ZeroPolicy.KEEP_ZERO) {
                            storage.put(key, 0L);
                            ensureInIndex(key);
                        }
                    } else {
                        // 正数：正常插入（不模拟，触发事件）
                        insert(key, amount, false);
                    }
                });
            } catch (Throwable t) {
                BeyondDimensions.LOGGER.warn("解码第{}个堆叠时出错: {}", i, t.toString());
            }
        }
    }

    /* ---------------- 便捷设置 ---------------- */
    public void setSlotCapacity(long capacity) { this.slotCapacity = capacity; }
    public void setSlotMaxSize(int maxSize)    { this.slotMaxSize  = maxSize; }

    /** 是否达到槽位数量上限 */
    public boolean isFullSlotsSize() { return slotIndex.size() >= slotMaxSize; }
}
