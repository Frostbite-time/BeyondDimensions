package com.wintercogs.beyonddimensions.api.capability.helper.unordered;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * 动态槽位视图：size() 随存储内容实时变化，调用方持有的索引可能在两次调用间失效。
 * 因此对越界索引不抛错：读取返回 EMPTY/0，插入与提取返回 0。
 */
public class ItemUnifiedStorageHandler implements ResourceHandler<@NotNull ItemResource>
{
    private final UnifiedStorage storage;
    private final ArrayList<StackJournal> snapshotJournals = new ArrayList<>();

    public ItemUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    private int itemCount()
    {
        return storage.getBucket(ItemStackKey.ID)
                .map(AbstractUnorderedStackHandler.TypeBucket::size)
                .orElse(0);
    }

    private IStackKey<?> getItemKeyAt(int index)
    {
        if (index < 0) return null;
        return storage.getBucket(ItemStackKey.ID)
                .map(bucket -> index < bucket.size() ? bucket.get(index) : null)
                .orElse(null);
    }

    private static ItemResource toResource(KeyAmount ka, UnifiedStorage storage)
    {
        if (ka == null || ka.isEmpty()) return ItemResource.EMPTY;

        Object outStack = storage.getOutStackByKey(ka.key());
        if (outStack instanceof ItemStack itemStack && !itemStack.isEmpty())
        {
            return ItemResource.of(itemStack);
        }

        Object stack = ka.toStack();
        if (stack instanceof ItemStack itemStack && !itemStack.isEmpty())
        {
            return ItemResource.of(itemStack);
        }

        return ItemResource.EMPTY;
    }

    private static boolean matches(KeyAmount ka, ItemResource resource, UnifiedStorage storage)
    {
        if (ka == null || ka.isEmpty() || resource.isEmpty()) return false;

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

    /**
     * 查找或创建与指定 key 关联的 StackJournal。
     * 遍历 snapshotJournals 列表找到匹配的 journal，若不存在则新建。
     */
    private StackJournal getOrCreateJournal(IStackKey<?> key)
    {
        for (StackJournal journal : snapshotJournals)
        {
            if (journal.getKey().equals(key))
            {
                return journal;
            }
        }
        StackJournal journal = new StackJournal(key);
        snapshotJournals.add(journal);
        return journal;
    }

    @Override
    public int size()
    {
        int items = itemCount();
        return storage.isFullSlotsSize() ? items : items + 1;
    }

    @Override
    public ItemResource getResource(int index)
    {
        IStackKey<?> key = getItemKeyAt(index);
        if (key == null) return ItemResource.EMPTY;

        KeyAmount ka = storage.getStackByKey(key);
        return toResource(ka, storage);
    }

    @Override
    public long getAmountAsLong(int index)
    {
        IStackKey<?> key = getItemKeyAt(index);
        if (key == null) return 0L;

        return Math.max(0L, storage.getStackByKey(key).amount());
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource)
    {
        if (!resource.isEmpty() && !isValid(index, resource))
        {
            return 0L;
        }

        long cap = Math.max(0L, storage.getSlotCapacity(0));
        if (resource.isEmpty()) return cap;
        return Math.min(cap, resource.getMaxStackSize());
    }

    @Override
    public boolean isValid(int index, ItemResource resource)
    {
        TransferPreconditions.checkNonEmpty(resource);
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;

        ItemStackKey key = toKey(resource);
        KeyAmount simulatedLeft = storage.insert(key, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        getOrCreateJournal(key).updateSnapshots(transaction);

        KeyAmount left = storage.insert(key, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;

        IStackKey<?> key = getItemKeyAt(index);
        if (key == null) return 0;

        KeyAmount current = storage.getStackByKey(key);
        if (!matches(current, resource, storage)) return 0;

        KeyAmount simulated = storage.extract(key, amount, true, false);
        if (simulated.isEmpty() || simulated.amount() <= 0L) return 0;

        getOrCreateJournal(key).updateSnapshots(transaction);

        KeyAmount taken = storage.extract(key, amount, false, false);
        return BDMath.clampLongToInt(Math.max(0L, taken.amount()));
    }

    // ---- Per-key journal for transaction support ----

    private class StackJournal extends SnapshotJournal<KeyAmount>
    {
        private final IStackKey<?> key;

        StackJournal(IStackKey<?> key)
        {
            this.key = key;
        }

        public IStackKey<?> getKey()
        {
            return key;
        }

        @Override
        protected KeyAmount createSnapshot()
        {
            return storage.getStackByKey(key);
        }

        @Override
        protected void revertToSnapshot(KeyAmount snapshot)
        {
            if (snapshot == null) return;
            if (snapshot.isEmpty() || snapshot.key().isEmpty())
            {
                // 该 key 原先不存在于存储中，将其置零移除
                storage.setAmountByKey(key, 0L);
            }
            else
            {
                storage.setAmountByKey(snapshot.key(), snapshot.amount());
            }
        }
    }
}