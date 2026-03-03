package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 有序、固定槽位的堆叠容器实现（箱子类）。
 * - 槽位数固定，允许同一 Key 占用多个槽位（与虚拟容器相反）
 * - 采用数组存储 + 多级索引（类型桶/精确 Key 桶 + 换尾删除） + BitSet 空槽位快速定位
 * - insert(key, ...) 优先合并已有同 Key 的槽位，再填充空槽
 * - insert(slot, ...) 仅对指定槽位尝试
 */
public class StackHandler implements IStackHandler
{

    private static final MapCodec<StackHandler> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    KeyAmount.CODEC.listOf().fieldOf("stacks")
                            .forGetter(sh -> {
                                ArrayList<KeyAmount> list = new ArrayList<>(sh.size);
                                for (int i = 0; i < sh.size; i++)
                                {
                                    list.add(new KeyAmount(sh.keys[i], sh.amounts[i]));
                                }
                                return list;
                            })
            ).apply(instance, StackHandler::new)
    );

    public static final Codec<StackHandler> CODEC = TYPE_CODEC.codec();

    /* ================= 基本存储（固定大小） ================= */

    private final int size;

    /**
     * 槽位上的 Key（EmptyStackKey.INSTANCE 代表空，不使用 null）
     */
    private final IStackKey<?>[] keys;

    /**
     * 槽位上的数量（空槽位必须为 0，与 keys 同步）
     */
    private final long[] amounts;

    /**
     * key -> 具体存储物的对照（只维护类型，不维护数量；不缓存 EmptyStackKey）
     */
    private final Map<IStackKey<?>, Object> key2stackMap = new HashMap<>();

    /* ================= 只读视图 ================= */

    private final List<KeyAmount> entriesView = Collections.unmodifiableList(new AbstractList<>()
    {
        @Override
        public KeyAmount get(int index)
        {
            if (index < 0 || index >= size) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
            IStackKey<?> k = keys[index];
            long amt = (k == EmptyStackKey.INSTANCE) ? 0L : amounts[index];
            return new KeyAmount(k, amt);
        }

        @Override
        public int size()
        {
            return size;
        }
    });

    /* ================= 索引（类型桶 / 精确 Key 桶 / 空桶） ================= */

    public static final class SlotBucket
    {
        final ArrayList<Integer> slots = new ArrayList<>();
        final HashMap<Integer, Integer> pos = new HashMap<>(); // slot -> index in slots

        void add(int slot)
        {
            if (pos.containsKey(slot)) return;
            int i = slots.size();
            slots.add(slot);
            pos.put(slot, i);
        }

        void remove(int slot)
        {
            Integer p = pos.remove(slot);
            if (p == null) return;
            int last = slots.size() - 1;
            if (p != last)
            {
                int tail = slots.get(last);
                slots.set(p, tail);
                pos.put(tail, p);
            }
            slots.remove(last);
        }

        public int size()
        {
            return slots.size();
        }

        public int get(int i)
        {
            return slots.get(i);
        }

        List<Integer> snapshot()
        {
            return new ArrayList<>(slots);
        } // 防御性副本
    }

    // typeId -> 该类型下所有非空槽位（按换尾维护）【不包含 EmptyStackKey】
    private final Map<Identifier, SlotBucket> typeBuckets = new HashMap<>();

    private SlotBucket bucketOf(Identifier typeId)
    {
        return typeBuckets.computeIfAbsent(typeId, t -> new SlotBucket());
    }

    public Optional<SlotBucket> getBucket(Identifier typeId)
    {
        return Optional.ofNullable(typeBuckets.get(typeId));
    }

    // 精确 Key -> 该 Key 占用的所有槽位（允许同 Key 多槽）【包含 EmptyStackKey，一个“空桶”】
    private final Map<IStackKey<?>, SlotBucket> keyBuckets = new HashMap<>();

    private SlotBucket bucketOf(IStackKey<?> key)
    {
        return keyBuckets.computeIfAbsent(key, k -> new SlotBucket());
    }

    public Optional<SlotBucket> getBucket(IStackKey<?> key)
    {
        return Optional.ofNullable(keyBuckets.get(key));
    }

    /* ================= 构造 ================= */

    public StackHandler(int size)
    {
        this.size = Math.max(0, size);
        this.keys = new IStackKey<?>[this.size];
        this.amounts = new long[this.size];

        // 初始全部置空：使用 EmptyStackKey.INSTANCE，不使用 null
        Arrays.fill(this.keys, EmptyStackKey.INSTANCE);
        Arrays.fill(this.amounts, 0L);

        // ★ 初始化“空桶”：所有槽都是空
        SlotBucket eb = bucketOf(EmptyStackKey.INSTANCE);
        for (int i = 0; i < this.size; i++)
        {
            eb.add(i);
        }
    }

    public StackHandler(List<KeyAmount> stacks)
    {
        this(stacks.size());
        for (int i = 0; i < this.size; i++)
        {
            KeyAmount ka = stacks.get(i);
            if (ka != null)
            {
                setStackDirectly(i, ka.key(), ka.amount());
            }
            else
            {
                setStackDirectly(i, EmptyStackKey.INSTANCE, 0);
            }
        }
    }

    /* ================= IStackHandler 实现 ================= */

    @Override
    public List<KeyAmount> getStorage()
    {
        return entriesView;
    }

    @Override
    public void onChange()
    { /* 根据需要覆写（保存/脏标记/事件） */ }

    @Override
    public int getSlots()
    {
        return size;
    }

    @Override
    public void clearStorage()
    {
        // 清空数组（用 EmptyStackKey.INSTANCE 作为空标记）
        Arrays.fill(keys, EmptyStackKey.INSTANCE);
        Arrays.fill(amounts, 0L);

        // 清空索引
        typeBuckets.clear();
        keyBuckets.clear();

        // 清空外部类型缓存
        key2stackMap.clear();

        // ★ 重建空桶
        SlotBucket eb = bucketOf(EmptyStackKey.INSTANCE);
        for (int i = 0; i < size; i++)
        {
            eb.add(i);
        }
        onChange();
    }

    @Override
    public @NotNull KeyAmount getStackBySlot(int slot)
    {
        if (slot < 0 || slot >= size) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        IStackKey<?> k = keys[slot];
        return new KeyAmount(k, (k == EmptyStackKey.INSTANCE) ? 0L : amounts[slot]);
    }

    @Override
    public @NotNull KeyAmount getStackByKey(IStackKey<?> key)
    {
        if (key == null || key == EmptyStackKey.INSTANCE) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        SlotBucket b = keyBuckets.get(key);
        if (b == null || b.size() == 0) return new KeyAmount(key, 0L);
        int slot = b.get(0); // “第一个找到的堆叠”
        return new KeyAmount(key, amounts[slot]);
    }

    @Override
    public boolean hasStack(IStackKey<?> key)
    {
        if (key == null || key == EmptyStackKey.INSTANCE) return false;
        SlotBucket b = keyBuckets.get(key);
        return b != null && b.size() > 0;
    }

    @Override
    public void setStackDirectly(int slot, IStackKey<?> key, long amount)
    {
        if (slot < 0 || slot >= size) return;

        // 先移除旧的槽位映射（谨慎：用 get()，不创建新桶）
        IStackKey<?> oldKey = keys[slot];
        if (oldKey == EmptyStackKey.INSTANCE)
        {
            // 从空桶移除
            SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
            if (eb != null) eb.remove(slot);
        }
        else
        {
            SlotBucket tb = typeBuckets.get(oldKey.getTypeId());
            if (tb != null)
            {
                tb.remove(slot);
                if (tb.size() == 0) typeBuckets.remove(oldKey.getTypeId());
            }
            SlotBucket kb = keyBuckets.get(oldKey);
            if (kb != null)
            {
                kb.remove(slot);
                if (kb.size() == 0) keyBuckets.remove(oldKey);
            }
        }

        // 空/非正数 -> 置空（统一使用 EmptyStackKey.INSTANCE）
        if (key == null || key == EmptyStackKey.INSTANCE || amount <= 0L)
        {
            keys[slot] = EmptyStackKey.INSTANCE;
            amounts[slot] = 0L;

            // 加入空桶
            bucketOf(EmptyStackKey.INSTANCE).add(slot);

            // 仅当容器已无旧键时，才移除外部缓存
            removeFromIndex(oldKey);

            onChange();
            return;
        }

        long clamped = Math.max(0L, Math.min(amount, getSlotCapacity(slot)));
        if (clamped == 0L)
        {
            keys[slot] = EmptyStackKey.INSTANCE;
            amounts[slot] = 0L;

            // ★ 加入空桶
            bucketOf(EmptyStackKey.INSTANCE).add(slot);

            removeFromIndex(oldKey);

            onChange();
            return;
        }

        // 写入新键
        keys[slot] = key;
        amounts[slot] = clamped;

        // ★ 从空桶移除
        SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
        if (eb != null) eb.remove(slot);

        // 建立新映射（仅非空键）
        bucketOf(key.getTypeId()).add(slot);
        bucketOf(key).add(slot);
        ensureInIndex(key);

        // ★ 如果换键，且旧键已无任何槽位占用，则移除缓存
        removeFromIndex(oldKey);

        onChange();
    }

    @Override
    public void addStackDirectly(IStackKey<?> key, long amount)
    {
        if (key == null || key == EmptyStackKey.INSTANCE || amount <= 0L) return;
        SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
        if (eb == null || eb.size() == 0) return; // 没空位
        int empty = eb.get(0); // 放入第一个空位
        setStackDirectly(empty, key, amount);
    }

    @Override
    public @NotNull KeyAmount insert(int slot, IStackKey<?> key, long amount, boolean simulate)
    {
        if (key == null || key == EmptyStackKey.INSTANCE || amount <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        if (slot < 0 || slot >= size) return new KeyAmount(key, amount);
        if (!isStackValid(slot, key)) return new KeyAmount(key, amount);

        long left = amount;

        IStackKey<?> curKey = keys[slot];
        if (curKey == EmptyStackKey.INSTANCE)
        {
            long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(slot));
            long ins = Math.min(left, cap);
            if (ins <= 0) return new KeyAmount(key, left);
            if (!simulate)
            {
                keys[slot] = key;
                amounts[slot] = ins;

                // ★ 从空桶移除
                SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
                if (eb != null) eb.remove(slot);

                bucketOf(key.getTypeId()).add(slot);
                bucketOf(key).add(slot);
                ensureInIndex(key);
                onChange();
            }
            left -= ins;
            return new KeyAmount(key, left);
        }

        // 非空：类型必须相同（完全相同的 Key）
        if (!curKey.equals(key))
        {
            return new KeyAmount(key, left);
        }

        long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(slot));
        long room = Math.max(0L, cap - amounts[slot]);
        long ins = Math.min(left, room);
        if (ins <= 0) return new KeyAmount(key, left);
        if (!simulate)
        {
            amounts[slot] += ins;
            onChange();
        }
        left -= ins;
        return new KeyAmount(key, left);
    }

    @Override
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        if (key == null || key == EmptyStackKey.INSTANCE || amount <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);

        long left = amount;

        // 第一阶段：合并已有同 Key 的槽位
        SlotBucket exact = keyBuckets.get(key);
        if (exact != null && exact.size() > 0)
        {
            List<Integer> slots = exact.snapshot(); // 快照
            for (int slot : slots)
            {
                if (left <= 0) break;
                long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(slot));
                long room = Math.max(0L, cap - amounts[slot]);
                if (room <= 0) continue;
                long ins = Math.min(left, room);
                if (!simulate)
                {
                    amounts[slot] += ins;
                }
                left -= ins;
            }
        }

        // 第二阶段：填充空槽位（从空桶拿候选）
        if (left > 0)
        {
            SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
            if (eb != null && eb.size() > 0)
            {
                List<Integer> slots = eb.snapshot(); // 快照
                for (int idx : slots)
                {
                    if (left <= 0) break;
                    if (!isStackValid(idx, key)) continue;

                    long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(idx));
                    long ins = Math.min(left, cap);
                    if (ins > 0)
                    {
                        if (!simulate)
                        {
                            keys[idx] = key;
                            amounts[idx] = ins;

                            // ★ 从空桶移除
                            eb.remove(idx);

                            bucketOf(key.getTypeId()).add(idx);
                            bucketOf(key).add(idx);
                            ensureInIndex(key);
                        }
                        left -= ins;
                    }
                }
            }
        }

        if (!simulate && left != amount)
        {
            onChange(); // 本次按类型插入可能影响多个槽位，只发一次变更
        }

        return new KeyAmount(key, left);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long count, boolean simulate)
    {
        if (slot < 0 || slot >= size || count <= 0L) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        IStackKey<?> k = keys[slot];
        if (k == EmptyStackKey.INSTANCE) return new KeyAmount(EmptyStackKey.INSTANCE, 0L);

        long have = amounts[slot];
        long take = Math.min(count, have);
        if (take <= 0) return new KeyAmount(k, 0L);

        if (!simulate)
        {
            long left = have - take;
            if (left == 0L)
            {
                // 置空并维护索引
                SlotBucket tb = typeBuckets.get(k.getTypeId());
                if (tb != null)
                {
                    tb.remove(slot);
                    if (tb.size() == 0) typeBuckets.remove(k.getTypeId());
                }
                SlotBucket kb = keyBuckets.get(k);
                if (kb != null)
                {
                    kb.remove(slot);
                    if (kb.size() == 0) keyBuckets.remove(k);
                }
                keys[slot] = EmptyStackKey.INSTANCE;
                amounts[slot] = 0L;

                // 加入空桶
                bucketOf(EmptyStackKey.INSTANCE).add(slot);

                // 仅当容器已无该键时才移除缓存
                removeFromIndex(k);
            }
            else
            {
                amounts[slot] = left;
            }
            onChange();
        }
        return new KeyAmount(k, take);
    }

    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate, boolean fuzzy)
    {
        var realKey = key;
        if (fuzzy)
        {
            realKey = keyBuckets.keySet().stream()
                    .filter(x -> x.isSame(key))
                    .findFirst().orElse(null);
        }
        if (realKey == null || realKey == EmptyStackKey.INSTANCE || amount <= 0L)
        {
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);
        }
        SlotBucket exact = keyBuckets.get(realKey);
        if (exact == null || exact.size() == 0) return new KeyAmount(realKey, 0L);

        long need = amount;
        long taken = 0L;

        // 快照防止遍历期间结构改变
        List<Integer> slots = exact.snapshot();
        for (int slot : slots)
        {
            if (need <= 0) break;
            long have = amounts[slot];
            if (have <= 0) continue;
            long t = Math.min(need, have);

            if (!simulate)
            {
                long left = have - t;
                if (left == 0L)
                {
                    // 删除槽位映射
                    SlotBucket tb = typeBuckets.get(realKey.getTypeId());
                    if (tb != null)
                    {
                        tb.remove(slot);
                        if (tb.size() == 0) typeBuckets.remove(realKey.getTypeId());
                    }
                    SlotBucket kb = keyBuckets.get(realKey);
                    if (kb != null)
                    {
                        kb.remove(slot);
                        if (kb.size() == 0) keyBuckets.remove(realKey);
                    }
                    keys[slot] = EmptyStackKey.INSTANCE;
                    amounts[slot] = 0L;

                    // ★ 加入空桶
                    bucketOf(EmptyStackKey.INSTANCE).add(slot);

                    // ★ 仅当容器已无该键时才移除缓存
                    removeFromIndex(realKey);
                }
                else
                {
                    amounts[slot] = left;
                }
            }
            taken += t;
            need -= t;
        }

        if (!simulate && taken > 0) onChange();
        return new KeyAmount(realKey, taken);
    }

    @Override
    public long getSlotCapacity(int slot)
    {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isStackValid(int slot, IStackKey<?> key)
    {
        return key != null; // 如需白名单/黑名单，覆写或改成策略；空键在外层已过滤
    }

    @Override
    public boolean isEmpty()
    {
        SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
        return eb != null && eb.size() == size;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
        Tag encoded = CODEC.encodeStart(ops, this)
                .getOrThrow(IllegalStateException::new);
        return (CompoundTag) encoded; // 记录结构
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        if (tag == null) return;
        if (tag.isEmpty()) return;

        clearStorage();
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
        StackHandler decoded = CODEC.parse(ops, tag)
                .getOrThrow(IllegalStateException::new);
        for (int i = 0; i < decoded.size; i++)
        {
            setStackDirectly(i, decoded.keys[i], decoded.amounts[i]);
        }
    }

    private void ensureInIndex(IStackKey<?> key)
    {
        if (key != null && key != EmptyStackKey.INSTANCE && !key2stackMap.containsKey(key))
        {
            key2stackMap.put(key, key.copyStack());
        }
    }

    private void removeFromIndex(IStackKey<?> key)
    {
        if (key == null || key == EmptyStackKey.INSTANCE) return;

        // 容器是否还有该 key？
        SlotBucket kb = keyBuckets.get(key); // 注意：不能用 bucketOf() 以免误创建
        boolean stillPresent = (kb != null && kb.size() > 0);

        if (!stillPresent)
        {
            // 清理空桶（若已空），避免泄漏
            if (kb != null && kb.size() == 0)
            {
                keyBuckets.remove(key);
            }
            key2stackMap.remove(key); // ★ 只有完全没有了才移除缓存
        }
    }

    /**
     * 根据key获取已经缓存的对应stack，自行判断类型；返回值数量未设定，需要调用方自行 setCount；若要缓存请复制副本
     */
    public @Nullable Object getOutStackByKey(IStackKey<?> key)
    {
        return (key == null || key == EmptyStackKey.INSTANCE) ? null : key2stackMap.get(key);
    }
}
