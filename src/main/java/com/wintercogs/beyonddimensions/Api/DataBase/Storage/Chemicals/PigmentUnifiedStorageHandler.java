package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackKey;
import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;
import org.jetbrains.annotations.NotNull;

public class PigmentUnifiedStorageHandler implements IPigmentHandler
{

    private final UnifiedStorage storage;

    public PigmentUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getBucket(PigmentStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size() + 1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public @NotNull PigmentStack getChemicalInTank(int slot)
    {
        return storage.getBucket(PigmentStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> {
                    Object outStack = storage.getOutStackByKey(key);
                    if (outStack instanceof PigmentStack pigmentStack)
                    {
                        if (!pigmentStack.isEmpty())
                            pigmentStack.setAmount(storage.getStackByKey(key).amount());
                        return pigmentStack;
                    }
                    return null;
                })
                .orElse(PigmentStack.EMPTY);
    }

    @Override
    public void setChemicalInTank(int tank, PigmentStack stack)
    {
        if (stack.isEmpty())
            return;
        storage.insert(new PigmentStackKey(stack), stack.getAmount(), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public boolean isValid(int tank, @NotNull PigmentStack stack)
    {
        return true;
    }

    @Override
    public @NotNull PigmentStack insertChemical(int tank, PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return PigmentStack.EMPTY;
        long remaining = storage.insert(new PigmentStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new PigmentStack(stack, remaining);
        return PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack extractChemical(int tank, long amount, Action action)
    {
        if (storage.extract(new PigmentStackKey(getChemicalInTank(tank)), amount, action.simulate(), false).toStack() instanceof PigmentStack result)
            return result;
        else
            return PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack insertChemical(PigmentStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return PigmentStack.EMPTY;
        long remaining = storage.insert(new PigmentStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new PigmentStack(stack, remaining);
        return PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack extractChemical(long amount, Action action)
    {
        if (storage.extract(new PigmentStackKey(getChemicalInTank(0)), amount, action.simulate(), false).toStack() instanceof PigmentStack result)
            return result;
        else
            return PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack extractChemical(@NotNull PigmentStack stack, Action action)
    {
        if (storage.extract(new PigmentStackKey(stack), stack.getAmount(), action.simulate(), false).toStack() instanceof PigmentStack result)
            return result;
        else
            return PigmentStack.EMPTY;
    }

    @Override
    public @NotNull PigmentStack getEmptyStack()
    {
        return PigmentStack.EMPTY;
    }
}
