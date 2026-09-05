package com.wintercogs.beyonddimensions.api.capability.helper.ordered;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * 仅基于桶（Fluid 桶 + Empty 桶）进行索引映射的 Fluid 资源视图。
 * 动态槽位：size() 随存储内容实时变化，调用方持有的索引可能在两次调用间失效，
 * 因此对越界索引不抛错：读取返回 EMPTY/0，插入与提取返回 0，isValid 返回 false。
 */
public class FluidStackTypedHandler implements ResourceHandler<@NotNull FluidResource>
{
    private static final Identifier FLUID_TYPE = FluidStackKey.ID;
    private static final long DEFAULT_TANK_CAPACITY = 64000L;

    private final StackHandler handlerStorage;
    private final ArrayList<StackJournal> snapshotJournals;

    public FluidStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
        this.snapshotJournals = new ArrayList<>(handlerStorage.getSlots());
        for (int i = 0; i < handlerStorage.getSlots(); i++)
        {
            snapshotJournals.add(new StackJournal(i));
        }
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
        if (!inFluidRegion(index)) return FluidResource.EMPTY;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return FluidResource.EMPTY;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        return toResource(ka, handlerStorage);
    }

    @Override
    public long getAmountAsLong(int index)
    {
        if (!inFluidRegion(index)) return 0L;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0L;

        KeyAmount ka = handlerStorage.getStackBySlot(actualIndex);
        return ka.isEmpty() ? 0L : Math.max(0L, ka.amount());
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource)
    {
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
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) return 0;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0;

        FluidStackKey key = toKey(resource);
        KeyAmount simulatedLeft = handlerStorage.insert(actualIndex, key, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        snapshotJournals.get(actualIndex).updateSnapshots(transaction);

        KeyAmount left = handlerStorage.insert(actualIndex, key, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (amount == 0) return 0;
        if (!inFluidRegion(index)) return 0;

        int actualIndex = resolveActualIndex(index);
        if (actualIndex < 0) return 0;

        KeyAmount current = handlerStorage.getStackBySlot(actualIndex);
        if (!matches(current, resource, handlerStorage)) return 0;

        KeyAmount simulated = handlerStorage.extract(actualIndex, amount, true);
        if (simulated.isEmpty() || simulated.amount() <= 0L) return 0;

        snapshotJournals.get(actualIndex).updateSnapshots(transaction);

        KeyAmount taken = handlerStorage.extract(actualIndex, amount, false);
        return BDMath.clampLongToInt(Math.max(0L, taken.amount()));
    }

    // ---- Per-slot journal for transaction support ----

    private class StackJournal extends SnapshotJournal<KeyAmount>
    {
        private final int slotIndex;

        StackJournal(int slotIndex)
        {
            this.slotIndex = slotIndex;
        }

        @Override
        protected KeyAmount createSnapshot()
        {
            return handlerStorage.getStackBySlot(slotIndex);
        }

        @Override
        protected void revertToSnapshot(KeyAmount snapshot)
        {
            if (snapshot == null) return;
            handlerStorage.setStackDirectly(slotIndex, snapshot.key(), snapshot.amount());
        }
    }
}