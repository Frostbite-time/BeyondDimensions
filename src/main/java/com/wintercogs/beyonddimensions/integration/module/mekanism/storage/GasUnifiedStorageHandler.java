package com.wintercogs.beyonddimensions.integration.module.mekanism.storage;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;
import org.jetbrains.annotations.NotNull;

public class GasUnifiedStorageHandler implements IGasHandler
{

    private final UnifiedStorage storage;

    public GasUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getBucket(GasStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size() + 1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public @NotNull GasStack getChemicalInTank(int slot)
    {
        return storage.getBucket(GasStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> {
                    Object outStack = storage.getOutStackByKey(key);
                    if (outStack instanceof GasStack gasStack)
                    {
                        if (!gasStack.isEmpty())
                            gasStack.setAmount(storage.getStackByKey(key).amount());
                        return gasStack;
                    }
                    return null;
                })
                .orElse(GasStack.EMPTY);
    }

    @Override
    public void setChemicalInTank(int tank, GasStack stack)
    {
        if (stack.isEmpty())
            return;
        storage.insert(new GasStackKey(stack), stack.getAmount(), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public boolean isValid(int tank, @NotNull GasStack stack)
    {
        return true;
    }

    @Override
    public @NotNull GasStack insertChemical(int tank, GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return GasStack.EMPTY;
        long remaining = storage.insert(new GasStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;
    }

    @Override
    public @NotNull GasStack extractChemical(int tank, long amount, Action action)
    {
        if (storage.extract(new GasStackKey(getChemicalInTank(tank)), amount, action.simulate(), false).toStack() instanceof GasStack result)
            return result;
        else
            return GasStack.EMPTY;
    }

    @Override
    public @NotNull GasStack insertChemical(GasStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return GasStack.EMPTY;
        long remaining = storage.insert(new GasStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;
    }

    @Override
    public @NotNull GasStack extractChemical(long amount, Action action)
    {
        if (storage.extract(new GasStackKey(getChemicalInTank(0)), amount, action.simulate(), false).toStack() instanceof GasStack result)
            return result;
        else
            return GasStack.EMPTY;
    }

    @Override
    public @NotNull GasStack extractChemical(@NotNull GasStack stack, Action action)
    {
        if (storage.extract(new GasStackKey(stack), stack.getAmount(), action.simulate(), false).toStack() instanceof GasStack result)
            return result;
        else
            return GasStack.EMPTY;
    }

    @Override
    public @NotNull GasStack getEmptyStack()
    {
        return GasStack.EMPTY;
    }
}