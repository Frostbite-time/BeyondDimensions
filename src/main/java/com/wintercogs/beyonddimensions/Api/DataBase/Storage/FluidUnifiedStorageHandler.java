package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Util.BDMath;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FluidUnifiedStorageHandler extends SnapshotJournal<List<KeyAmount>> implements ResourceHandler<@NotNull FluidResource>
{
    private final UnifiedStorage storage;

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

    @Override
    public int size()
    {
        int fluids = fluidCount();
        return storage.isFullSlotsSize() ? fluids : fluids + 1;
    }

    @Override
    public FluidResource getResource(int index)
    {
        Objects.checkIndex(index, size());

        IStackKey<?> key = getFluidKeyAt(index);
        if (key == null) return FluidResource.EMPTY;

        KeyAmount ka = storage.getStackByKey(key);
        return toResource(ka, storage);
    }

    @Override
    public long getAmountAsLong(int index)
    {
        Objects.checkIndex(index, size());

        IStackKey<?> key = getFluidKeyAt(index);
        if (key == null) return 0L;

        return Math.max(0L, storage.getStackByKey(key).amount());
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource)
    {
        Objects.checkIndex(index, size());

        if (!resource.isEmpty() && !isValid(index, resource))
        {
            return 0L;
        }

        return Math.max(0L, storage.getSlotCapacity(0));
    }

    @Override
    public boolean isValid(int index, FluidResource resource)
    {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmpty(resource);
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, @NotNull TransactionContext transaction)
    {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;

        FluidStackKey key = toKey(resource);
        KeyAmount simulatedLeft = storage.insert(key, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        updateSnapshots(transaction);

        KeyAmount left = storage.insert(key, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, @NotNull TransactionContext transaction)
    {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;

        IStackKey<?> key = getFluidKeyAt(index);
        if (key == null) return 0;

        KeyAmount current = storage.getStackByKey(key);
        if (!matches(current, resource, storage)) return 0;

        KeyAmount simulated = storage.extract(key, amount, true, false);
        if (simulated.isEmpty() || simulated.amount() <= 0L) return 0;

        updateSnapshots(transaction);

        KeyAmount taken = storage.extract(key, amount, false, false);
        return BDMath.clampLongToInt(Math.max(0L, taken.amount()));
    }

    @Override
    protected List<KeyAmount> createSnapshot()
    {
        List<KeyAmount> view = storage.getStorage();
        ArrayList<KeyAmount> snapshot = new ArrayList<>(view.size());
        for (int i = 0; i < view.size(); i++)
        {
            KeyAmount ka = view.get(i);
            snapshot.add(new KeyAmount(ka.key(), ka.amount()));
        }
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(List<KeyAmount> snapshot)
    {
        if (snapshot == null) return;

        storage.clearStorage();
        for (int i = 0; i < snapshot.size(); i++)
        {
            KeyAmount ka = snapshot.get(i);
            if (!ka.isEmpty())
            {
                storage.setAmountByKey(ka.key(), ka.amount());
            }
        }
    }
}
