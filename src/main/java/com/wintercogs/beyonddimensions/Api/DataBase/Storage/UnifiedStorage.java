package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.Custom.MatterCompressionBall;
import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.core.HolderLookup;
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


/**
 * 基于{@link IStackHandler}接口的无序存储实现。
 * <p>
 * 具有以下特点：
 * <ul>
 *     <li>每种精确匹配的堆叠类型仅允许占用一个槽位</li>
 *     <li>插入的堆叠会自动分配槽位</li>
 *     <li>实际用来存储的列表大小不实际反映最大槽位数量</li>
 * </ul>
 * <p>
 * 虽然需要一个DimensionsNet作为参数进行构造，但仅用于onChange方法通知数据保存。你可以复制代码，移除DimensionsNet参数，以获得一个可以自定义的无序存储实现
 */
public class UnifiedStorage implements IStackHandler
{

    /**
     * 对应的维度网络
     */
    private DimensionsNet net;

    // 内部存储-----------------------------------------------------------------------------
    /** 实际存储：key -> amount；约定：amount > 0；<=0 的键会被移除 */
    private final Map<IStackKey<?>, Long> storage = new HashMap<>();
    /** 非空键的紧凑槽位索引（换尾删除） */
    private final ArrayList<IStackKey<?>> slotIndex = new ArrayList<>();
    /** key -> 槽位位置（仅记录非空键） */
    private final Map<IStackKey<?>, Integer> posMap = new HashMap<>();
    // 分化包装
    /** key -> 具体存储物的对照（如ItemStackKey -> ItemStack），只维护类型，不维护数量，分化包装读取时手动设置数量 */
    private final Map<IStackKey<?>, Object> key2stackMap = new HashMap<>();
    /** typeId -> key列表的对照，使用换尾删除（不同类型对外界的维护表，如itemId配合此列表后用于IItemHandler的实现） */
    private final Map<ResourceLocation, TypeBucket> type2buckets = new HashMap<>();

    /** 只读、动态的 KeyAmount 视图（推荐给新界面使用） */
    private final List<KeyAmount> entriesView = Collections.unmodifiableList(
            new AbstractList<KeyAmount>() {
                @Override
                public KeyAmount get(int index) {
                    IStackKey<?> key = slotIndex.get(index); // 一次跳转
                    long amt = storage.getOrDefault(key, 0L); // 一次哈希+equals，是这里最重的操作，但是Key本身实现良好，应该不会有太多开销
                    return new KeyAmount(key, amt); // 一个轻量对象，包含一个引用和一个基本类型
                }
                @Override
                public int size() {
                    return slotIndex.size();
                }
            }
    );

    //onchange回调处理==================================================
    @FunctionalInterface // 带上下文版本
    public interface DeltaListener
    {
        void onDelta(IStackKey<?> key, long size, boolean insert);
    }

    @FunctionalInterface // 不带上下文版本
    public interface AnyChangeListener
    {
        void onAnyChange();
    }

    // 弱订阅用
    @FunctionalInterface
    public interface QuadConsumer<A,B,C,D> { void accept(A a, B b, C c, D d); }

    // ====== 弱 owner + 回调条目 ======
    private static final class OwnerRef extends WeakReference<Object>
    {
        OwnerRef(Object owner, ReferenceQueue<Object> q) { super(owner, q); }
    }
    // 无信息条目
    private static final class AnyEntry
    {
        final OwnerRef ownerRef;
        final AnyChangeListener listener; // 强回调，但内部请勿强握 owner
        AnyEntry(OwnerRef ref, AnyChangeListener l) { this.ownerRef = ref; this.listener = l; }
    }
    // 增量信息条目
    private static final class DeltaEntry
    {
        final OwnerRef ownerRef;
        final DeltaListener listener; // 强回调，但内部请勿强握 owner
        DeltaEntry(OwnerRef ref, DeltaListener l) { this.ownerRef = ref; this.listener = l; }
    }

    private final CopyOnWriteArrayList<AnyEntry> anyListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DeltaEntry> deltaListeners = new CopyOnWriteArrayList<>();

    /** 抑制 any 的“delta 上下文”嵌套计数；>0 时 fireChange() 将被忽略。 */
    private int deltaContextDepth = 0;

    /** 进入/退出 delta 上下文的小工具 */
    private void beginDeltaContext() { deltaContextDepth++; }
    private void endDeltaContext()   { deltaContextDepth = Math.max(0, deltaContextDepth - 1); }
    private boolean inDeltaContext() { return deltaContextDepth > 0; }

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    /**
     * 统一存储的属性 对于真正的存储实例，在序列化和反序列化的时候通过持久化和再赋值确定。
     * <p>
     * 对于临时数据，默认给予最大值
     */
    public long slotCapacity = Long.MAX_VALUE;
    public int slotMaxSize = Integer.MAX_VALUE;

    public UnifiedStorage(DimensionsNet net)
    {
        this.net = net;
    }

    private UnifiedStorage(long capacity, int maxSize)
    {
        this.slotCapacity = capacity;
        this.slotMaxSize = maxSize;
    }

    // 返回一个无法被插入和提取的空的UnifiedStorage，其可被安全的视为一个全空容器
    // 无需提供net，因为其覆写了需要net的方法
    public static UnifiedStorage getEmpty()
    {
        return new UnifiedStorage(0,0){
            @Override
            public void onChange()
            {

            }

            @Override
            public long getSlotCapacity(int slot)
            {
                return 0;
            }
        };
    }

    // ====== 订阅 API（强订阅）======
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

    // ====== 订阅 API（弱订阅，回调内部也只握弱引用）======
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
    public <T> AutoCloseable subscribeDeltaWeak(
            T owner,
            QuadConsumer<T, IStackKey<?>, Long, Boolean> onDelta
    ) {
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

    // 触发无上下文回调，如果本次更改正在触发上下回调则无视
    protected void fireChange()
    {
        if (inDeltaContext()) return;
        drainRefQueue();
        for (AnyEntry e : anyListeners) {
            try { e.listener.onAnyChange(); } catch (Throwable ignored) {}
        }
    }
    // 触发上下文回调
    protected void fireDelta(IStackKey<?> type, long size, boolean insert)
    {
        drainRefQueue();
        for (DeltaEntry e : deltaListeners) {
            try { e.listener.onDelta(type, size, insert); } catch (Throwable ignored) {}
        }
    }

    // 帮助GC
    private void drainRefQueue() {
        OwnerRef ref;
        while ((ref = (OwnerRef) refQueue.poll()) != null) {
            OwnerRef dead = ref;
            anyListeners.removeIf(e -> e.ownerRef == dead);
            deltaListeners.removeIf(e -> e.ownerRef == dead);
        }
    }

    @Override
    public void onChange()
    {
        net.setDirty();
        fireChange();
    }

    // 注：仅在只有一个内容变化的情况下触发带上下文信息的变化
    // 其触发时，会阻止无上下文信息的二次触发，要确保此信息所携带的内容，就是本次变化的全部内容
    // insert为真则为插入操作，否则为提取
    // size为变化量
    private void onContentChanged(IStackKey<?> type, long size, boolean insert)
    {
        beginDeltaContext();
        try {
            onChange(); // 走统一入口，但是当处于Delta上下文时，内部的fireChange被略过
        } finally {
            endDeltaContext();
        }
        fireDelta(type, size, insert); // 发送增量广播
    }

    @Override
    public List<KeyAmount> getStorage() {
        return entriesView; // 不可修改、动态视图
    }

    @Override
    public void clearStorage()
    {
        storage.clear();
        slotIndex.clear();
        posMap.clear();
        onChange();
    }

    @Override
    public @NotNull KeyAmount getStackBySlot(int slot)
    {
        if (slot < 0 || slot >= slotIndex.size()) return new KeyAmount(null, 0L);
        IStackKey<?> key = slotIndex.get(slot);
        return new KeyAmount(key, storage.getOrDefault(key, 0L));
    }

    @Override
    public @NotNull KeyAmount getStackByKey(IStackKey<?> key)
    {
        if (key == null) return new KeyAmount(null, 0L);
        return new KeyAmount(key, storage.getOrDefault(key, 0L));
    }

    @Override
    public boolean hasStack(IStackKey<?> key)
    {
        return key != null && storage.getOrDefault(key, 0L) > 0L;
    }

    /**
     * 辅助：根据键直接设置当前数量。
     * 语义：
     * - amount <= 0 ：移除该键（若存在）
     * - 否则：将其数量设置为 min(amount, slotCapacity)
     * 约束：
     * - 若该键当前不存在且需要新建槽位，但已达 slotMaxSize，则不修改并返回原数量（通常为 0）
     * 返回：最终存储的数量（long）
     */
    public long setAmountByKey(IStackKey<?> key, long amount) {
        if (key == null) return 0L;

        long current = storage.getOrDefault(key, 0L);
        long target  = Math.max(0L, Math.min(amount, slotCapacity));

        // 无变化，直接返回
        if (target == current) return current;

        // 目标为 0 -> 移除
        if (target == 0L) {
            if (current > 0L) {
                storage.remove(key);
                removeFromIndex(key);
                onContentChanged(key, current, /*insert=*/false);
            }
            return 0L;
        }

        // 需要从 0 -> 正数：可能要新建槽位
        if (current == 0L) {
            if (!posMap.containsKey(key) && slotIndex.size() >= slotMaxSize) {
                // 槽位不足：不做修改
                return current; // 一般为 0
            }
            storage.put(key, target);
            ensureInIndex(key);
            onContentChanged(key, target, /*insert=*/true);
            return target;
        }

        // 已存在且为正：更新为目标值
        storage.put(key, target);
        long delta = Math.abs(target - current);
        onContentChanged(key, delta, /*insert=*/target > current);
        return target;
    }

    /**
     * “整对替换”：把该槽位的旧 KV 移除，再把传入的 KV 写入。
     * 若 newKey 已存在于其他槽位，不会产生重复槽位，新键仍占其原有槽位（数量被覆盖为 target）。
     */
    @Override
    public void setStackDirectly(int slot, IStackKey<?> newKey, long amount)
    {
        if (slot < 0 || slot >= slotIndex.size()) return;

        IStackKey<?> oldKey = slotIndex.get(slot);
        long target = Math.max(0L, amount);

        // 新旧 key 相同：只更新数量或清空
        if (Objects.equals(oldKey, newKey)) {
            if (target == 0L) {
                storage.remove(oldKey);
                removeFromIndex(oldKey);
            } else {
                storage.put(oldKey, Math.min(target, slotCapacity));
            }
            onChange();
            return;
        }

        // 键不同，则替换KV对
        storage.remove(oldKey);
        removeFromIndex(oldKey);

        // 2) 写入新键（可能已存在或为空/0）
        if (newKey != null && target > 0L) {
            long clamped = Math.min(target, slotCapacity);
            storage.put(newKey, clamped);
            ensureInIndex(newKey); // 若已存在则 no-op；若不存在则新建槽位（此时不会超上限：我们刚刚释放了一个）
        }
        onChange();
    }

    /** 末尾添加：在虚拟容器里等价为“按 key 插入” */
    @Override
    public void addStackDirectly(IStackKey<?> key, long amount) {
        insert(key, amount, false);
    }

    /** 虚拟容器中，“按槽位插入”==“按 key 插入”（忽略 slot） */
    @Override
    public @NotNull KeyAmount insert(int slot, IStackKey<?> key, long amount, boolean simulate) {
        return insert(key, amount, simulate);
    }

    /** 按key插入 */
    // --- insert(key, amount, simulate)：带“物质压缩球”特殊处理 ---
    @Override
    public KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        if (key == null) return new KeyAmount(null, Math.max(0L, amount));
        long add = Math.max(0L, amount);
        if (add == 0L) return new KeyAmount(key, 0L);

        // 特判：物质压缩球（原子性操作，要么全解压，要么不解压）
        if (key instanceof ItemStackKey itemKey && itemKey.getSource() == ModItems.MATTER_COMPRESS_BALL.get()) {
            return unzipMatterBall(itemKey, add, simulate);
        }

        // 正常路径：按 key 自动合并 / 新建槽位
        long current = storage.getOrDefault(key, 0L);

        boolean needNewSlot = (current == 0L) && !posMap.containsKey(key);
        if (needNewSlot && slotIndex.size() >= slotMaxSize) {
            // 槽位不足：整量剩余
            return new KeyAmount(key, add);
        }

        long cap = slotCapacity;                     // 每键容量上限
        long room = cap <= current ? 0L : (cap - current);
        if (room <= 0L) {
            return new KeyAmount(key, add);         // 已达单键上限
        }

        long actual = Math.min(room, add);
        long leftover = add - actual;

        if (!simulate && actual > 0L) {
            storage.put(key, current + actual);
            ensureInIndex(key);
            onContentChanged(key, actual, true);
        }
        return new KeyAmount(key, leftover);
    }

    /**
     * 解压物质球逻辑：
     * - 若球内内容 * 球数 能完全插入，则：
     *   - simulate=true：返回 (ballKey, 0) 表示“不会有剩余”
     *   - simulate=false：实际将内容插入，并“消耗”这些球（返回 (ballKey, 0)）
     * - 否则：返回 (ballKey, 原球数)
     */
    /**
     * 解压物质球逻辑（修复：全或无 + 新槽位预估 + 原子性回滚）
     * - 若球内内容 * 球数 能完全插入（考虑到“需要新增的不同 key 个数”与“单键容量”），则：
     *   - simulate=true：返回 (ballKey, 0)
     *   - simulate=false：真实插入；若中途任何一条失败，立刻回滚已插入增量并返回 (ballKey, 原球数)
     * - 否则：返回 (ballKey, 原球数)
     */
    protected KeyAmount unzipMatterBall(ItemStackKey ballKey, long ballCount, boolean simulate)
    {
        ItemStack ballStack = ballKey.copyStackWithCount(ballCount);
        if (ballStack.isEmpty() || !(ballStack.getItem() instanceof MatterCompressionBall)) {
            return new KeyAmount(ballKey, ballCount);
        }

        List<KeyAmount> contents = ballStack.getOrDefault(ModDataComponents.ISTACK_SLOTS, new ArrayList<>());
        if (contents == null || contents.isEmpty()) {
            // 空球：直接“消耗球”
            return new KeyAmount(ballKey, 0L);
        }

        // ---------- 预检阶段：聚合同 key，总需求与新槽位数量校验 ----------
        // 聚合同 key 需求量：needMap[key] = sum(amountInBall) * ballCount
        final Map<IStackKey<?>, Long> needMap = new HashMap<>();
        try {
            for (KeyAmount entry : contents) {
                if (entry == null || entry.key() == null || entry.amount() <= 0L) continue;
                long scaled = Math.multiplyExact(entry.amount(), ballCount); // 溢出视为失败
                needMap.merge(entry.key(), scaled, Math::addExact);          // 聚合时也检测溢出
            }
        } catch (ArithmeticException overflow) {
            // 任何溢出都当作“放不下”
            return new KeyAmount(ballKey, ballCount);
        }

        // 统计需要新增的不同 key 个数，同时校验单键容量是否足够
        int freeSlots = Math.max(0, slotMaxSize - slotIndex.size());
        int newKeysNeeded = 0;
        for (Map.Entry<IStackKey<?>, Long> e : needMap.entrySet()) {
            IStackKey<?> k = e.getKey();
            long need = e.getValue();
            long current = storage.getOrDefault(k, 0L);

            boolean isNew = (current == 0L) && !posMap.containsKey(k);
            if (isNew) {
                newKeysNeeded++;
                if (newKeysNeeded > freeSlots) {
                    return new KeyAmount(ballKey, ballCount); // 新槽位不够
                }
            }
            long room = (slotCapacity <= current) ? 0L : (slotCapacity - current);
            if (need > room) {
                return new KeyAmount(ballKey, ballCount); // 单键容量不够
            }
        }

        // 到这里说明“严格预检通过”
        if (simulate) {
            return new KeyAmount(ballKey, 0L);
        }

        // ---------- 真实阶段：逐条插入 + 原子性回滚 ----------
        // 注意：为与球内数据一致，这里按原 contents 顺序插入（允许重复 key）
        final ArrayList<KeyAmount> applied = new ArrayList<>(); // 记录已成功插入的增量，用于失败回滚
        for (KeyAmount entry : contents) {
            if (entry == null || entry.key() == null || entry.amount() <= 0L) continue;
            long scaled;
            try {
                scaled = Math.multiplyExact(entry.amount(), ballCount);
            } catch (ArithmeticException overflow) {
                // 极端情况：此条溢出，按失败处理并回滚
                // 回滚
                for (int i = applied.size() - 1; i >= 0; i--) {
                    KeyAmount a = applied.get(i);
                    extract(a.key(), a.amount(), false);
                }
                return new KeyAmount(ballKey, ballCount);
            }

            KeyAmount leftover = insert(entry.key(), scaled, false);
            long appliedNow = scaled - leftover.amount();
            if (appliedNow > 0L) {
                applied.add(new KeyAmount(entry.key(), appliedNow));
            }
            if (leftover.amount() > 0L) {
                // 发生了不可预期的失败（理论上预检已保证不会失败），执行回滚
                for (int i = applied.size() - 1; i >= 0; i--) {
                    KeyAmount a = applied.get(i);
                    extract(a.key(), a.amount(), false);
                }
                return new KeyAmount(ballKey, ballCount);
            }
        }

        // 全部成功插入，消耗物质球
        return new KeyAmount(ballKey, 0L);
    }


    @Override
    public @NotNull KeyAmount extract(int slot, long count, boolean simulate)
    {
        if (slot < 0 || slot >= slotIndex.size() || count <= 0L) {
            return new KeyAmount(null, 0L);
        }
        IStackKey<?> key = slotIndex.get(slot);
        long current = storage.getOrDefault(key, 0L);
        if (current <= 0L) return new KeyAmount(key, 0L);

        long take = Math.min(count, current);
        if (!simulate) {
            long left = current - take;
            if (left == 0L) {
                storage.remove(key);
                removeFromIndex(key);
            } else {
                storage.put(key, left);
            }
            onContentChanged(key, take, false);
        }
        return new KeyAmount(key, take);
    }

    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate) {
        if (key == null || amount <= 0L) return new KeyAmount(null, 0L);
        long current = storage.getOrDefault(key, 0L);
        if (current <= 0L) return new KeyAmount(key, 0L);

        long take = Math.min(amount, current);
        if (!simulate) {
            long left = current - take;
            if (left == 0L) {
                storage.remove(key);
                removeFromIndex(key);
            } else {
                storage.put(key, left);
            }
            onContentChanged(key, take, false);
        }
        return new KeyAmount(key, take);
    }

    @Override
    public long getSlotCapacity(int slot) {
        return slotCapacity;
    }

    @Override
    public boolean isStackValid(int slot, IStackKey<?> key) {
        return true; // 虚拟容器：不做槽位白名单限制
    }

    @Override
    public boolean isEmpty() {
        // 虚拟容器：以索引是否为空为准
        return slotIndex.isEmpty();
    }

    /* ---------------- 索引维护：O(1) 换尾 ---------------- */

    // 确保index被加入
    private void ensureInIndex(IStackKey<?> key)
    {
        if (posMap.containsKey(key)) return;
        int idx = slotIndex.size();
        slotIndex.add(key);
        posMap.put(key, idx);
        bucketOf(key.getTypeId()).add(key);
        if(!key2stackMap.containsKey(key))
        {
            key2stackMap.put(key,key.copyStack());
        }
    }

    // 换尾删除
    private void removeFromIndex(IStackKey<?> key)
    {
        Integer pos = posMap.remove(key);
        if (pos == null) return;
        int last = slotIndex.size() - 1;
        if (pos != last) {
            IStackKey<?> tail = slotIndex.get(last);
            slotIndex.set(pos, tail);
            posMap.put(tail, pos);
        }
        slotIndex.remove(last);
        bucketOf(key.getTypeId()).remove(key); // 内部换尾
        key2stackMap.remove(key);
    }

    // region 序列化方法
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();

        tag.putLong("slotCapacity", this.slotCapacity);
        tag.putInt("slotMaxSize", this.slotMaxSize);

        ListTag stacksTag = new ListTag();

        for(Map.Entry<IStackKey<?>, Long> entry : storage.entrySet())
        {
            IStackKey<?> key = entry.getKey();
            long value = entry.getValue();
            if(key.isEmpty() || value <= 0L) continue;

            CompoundTag stackTag = new CompoundTag();
            stackTag.put("key", key.serializeNBT(provider));
            stackTag.putLong("amount", value);
            stacksTag.add(stackTag);
        }
        tag.put("stacks", stacksTag);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        clearStorage();

        // 读取容量/上限
        slotCapacity = tag.contains("slotCapacity", Tag.TAG_LONG) ? tag.getLong("slotCapacity") : Long.MAX_VALUE;
        slotMaxSize  = tag.contains("slotMaxSize",  Tag.TAG_INT)  ? tag.getInt("slotMaxSize")  : Integer.MAX_VALUE;

        // stacks: List<CompoundTag>，每个元素包含 { key: CompoundTag, amount: long }
        ListTag stacksTag = tag.getList("stacks", Tag.TAG_COMPOUND);

        // 使用带注册表上下文的 Ops，保证 CODEC 可解析注册表关联内容
        final DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);

        for (int i = 0; i < stacksTag.size(); i++)
        {
            Tag el = stacksTag.get(i);
            if (!(el instanceof CompoundTag stackTag)) continue;

            long amount = stackTag.getLong("amount");
            if (amount <= 0) continue;

            // 读取 key 子标签
            Tag rawKeyTag = stackTag.get("key");
            if (!(rawKeyTag instanceof CompoundTag keyTag)) continue;

            try
            {
                // 用 IStackKey.CODEC 解析 key（不再调用 deserializeNBTCommon）
                DataResult<IStackKey<?>> res = IStackKey.CODEC.parse(ops, keyTag);

                res.resultOrPartial(err -> {
                }).ifPresent(key -> {
                    if (!key.isEmpty()) {
                        insert(key, amount, false);
                    }
                });
            }
            catch (Throwable t)
            {
                // 记录日志，然后继续下一个解码
                BeyondDimensions.LOGGER.warn("解码第{}个堆叠时出错: {}", i, t.toString());
            }
        }
    }
    // endregion

    /**
     * 快速获取当前网络存储的FE能量总量，辅助方法
     */
    public long getEnergyStored()
    {
        // 即使网络内没有，也会返回一个带0L的keyAmount
        KeyAmount stack = getStackByKey(EnergyStackKey.INSTANCE);
        return stack.amount();
    }

    /**
     * 检查当前存储是否已经到达槽位数量的最大上限
     */
    public boolean isFullSlotsSize()
    {
        return storage.size() >= slotMaxSize;
    }

    // 理论上来说，以下两个数即使是负数，逻辑也能正常运行，所以没必要再额外检查了

    /**
     * 设置单个槽位可存储容量的上限
     */
    public void setSlotCapacity(long capacity)
    {
        this.slotCapacity = capacity;
    }

    /**
     * 设置槽位数量的上限
     */
    public void setSlotMaxSize(int maxSize)
    {
        this.slotMaxSize = maxSize;
    }

    // 每类型一个紧凑桶：keys + 反向位置表（换尾删除，O(1)）
    public static final class TypeBucket
    {
        final ArrayList<IStackKey<?>> keys = new ArrayList<>();
        final Map<IStackKey<?>, Integer> pos = new HashMap<>();

        void add(IStackKey<?> k) {
            if (pos.containsKey(k)) return;
            int i = keys.size();
            keys.add(k);
            pos.put(k, i);
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
        int size() { return keys.size(); }
        IStackKey<?> get(int i) { return keys.get(i); }
    }
    private TypeBucket bucketOf(ResourceLocation type)
    {
        return type2buckets.computeIfAbsent(type, t -> new TypeBucket());
    }

    /** 获取对应类型的桶视图 */
    public Optional<TypeBucket> getBucket(ResourceLocation type)
    {
        return Optional.ofNullable(type2buckets.get(type));
    }

    /** 根据key获取已经缓存的对应stack，自行判断类型，返回值的数量无法确定，根据keyAmount自己使用setCount，如果要缓存它，必须复制一个副本 */
    public Object getOutStackByKey(IStackKey<?> key)
    {
        return key2stackMap.get(key);
    }
}

