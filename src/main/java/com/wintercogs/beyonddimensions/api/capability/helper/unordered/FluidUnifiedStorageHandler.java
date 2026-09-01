package com.wintercogs.beyonddimensions.api.capability.helper.unordered;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * 动态槽位视图：size() 随存储内容实时变化，调用方持有的索引可能在两次调用间失效。
 * 因此对越界索引不抛错：读取返回 EMPTY/0，插入与提取返回 0。
 */
public class FluidUnifiedStorageHandler implements ResourceHandler<@NotNull FluidResource>
{
    private final UnifiedStorage storage;
    private final ArrayList<StackJournal> snapshotJournals = new ArrayList<>();

    public FluidUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    private int fluidCount()
    {
        return storage.getBucket(FluidStackKey.ID)
                .map(AbstractUnorderedStackHandler.TypeBucket::size)
                .orElse(0);
    }

    private IStackKey<?> getFluidKeyAt(int index)
    {
        if (index < 0) return null;
        return storage.getBucket(FluidStackKey.ID)
                .map(bucket -> index < bucket.size() ? bucket.get(index) : null)
                .orElse(null);
    }

    private static FluidResource toResource(KeyAmount ka, UnifiedStorage storage)
    {
        if (ka == null || ka.isEmpty()) return FluidResource.EMPTY;

        Object outStack = storage.getOutStackByKey(ka.key());
        if (outStack instanceof FluidStack fluidStack && !fluidStack.isEmpty())
        {
            return FluidResource.of(fluidStack);
        }

        Object stack = ka.toStack();
        if (stack instanceof FluidStack fluidStack && !fluidStack.isEmpty())
        {
            return FluidResource.of(fluidStack);
        }

        return FluidResource.EMPTY;
    }

    private static boolean matches(KeyAmount ka, FluidResource resource, UnifiedStorage storage)
    {
        if (ka == null || ka.isEmpty() || resource.isEmpty()) return false;

        Object outStack = storage.getOutStackByKey(ka.key());
        if (outStack instanceof FluidStack fluidStack && !fluidStack.isEmpty())
        {
            return resource.matches(fluidStack);
        }

        Object stack = ka.key().copyStack();
        return stack instanceof FluidStack fluidStack && !fluidStack.isEmpty() && resource.matches(fluidStack);
    }

    private static FluidStackKey toKey(FluidResource resource)
    {
        return new FluidStackKey(resource.toStack(1));
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
        int fluids = fluidCount();
        return storage.isFullSlotsSize() ? fluids : fluids + 1;
    }

    @Override
    public FluidResource getResource(int index)
    {
        IStackKey<?> key = getFluidKeyAt(index);
        if (key == null) return FluidResource.EMPTY;

        KeyAmount ka = storage.getStackByKey(key);
        return toResource(ka, storage);
    }

    @Override
    public long getAmountAsLong(int index)
    {
        IStackKey<?> key = getFluidKeyAt(index);
        if (key == null) return 0L;

        return Math.max(0L, storage.getStackByKey(key).amount());
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource)
    {
        if (!resource.isEmpty() && !isValid(index, resource))
        {
            return 0L;
        }

        return Math.max(0L, storage.getSlotCapacity(0));
    }

    @Override
    public boolean isValid(int index, FluidResource resource)
    {
        TransferPreconditions.checkNonEmpty(resource);
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;

        FluidStackKey key = toKey(resource);
        KeyAmount simulatedLeft = storage.insert(key, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        getOrCreateJournal(key).updateSnapshots(transaction);

        KeyAmount left = storage.insert(key, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;

        IStackKey<?> key = getFluidKeyAt(index);
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