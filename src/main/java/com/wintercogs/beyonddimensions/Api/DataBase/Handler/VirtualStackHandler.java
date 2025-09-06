package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 基于{@link IStackHandler}接口的无序存储实现。
 * <p>
 * 具有以下特点：
 * <ul>
 *     <li>每种精确匹配的堆叠类型仅允许占用一个槽位</li>
 *     <li>插入的堆叠会自动分配槽位</li>
 *     <li>实际用来存储大小不实际反映最大槽位数量</li>
 * </ul>
 * <p>
 */
public class VirtualStackHandler implements IStackHandler
{
    /** 实际存储：key -> amount；约定：amount > 0；<=0 的键会被移除 */
    private final Map<IStackKey<?>, Long> storage = new HashMap<>();

    /** 非空键的紧凑槽位索引（换尾删除） */
    private final ArrayList<IStackKey<?>> slotIndex = new ArrayList<>();

    /** key -> 槽位位置（仅记录非空键） */
    private final Map<IStackKey<?>, Integer> posMap = new HashMap<>();

    /** 槽位数量上限（虚拟容器用） */
    private int slotMaxSize = Integer.MAX_VALUE;
    private long slotCapacity = Long.MAX_VALUE;

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

    private VirtualStackHandler(long capacity, int maxSize)
    {
        this.slotCapacity = capacity;
        this.slotMaxSize = maxSize;
    }


    @Override
    public List<KeyAmount> getStorage() {
        return entriesView; // 不可修改、动态视图
    }

    @Override
    public void onChange() {
        // 留空：需要时由调用方覆写或改为回调
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
    @Override
    public @NotNull KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        if (key == null) return new KeyAmount(null, Math.max(0L, amount));
        long add = Math.max(0L, amount);
        if (add == 0L) return new KeyAmount(key, 0L);

        long current = storage.getOrDefault(key, 0L);

        // 是否需要新建槽位
        boolean needNewSlot = (current == 0L) && !posMap.containsKey(key);
        if (needNewSlot && slotIndex.size() >= slotMaxSize) {
            // 槽位不足：整量剩余
            return new KeyAmount(key, add);
        }

        // 计算剩余容量（每键上限）
        long room = (slotCapacity <= current) ? 0L : (slotCapacity - current);
        if (room <= 0L) {
            return new KeyAmount(key, add); // 已达单键上限
        }

        long actual = Math.min(room, add); // 此处，room和add均始终大于0，故actual始终大于0
        long leftover = add - actual;

        if (!simulate)
        {
            storage.put(key, current + actual);
            ensureInIndex(key);
            onChange();
        }
        return new KeyAmount(key, leftover);
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
            onChange();
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
            onChange();
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
    }
}
