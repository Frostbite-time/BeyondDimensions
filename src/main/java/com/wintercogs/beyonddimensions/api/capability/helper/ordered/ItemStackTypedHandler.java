package com.wintercogs.beyonddimensions.api.capability.helper.ordered;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 仅基于桶（Item 桶 + Empty 桶）进行索引映射的 Item 资源视图。
 * - 不做快照/镜像；单次调用内直接访问桶当前状态。
 * - 可视槽位 = ItemStackKey 桶槽位 + EmptyStackKey 桶槽位（顺序：先 Item，后 Empty）。
 * - 对外实现 ResourceHandler<ItemResource>，并通过 SnapshotJournal 支持事务回滚。
 * - 动态槽位：size() 随存储内容实时变化，调用方持有的索引可能在两次调用间失效，
 * 因此对越界索引不抛错：读取返回 EMPTY/0，插入与提取返回 0，isValid 返回 false。
 */
public class ItemStackTypedHandler extends SnapshotJournal<List<KeyAmount>> implements ResourceHandler<@NotNull ItemResource>
{
    private static final Identifier ITEM_TYPE = ItemStackKey.ID;

    private final StackHandler handlerStorage;

    public ItemStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    // ---- 小工具：纯 Optional 计算，避免 orElse(null) ----

    /**
     * Item 桶的槽位数量（非空的 ItemStackKey 槽）。
     */
    private int itemCount()
    {
        return handlerStorage.getBucket(ITEM_TYPE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 空桶的槽位数量（EmptyStackKey 槽）。
     */
    private int emptyCount()
    {
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    /**
     * 可视槽位是否处于 Item 区域（非 Empty 区域）。
     */
    private boolean inItemRegion(int visibleSlot)
    {
        int items = itemCount();
        return visibleSlot >= 0 && visibleSlot < items;
    }

    /**
     * 取 Item 桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getItemSlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(ITEM_TYPE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    /**
     * 取空桶中第 index 个槽的真实索引；若不存在返回 -1。
     */
    private int getEmptySlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    /**
     * 将“可视槽位”映射为真实槽位；无效则 -1。
     */
    private int resolveActualIndex(int visibleSlot)
    {
        if (visibleSlot < 0) return -1;
        int items = itemCount();
        if (visibleSlot < items)
        {
            return getItemSlotAt(visibleSlot);
        }
        int rest = visibleSlot - items;
        return getEmptySlotAt(rest);
    }

    private static ItemResource toResource(KeyAmount ka, StackHandler storage)
    {
        if (ka.isEmpty()) return ItemResource.EMPTY;

        Object cached = storage.getOutStackByKey(ka.key());
        if (cached instanceof ItemStack item && !item.isEmpty())
        {
            return ItemResource.of(item);
        }

        Object stack = ka.toStack();
        if (stack instanceof ItemStack item && !item.isEmpty())
        {
            return ItemResource.of(item);
        }

        return ItemResource.EMPTY;
    }

    private static boolean matches(KeyAmount ka, ItemResource resource, StackHandler storage)
    {
        if (ka.isEmpty() || resource.isEmpty()) return false;

        Object cached = storage.getOutStackByKey(ka.key());
        if (cached instanceof ItemStack item && !item.isEmpty())
        {
            return resource.matches(item);
        }

        Object stack = ka.key().copyStack();
        return stack instanceof ItemStack item && !item.isEmpty() && resource.matches(item);
    }

    private static ItemStackKey toKey(ItemResource resource)
    {
        return new ItemStackKey(resource.toStack(1));
    }

    // ---- ResourceHandler ----

    /**
     * 可视槽位 = Item 桶 + 空桶；全空时也会 > 0。
     */
    @Override
    public int size()
    {
        return itemCount() + emptyCount();
    }

    /**
     * 高频：直接使用缓存对象并设数量。
     * 若缓存不存在/为空，或该槽处于空区域，则返回 ItemStack.EMPTY。
     */
    @Override
    public ItemResource getResource(int index)
    {
        if (!inItemRegion(index)) return ItemResource.EMPTY;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return ItemResource.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        return toResource(ka, handlerStorage);
    }

    /**
     * 插入：可落在空槽区或 Item 区，内部 insert 会做校验与容量约束。
     */
    @Override
    public long getAmountAsLong(int index)
    {
        if (!inItemRegion(index)) return 0L;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0L;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        return ka.isEmpty() ? 0L : Math.max(0L, ka.amount());
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource)
    {
        if (!resource.isEmpty() && !isValid(index, resource))
        {
            return 0L;
        }

        if (!inItemRegion(index))
        {
            return 99L;
        }

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0)
        {
            return 99L;
        }

        long byType = resource.isEmpty() ? 99L : resource.getMaxStackSize();
        long byCap = handlerStorage.getSlotCapacity(actualIndex);
        return Math.max(0L, Math.min(byType, byCap));
    }

    @Override
    public boolean isValid(int index, ItemResource resource)
    {
        TransferPreconditions.checkNonEmpty(resource);

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0)
        {
            return false;
        }

        IStackKey<?> key = toKey(resource);
        return handlerStorage.isStackValid(actualIndex, key);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) return 0;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0;

        ItemStackKey key = toKey(resource);
        KeyAmount simulatedLeft = handlerStorage.insert(actualIndex, key, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        updateSnapshots(transaction);

        KeyAmount left = handlerStorage.insert(actualIndex, key, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    /**
     * 仅 Item 区可抽取；空槽区恒 EMPTY。
     */
    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) return 0;
        if (!inItemRegion(index)) return 0;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0;

        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        if (!matches(current, resource, handlerStorage)) return 0;

        KeyAmount simulated = handlerStorage.extract(actualIndex, amount, true);
        if (simulated.isEmpty() || simulated.amount() <= 0L) return 0;

        updateSnapshots(transaction);

        KeyAmount taken = handlerStorage.extract(actualIndex, amount, false);
        return BDMath.clampLongToInt(Math.max(0L, taken.amount()));
    }

    @Override
    protected List<KeyAmount> createSnapshot()
    {
        int total = handlerStorage.getSlots();
        ArrayList<KeyAmount> snapshot = new ArrayList<>(total);
        for (int i = 0; i < total; i++)
        {
            KeyAmount ka = handlerStorage.getStackBySlot(i);
            snapshot.add(new KeyAmount(ka.key(), ka.amount()));
        }
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(List<KeyAmount> snapshot)
    {
        if (snapshot == null) return;

        int total = handlerStorage.getSlots();
        int restore = Math.min(total, snapshot.size());

        for (int i = 0; i < restore; i++)
        {
            KeyAmount ka = snapshot.get(i);
            handlerStorage.setStackDirectly(i, ka.key(), ka.amount());
        }
        for (int i = restore; i < total; i++)
        {
            handlerStorage.setStackDirectly(i, EmptyStackKey.INSTANCE, 0L);
        }
    }
}
