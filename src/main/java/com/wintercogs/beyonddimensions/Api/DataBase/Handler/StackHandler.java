package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

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

    public static final Codec<StackHandler> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    KeyAmount.CODEC.listOf().fieldOf("stacks")
                            .forGetter(stackHandler -> {
                                java.util.ArrayList<KeyAmount> list = new java.util.ArrayList<>(stackHandler.size);
                                for (int i = 0; i < stackHandler.size; i++) {
                                    list.add(new KeyAmount(stackHandler.keys[i], stackHandler.amounts[i]));
                                }
                                return list;
                            })
            ).apply(instance, StackHandler::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf,StackHandler> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public StackHandler decode(RegistryFriendlyByteBuf buf)
        {
            int size = buf.readVarInt();
            ArrayList<KeyAmount> list = new ArrayList<>();
            for(int i = 0; i < size; i++)
            {
                list.add(new KeyAmount(IStackKey.STREAM_CODEC.decode(buf), buf.readVarLong()));
            }
            return new StackHandler(list);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, StackHandler stackHandler)
        {
            buf.writeVarInt(stackHandler.size);
            for(int i = 0; i < stackHandler.size; i++)
            {
                stackHandler.keys[i].serialize(buf);
                buf.writeVarLong(stackHandler.amounts[i]);
            }
        }
    };

    /* ---------------- 基本存储（固定大小） ---------------- */

    private final int size;
    private final IStackKey<?>[] keys;   // 槽位上的 Key（null 代表空）
    private final long[] amounts;        // 槽位上的数量（==0 则视为空，与 keys 同步）

    // 空槽位标记：bit=1 表示该槽位为空
    private final BitSet emptySlots;

    /** key -> 具体存储物的对照（如ItemStackKey -> ItemStack），只维护类型，不维护数量，分化包装读取时手动设置数量 */
    private final Map<IStackKey<?>, Object> key2stackMap = new HashMap<>();

    /* ---------------- 只读视图 ---------------- */

    private final List<KeyAmount> entriesView = Collections.unmodifiableList(new AbstractList<KeyAmount>() {
        @Override public KeyAmount get(int index) {
            if (index < 0 || index >= size) return new KeyAmount(null, 0L);
            IStackKey<?> k = keys[index];
            long amt = (k == null) ? 0L : amounts[index];
            return new KeyAmount(k, amt);
        }
        @Override public int size() { return size; }
    });

    /* ---------------- 索引（类型桶 / 精确 Key 桶） ---------------- */
    /* 一方面加速分化包装，另一方面加速按key查询、插入、移除 */
    public static final class SlotBucket {
        final ArrayList<Integer> slots = new ArrayList<>();
        final HashMap<Integer, Integer> pos = new HashMap<>(); // slot -> index in slots

        void add(int slot) {
            if (pos.containsKey(slot)) return;
            int i = slots.size();
            slots.add(slot);
            pos.put(slot, i);
        }
        void remove(int slot) {
            Integer p = pos.remove(slot);
            if (p == null) return;
            int last = slots.size() - 1;
            if (p != last) {
                int tail = slots.get(last);
                slots.set(p, tail);
                pos.put(tail, p);
            }
            slots.remove(last);
        }
        int size() { return slots.size(); }
        int get(int i) { return slots.get(i); }
        List<Integer> snapshot() { return new ArrayList<>(slots); } // 防御性副本（模拟/遍历时可用）
    }

    // typeId -> 该类型下所有非空槽位（按换尾维护）
    private final Map<ResourceLocation, SlotBucket> typeBuckets = new HashMap<>();
    private SlotBucket bucketOf(ResourceLocation typeId) {
        return typeBuckets.computeIfAbsent(typeId, t -> new SlotBucket());
    }
    public Optional<SlotBucket> getBucket(ResourceLocation typeId)
    {
        return Optional.ofNullable(typeBuckets.get(typeId));
    }

    // 精确 Key -> 该 Key 占用的所有槽位（允许同 Key 多槽）
    private final Map<IStackKey<?>, SlotBucket> keyBuckets = new HashMap<>();
    private SlotBucket bucketOf(IStackKey<?> key) {
        return keyBuckets.computeIfAbsent(key, k -> new SlotBucket());
    }
    public Optional<SlotBucket> getBucket(IStackKey<?> key)
    {
        return Optional.ofNullable(keyBuckets.get(key));
    }

    /* ---------------- 构造 ---------------- */

    public StackHandler(int size) {
        this.size = Math.max(0, size);
        this.keys = new IStackKey<?>[this.size];
        this.amounts = new long[this.size];
        this.emptySlots = new BitSet(this.size);
        this.emptySlots.set(0, this.size, true); // 初始全部为空
    }

    public StackHandler(List<KeyAmount> stacks)
    {
        this(stacks.size());

        for(int i=0;i<this.size;i++)
        {
            KeyAmount ka = stacks.get(i);
            if(ka != null)
            {
                setStackDirectly(i,ka.key(),ka.amount());
            }
            else
            {
                setStackDirectly(i,ItemStackKey.EMPTY,0);
            }
        }
    }

    /* ---------------- IStackHandler 实现 ---------------- */

    @Override public List<KeyAmount> getStorage() { return entriesView; }

    @Override public void onChange() { /* 根据需要覆写（保存/脏标记/事件） */ }

    @Override public int getSlots() { return size; }

    @Override
    public void clearStorage() {
        // 清空数组
        Arrays.fill(keys, null);
        Arrays.fill(amounts, 0L);
        // 清空索引
        typeBuckets.clear();
        keyBuckets.clear();
        // 标记空槽
        emptySlots.clear();
        emptySlots.set(0, size, true);
        onChange();
    }

    @Override
    public @NotNull KeyAmount getStackBySlot(int slot) {
        if (slot < 0 || slot >= size) return new KeyAmount(null, 0L);
        IStackKey<?> k = keys[slot];
        return new KeyAmount(k, (k == null) ? 0L : amounts[slot]);
    }

    @Override
    public @NotNull KeyAmount getStackByKey(IStackKey<?> key) {
        if (key == null) return new KeyAmount(null, 0L);
        SlotBucket b = keyBuckets.get(key);
        if (b == null || b.size() == 0) return new KeyAmount(key, 0L);
        int slot = b.get(0); // “第一个找到的堆叠”
        return new KeyAmount(key, amounts[slot]);
    }

    @Override
    public boolean hasStack(IStackKey<?> key) {
        SlotBucket b = (key == null) ? null : keyBuckets.get(key);
        return b != null && b.size() > 0;
    }

    @Override
    public void setStackDirectly(int slot, IStackKey<?> key, long amount) {
        if (slot < 0 || slot >= size) return;

        // 先移除旧的槽位映射
        IStackKey<?> oldKey = keys[slot];
        if (oldKey != null) {
            bucketOf(oldKey.getTypeId()).remove(slot);
            bucketOf(oldKey).remove(slot);
        }

        if (key == null || amount <= 0L) {
            // 置空
            keys[slot] = null;
            amounts[slot] = 0L;
            emptySlots.set(slot);
            onChange();
            return;
        }

        long clamped = Math.max(0L, Math.min(amount, getSlotCapacity(slot)));
        keys[slot] = key;
        amounts[slot] = clamped;

        // 建立新映射
        bucketOf(key.getTypeId()).add(slot);
        bucketOf(key).add(slot);
        emptySlots.clear(slot);

        onChange();
    }

    @Override
    public void addStackDirectly(IStackKey<?> key, long amount) {
        if (key == null || amount <= 0L) return;
        int empty = emptySlots.nextSetBit(0);
        if (empty < 0) return; // 没空位
        setStackDirectly(empty, key, amount);
    }

    @Override
    public @NotNull KeyAmount insert(int slot, IStackKey<?> key, long amount, boolean simulate) {
        if (key == null || amount <= 0L) return new KeyAmount(null, 0L);
        if (slot < 0 || slot >= size) return new KeyAmount(key, amount);
        if (!isStackValid(slot, key)) return new KeyAmount(key, amount);

        long left = amount;

        IStackKey<?> curKey = keys[slot];
        if (curKey == null) {
            // 容量同时受key类型的原版最大容量以及槽位最大容量限制
            long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(slot));
            long ins = Math.min(left, cap);
            if (ins <= 0) return new KeyAmount(key, left);
            if (!simulate) {
                keys[slot] = key;
                amounts[slot] = ins;
                bucketOf(key.getTypeId()).add(slot);
                bucketOf(key).add(slot);
                emptySlots.clear(slot);
                ensureInIndex(key);
                onChange();
            }
            left -= ins;
            return new KeyAmount(key, left);
        }

        // 非空：类型必须相同（完全相同的 Key）
        if (!curKey.equals(key)) {
            return new KeyAmount(key, left);
        }

        long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(slot));
        long room = Math.max(0L, cap - amounts[slot]);
        long ins = Math.min(left, room);
        if (ins <= 0) return new KeyAmount(key, left);
        if (!simulate) {
            amounts[slot] += ins;
            onChange();
        }
        left -= ins;
        return new KeyAmount(key, left);
    }

    @Override
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate) {
        if (key == null || amount <= 0L) return new KeyAmount(null, 0L);

        long left = amount;

        // 第一阶段：合并已有同 Key 的槽位
        SlotBucket exact = keyBuckets.get(key);
        if (exact != null && exact.size() > 0) {
            // 为避免遍历时被结构修改，做一个快照
            List<Integer> slots = exact.snapshot();
            for (int slot : slots) {
                if (left <= 0) break;
                long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(slot));
                long room = Math.max(0L, cap - amounts[slot]);
                if (room <= 0) continue;
                long ins = Math.min(left, room);
                if (simulate) {
                    left -= ins;
                } else {
                    amounts[slot] += ins;
                    left -= ins;
                }
            }
        }

        // 第二阶段：填充空槽位
        if (left > 0) {
            int idx = emptySlots.nextSetBit(0);
            while (idx >= 0 && left > 0) {
                if (isStackValid(idx, key)) {
                    long cap = Math.min(key.getVanillaMaxStackSize(), getSlotCapacity(idx));
                    long ins = Math.min(left, cap);
                    if (ins > 0) {
                        if (!simulate) {
                            keys[idx] = key;
                            amounts[idx] = ins;
                            bucketOf(key.getTypeId()).add(idx);
                            bucketOf(key).add(idx);
                            emptySlots.clear(idx);
                            ensureInIndex(key);
                        }
                        left -= ins;
                    }
                }
                idx = emptySlots.nextSetBit(idx + 1);
            }
        }

        if (!simulate && left != amount) {
            onChange(); // 本次按类型插入可能影响多个槽位，只发一次变更
        }

        return new KeyAmount(key, left);
    }

    @Override
    public @NotNull KeyAmount extract(int slot, long count, boolean simulate) {
        if (slot < 0 || slot >= size || count <= 0L) return new KeyAmount(null, 0L);
        IStackKey<?> k = keys[slot];
        if (k == null) return new KeyAmount(null, 0L);

        long have = amounts[slot];
        long take = Math.min(count, have);
        if (take <= 0) return new KeyAmount(k, 0L);

        if (!simulate) {
            long left = have - take;
            if (left == 0L) {
                // 置空并维护索引
                bucketOf(k.getTypeId()).remove(slot);
                bucketOf(k).remove(slot);
                keys[slot] = null;
                amounts[slot] = 0L;
                emptySlots.set(slot);
                removeFromIndex(k);
            } else {
                amounts[slot] = left;
            }
            onChange();
        }
        return new KeyAmount(k, take);
    }

    @Override
    public @NotNull KeyAmount extract(IStackKey<?> key, long amount, boolean simulate) {
        if (key == null || amount <= 0L) return new KeyAmount(null, 0L);
        SlotBucket exact = keyBuckets.get(key);
        if (exact == null || exact.size() == 0) return new KeyAmount(key, 0L);

        long need = amount;
        long taken = 0L;

        // 快照防止遍历期间结构改变
        List<Integer> slots = exact.snapshot();
        for (int slot : slots) {
            if (need <= 0) break;
            long have = amounts[slot];
            if (have <= 0) continue;
            long t = Math.min(need, have);
            if (t <= 0) continue;

            if (!simulate) {
                long left = have - t;
                if (left == 0L) {
                    bucketOf(key.getTypeId()).remove(slot);
                    bucketOf(key).remove(slot);
                    keys[slot] = null;
                    amounts[slot] = 0L;
                    emptySlots.set(slot);
                    removeFromIndex(key);
                } else {
                    amounts[slot] = left;
                }
            }
            taken += t;
            need -= t;
        }

        if (!simulate && taken > 0) onChange();
        return new KeyAmount(key, taken);
    }

    @Override
    public long getSlotCapacity(int slot) {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isStackValid(int slot, IStackKey<?> key) {
        return true; // 如需白名单/黑名单，覆写或改成策略
    }

    @Override
    public boolean isEmpty() {
        // 有空位 BitSet 不等于“非空计数”，但 keys[slot]==null 意味该位为 1
        // 只要有任意非空槽位，即空位数 < 总槽位
        return emptySlots.cardinality() == size;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
        Tag encoded = CODEC.encodeStart(ops, this)
                .getOrThrow(msg -> new IllegalStateException());
        // 这里一定是 CompoundTag（因为用了 fieldOf -> 记录结构）
        return (CompoundTag) encoded;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        clearStorage();
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, provider);
        StackHandler decoded = CODEC.parse(ops, tag)
                .getOrThrow(msg -> new IllegalStateException());
        for(int i = 0; i < decoded.size; i++)
        {
            setStackDirectly(i,decoded.keys[i],decoded.amounts[i]);
        }
    }

    private void ensureInIndex(IStackKey<?> key)
    {
        if(!key2stackMap.containsKey(key))
        {
            key2stackMap.put(key,key.copyStack());
        }

    }

    private void removeFromIndex(IStackKey<?> key)
    {
        key2stackMap.remove(key);
    }

    /** 根据key获取已经缓存的对应stack，自行判断类型，返回值的数量无法确定，根据keyAmount自己使用setCount，如果要缓存它，必须复制一个副本 */
    public Object getOutStackByKey(IStackKey<?> key)
    {
        return key2stackMap.get(key);
    }
}
