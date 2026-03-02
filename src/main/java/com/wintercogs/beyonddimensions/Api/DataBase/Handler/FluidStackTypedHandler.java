package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Util.BDMath;
import net.minecraft.resources.Identifier;
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

public class FluidStackTypedHandler extends SnapshotJournal<List<KeyAmount>> implements ResourceHandler<@NotNull FluidResource>
{
    private static final Identifier FLUID_TYPE = FluidStackKey.ID;
    private static final long DEFAULT_TANK_CAPACITY = 64000L;

    private final StackHandler handlerStorage;

    public FluidStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    private int fluidCount()
    {
        return handlerStorage.getBucket(FLUID_TYPE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    private int emptyCount()
    {
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    private boolean inFluidRegion(int visibleSlot)
    {
        int fluids = fluidCount();
        return visibleSlot >= 0 && visibleSlot < fluids;
    }

    private int getFluidSlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(FLUID_TYPE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    private int getEmptySlotAt(int index)
    {
        if (index < 0) return -1;
        return handlerStorage.getBucket(EmptyStackKey.INSTANCE)
                .map(b -> (index < b.size()) ? b.get(index) : -1)
                .orElse(-1);
    }

    private int resolveActualIndex(int visibleSlot)
    {
        if (visibleSlot < 0) return -1;
        int fluids = fluidCount();
        if (visibleSlot < fluids)
        {
            return getFluidSlotAt(visibleSlot);
        }
        int rest = visibleSlot - fluids;
        return getEmptySlotAt(rest);
    }

    private static FluidResource toResource(KeyAmount ka, StackHandler storage)
    {
        if (ka.isEmpty()) return FluidResource.EMPTY;

        Object cached = storage.getOutStackByKey(ka.key());
        if (cached instanceof FluidStack fluid && !fluid.isEmpty())
        {
            return FluidResource.of(fluid);
        }

        Object stack = ka.toStack();
        if (stack instanceof FluidStack fluid && !fluid.isEmpty())
        {
            return FluidResource.of(fluid);
        }

        return FluidResource.EMPTY;
    }

    private static boolean matches(KeyAmount ka, FluidResource resource, StackHandler storage)
    {
        if (ka.isEmpty() || resource.isEmpty()) return false;

        Object cached = storage.getOutStackByKey(ka.key());
        if (cached instanceof FluidStack fluid && !fluid.isEmpty())
        {
            return resource.matches(fluid);
        }

        Object stack = ka.key().copyStack();
        return stack instanceof FluidStack fluid && !fluid.isEmpty() && resource.matches(fluid);
    }

    private static FluidStackKey toKey(FluidResource resource)
    {
        return new FluidStackKey(resource.toStack(1));
    }

    @Override
    public int size()
    {
        return fluidCount() + emptyCount();
    }

    @Override
    public FluidResource getResource(int index)
    {
        Objects.checkIndex(index, size());
        if (!inFluidRegion(index)) return FluidResource.EMPTY;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return FluidResource.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        return toResource(ka, handlerStorage);
    }

    @Override
    public long getAmountAsLong(int index)
    {
        Objects.checkIndex(index, size());
        if (!inFluidRegion(index)) return 0L;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0L;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        return ka.isEmpty() ? 0L : Math.max(0L, ka.amount());
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource)
    {
        Objects.checkIndex(index, size());

        if (!resource.isEmpty() && !isValid(index, resource))
        {
            return 0L;
        }

        if (!inFluidRegion(index))
        {
            return DEFAULT_TANK_CAPACITY;
        }

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0)
        {
            return DEFAULT_TANK_CAPACITY;
        }

        long byType = resource.isEmpty() ? DEFAULT_TANK_CAPACITY : toKey(resource).getVanillaMaxStackSize();
        long byCap = handlerStorage.getSlotCapacity(actualIndex);
        return Math.max(0L, Math.min(byType, byCap));
    }

    @Override
    public boolean isValid(int index, FluidResource resource)
    {
        Objects.checkIndex(index, size());
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
    public int insert(int index, FluidResource resource, int amount, @NotNull TransactionContext transaction)
    {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) return 0;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0;

        FluidStackKey key = toKey(resource);
        KeyAmount simulatedLeft = handlerStorage.insert(actualIndex, key, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        updateSnapshots(transaction);

        KeyAmount left = handlerStorage.insert(actualIndex, key, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, @NotNull TransactionContext transaction)
    {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) return 0;
        if (!inFluidRegion(index)) return 0;

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
