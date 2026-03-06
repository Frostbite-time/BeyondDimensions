package com.wintercogs.beyonddimensions.api.capability.helper.unordered;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.SlurryStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;
import org.jetbrains.annotations.NotNull;

public class SlurryUnifiedStorageHandler implements ISlurryHandler
{

    private final UnifiedStorage storage;

    public SlurryUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getBucket(SlurryStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size() + 1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public @NotNull SlurryStack getChemicalInTank(int slot)
    {
        return storage.getBucket(SlurryStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> {
                    Object outStack = storage.getOutStackByKey(key);
                    if (outStack instanceof SlurryStack slurryStack)
                    {
                        if (!slurryStack.isEmpty())
                            slurryStack.setAmount(storage.getStackByKey(key).amount());
                        return slurryStack;
                    }
                    return null;
                })
                .orElse(SlurryStack.EMPTY);
    }

    @Override
    public void setChemicalInTank(int tank, SlurryStack stack)
    {
        if (stack.isEmpty())
            return;
        storage.insert(new SlurryStackKey(stack), stack.getAmount(), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public boolean isValid(int tank, @NotNull SlurryStack stack)
    {
        return true;
    }

    @Override
    public @NotNull SlurryStack insertChemical(int tank, SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = storage.insert(new SlurryStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;
    }

    @Override
    public @NotNull SlurryStack extractChemical(int tank, long amount, Action action)
    {
        if (storage.extract(new SlurryStackKey(getChemicalInTank(tank)), amount, action.simulate(), false).toStack() instanceof SlurryStack result)
            return result;
        else
            return SlurryStack.EMPTY;
    }

    @Override
    public @NotNull SlurryStack insertChemical(SlurryStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = storage.insert(new SlurryStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;
    }

    @Override
    public @NotNull SlurryStack extractChemical(long amount, Action action)
    {
        if (storage.extract(new SlurryStackKey(getChemicalInTank(0)), amount, action.simulate(), false).toStack() instanceof SlurryStack result)
            return result;
        else
            return SlurryStack.EMPTY;
    }

    @Override
    public @NotNull SlurryStack extractChemical(@NotNull SlurryStack stack, Action action)
    {
        if (storage.extract(new SlurryStackKey(stack), stack.getAmount(), action.simulate(), false).toStack() instanceof SlurryStack result)
            return result;
        else
            return SlurryStack.EMPTY;
    }

    @Override
    public @NotNull SlurryStack getEmptyStack()
    {
        return SlurryStack.EMPTY;
    }
}
