package com.wintercogs.beyonddimensions.api.storage.handler.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
    // 简单的CODEC薄壳
    public static final Codec<StackHandler> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
        Tag tag = dynamic.convert(NbtOps.INSTANCE).getValue();
        if (tag instanceof CompoundTag compoundTag)
        {
            StackHandler handler = new StackHandler(getSerializedSize(compoundTag));
            handler.deserializeNBT(compoundTag);
            return handler;
        }
        return new StackHandler(0);
    }, handler -> new Dynamic<>(NbtOps.INSTANCE, handler.serializeNBT()));

    private static int getSerializedSize(CompoundTag tag)
    {
        if (tag.contains("stacks", Tag.TAG_LIST))
        {
            return tag.getList("stacks", Tag.TAG_COMPOUND).size();
        }
        if (tag.contains("Stacks", Tag.TAG_LIST))
        {
            return tag.getList("Stacks", Tag.TAG_COMPOUND).size();
        }
        return 0;
    }

    /**
     * 存储槽位数
     */
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
     * key -> 对应 stack 缓存（只维护类型，不维护数量；不缓存 EmptyStackKey）
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

        public List<Integer> snapshot()
        {
            return new ArrayList<>(slots);
        }
    }

    // typeId -> 该类型下所有非空槽位（按换尾维护）
    private final Map<ResourceLocation, SlotBucket> typeBuckets = new HashMap<>();

    private SlotBucket bucketOfType(ResourceLocation typeId)
    {
        return typeBuckets.computeIfAbsent(typeId, t -> new SlotBucket());
    }

    public Optional<SlotBucket> getBucket(ResourceLocation typeId)
    {
        return Optional.ofNullable(typeBuckets.get(typeId));
    }

    // 精确 Key -> 该 Key 占用的所有槽位
    private final Map<IStackKey<?>, SlotBucket> keyBuckets = new HashMap<>();

    private SlotBucket bucketOfKey(IStackKey<?> key)
    {
        return keyBuckets.computeIfAbsent(key, k -> new SlotBucket());
    }

    public Optional<SlotBucket> getBucket(IStackKey<?> key)
    {
        return Optional.ofNullable(keyBuckets.get(key));
    }

    public StackHandler(int size)
    {
        this.size = Math.max(0, size);
        this.keys = new IStackKey<?>[this.size];
        this.amounts = new long[this.size];

        Arrays.fill(this.keys, EmptyStackKey.INSTANCE);
        Arrays.fill(this.amounts, 0L);

        // 初始化空桶：所有槽都是空
        SlotBucket eb = bucketOfKey(EmptyStackKey.INSTANCE);
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
    {
        // 根据需要覆写（保存/脏标记/事件）
    }

    @Override
    public int getSlots()
    {
        return size;
    }

    @Override
    public void clearStorage()
    {
        Arrays.fill(keys, EmptyStackKey.INSTANCE);
        Arrays.fill(amounts, 0L);

        typeBuckets.clear();
        keyBuckets.clear();
        key2stackMap.clear();

        // 重建空桶
        SlotBucket eb = bucketOfKey(EmptyStackKey.INSTANCE);
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

        int slot = b.get(0);
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

        // 先移除旧的槽位映射（用 get()，不创建新桶）
        IStackKey<?> oldKey = keys[slot];
        if (oldKey == EmptyStackKey.INSTANCE)
        {
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

            bucketOfKey(EmptyStackKey.INSTANCE).add(slot);
            removeFromIndex(oldKey);

            onChange();
            return;
        }

        long clamped = Math.max(0L, Math.min(amount, getSlotCapacity(slot)));
        if (clamped == 0L)
        {
            keys[slot] = EmptyStackKey.INSTANCE;
            amounts[slot] = 0L;

            bucketOfKey(EmptyStackKey.INSTANCE).add(slot);
            removeFromIndex(oldKey);

            onChange();
            return;
        }

        // 写入新键
        keys[slot] = key;
        amounts[slot] = clamped;

        // 从空桶移除
        SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
        if (eb != null) eb.remove(slot);

        // 建立新映射（仅非空键）
        bucketOfType(key.getTypeId()).add(slot);
        bucketOfKey(key).add(slot);
        ensureInIndex(key);

        // 如果换键且旧键已无任何槽位占用，则移除缓存
        removeFromIndex(oldKey);

        onChange();
    }

    @Override
    public void addStackDirectly(IStackKey<?> key, long amount)
    {
        if (key == null || key == EmptyStackKey.INSTANCE || amount <= 0L) return;

        SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
        if (eb == null || eb.size() == 0) return; // 没空位

        int empty = eb.get(0);
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

                // 从空桶移除
                SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
                if (eb != null) eb.remove(slot);

                bucketOfType(key.getTypeId()).add(slot);
                bucketOfKey(key).add(slot);
                ensureInIndex(key);

                onChange();
            }

            left -= ins;
            return new KeyAmount(key, left);
        }

        // 非空：必须完全相同的 Key（equals）
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
            List<Integer> slots = exact.snapshot();
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
                List<Integer> slots = eb.snapshot();
                for (int idx : slots)
                {
                    if (left <= 0) break;
                    if (!isStackValid(idx, key)) continue;

                    long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(idx));
                    long ins = Math.min(left, cap);
                    if (ins <= 0) continue;

                    if (!simulate)
                    {
                        keys[idx] = key;
                        amounts[idx] = ins;

                        // 从空桶移除
                        eb.remove(idx);

                        bucketOfType(key.getTypeId()).add(idx);
                        bucketOfKey(key).add(idx);
                        ensureInIndex(key);
                    }
                    left -= ins;
                }
            }
        }

        if (!simulate && left != amount)
        {
            onChange(); // 可能影响多个槽位，只发一次变更
        }

        return new KeyAmount(key, left);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long count, boolean simulate)
    {
        if (slot < 0 || slot >= size || count <= 0L)
            return new KeyAmount(EmptyStackKey.INSTANCE, 0L);

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
                bucketOfKey(EmptyStackKey.INSTANCE).add(slot);

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
        IStackKey<?> realKey = key;

        if (fuzzy && key != null)
        {
            // 模糊匹配：按 isSame 找到任意一个
            for (IStackKey<?> candidate : keyBuckets.keySet())
            {
                if (candidate != null && candidate != EmptyStackKey.INSTANCE && candidate.isSame(key))
                {
                    realKey = candidate;
                    break;
                }
            }
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

                    bucketOfKey(EmptyStackKey.INSTANCE).add(slot);
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
        return key != null;
    }

    @Override
    public boolean isEmpty()
    {
        SlotBucket eb = keyBuckets.get(EmptyStackKey.INSTANCE);
        return eb != null && eb.size() == size;
    }

    public CompoundTag serializeNBT()
    {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();

        for (int i = 0; i < size; i++)
        {
            IStackKey<?> k = keys[i];
            long amt = (k == EmptyStackKey.INSTANCE) ? 0L : amounts[i];

            CompoundTag e = new CompoundTag();
            e.put("key", IStackKey.serializeNBTCommon(k));
            e.putLong("amount", amt);
            list.add(e);
        }

        root.put("stacks", list);
        return root;
    }

    /**
     * 读取：
     * - 优先新格式 "stacks"
     * - 否则回退旧格式 "Stacks"（兼容 StackTypedHandler）
     */
    public void deserializeNBT(CompoundTag tag)
    {
        clearStorage();
        if (tag == null) return;

        // 1) 新格式优先
        if (tag.contains("stacks", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("stacks", Tag.TAG_COMPOUND);
            int n = Math.min(size, list.size());

            for (int i = 0; i < n; i++)
            {
                CompoundTag e = list.getCompound(i);

                // key: 使用 common 统一反序列化
                IStackKey<?> k = EmptyStackKey.INSTANCE;
                if (e.contains("key", Tag.TAG_COMPOUND))
                {
                    k = IStackKey.deserializeNBTCommon(e.getCompound("key"));
                }

                long amt = e.getLong("amount");
                setStackDirectly(i, k, amt);
            }
            return;
        }

        // 2) 旧格式回退：兼容 StackTypedHandler 写出的 "Stacks"
        if (tag.contains("Stacks", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Stacks", Tag.TAG_COMPOUND);

            for (int i = 0; i < size; i++)
            {
                if (i >= list.size()) break;

                CompoundTag entry = list.getCompound(i);
                String typeStr = entry.getString("Type");

                // 旧空占位：Type == "Empty" 或 TypedStack 不是 Compound
                Tag typedNode = entry.get("TypedStack");
                if ("Empty".equals(typeStr) || !(typedNode instanceof CompoundTag typedCompound))
                {
                    continue; // 保持空
                }

                ResourceLocation typeId = ResourceLocation.tryParse(typeStr);
                if (typeId == null) continue;

                // 旧版TypedStack内部也会写id，直接交给KeyAmount统一解析
                KeyAmount ka = KeyAmount.deserializeNBT(typedCompound);
                if (ka.isEmpty()) continue;
                setStackDirectly(i, ka.key(), ka.amount());
            }
        }
    }



    /* ================= 缓存索引维护 ================= */

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

        SlotBucket kb = keyBuckets.get(key); // 注意：不能用 bucketOfKey() 以免误创建
        boolean stillPresent = (kb != null && kb.size() > 0);

        if (!stillPresent)
        {
            if (kb != null && kb.size() == 0)
            {
                keyBuckets.remove(key);
            }
            key2stackMap.remove(key);
        }
    }

    /**
     * 根据 key 获取已经缓存的对应 stack，自行判断类型；
     * 返回值数量未设定，需要调用方自行 setCount；若要缓存请复制副本
     */
    public @Nullable Object getOutStackByKey(IStackKey<?> key)
    {
        return (key == null || key == EmptyStackKey.INSTANCE) ? null : key2stackMap.get(key);
    }
}
