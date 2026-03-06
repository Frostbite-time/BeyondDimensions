package com.wintercogs.beyonddimensions.integration.module.mekanism.storage;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;
import org.jetbrains.annotations.NotNull;

public class InfusionUnifiedStorageHandler implements IInfusionHandler
{

    private final UnifiedStorage storage;

    public InfusionUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getBucket(InfusionStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size() + 1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public @NotNull InfusionStack getChemicalInTank(int slot)
    {
        return storage.getBucket(InfusionStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .map(key -> {
                    Object outStack = storage.getOutStackByKey(key);
                    if (outStack instanceof InfusionStack infuseStack)
                    {
                        if (!infuseStack.isEmpty())
                            infuseStack.setAmount(storage.getStackByKey(key).amount());
                        return infuseStack;
                    }
                    return null;
                })
                .orElse(InfusionStack.EMPTY);
    }

    @Override
    public void setChemicalInTank(int tank, InfusionStack stack)
    {
        if (stack.isEmpty())
            return;
        storage.insert(new InfusionStackKey(stack), stack.getAmount(), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public boolean isValid(int tank, @NotNull InfusionStack stack)
    {
        return true;
    }

    @Override
    public @NotNull InfusionStack insertChemical(int tank, InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return InfusionStack.EMPTY;
        long remaining = storage.insert(new InfusionStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new InfusionStack(stack, remaining);
        return InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack extractChemical(int tank, long amount, Action action)
    {
        if (storage.extract(new InfusionStackKey(getChemicalInTank(tank)), amount, action.simulate(), false).toStack() instanceof InfusionStack result)
            return result;
        else
            return InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack insertChemical(InfusionStack stack, @NotNull Action action)
    {
        if (stack.isEmpty())
            return InfusionStack.EMPTY;
        long remaining = storage.insert(new InfusionStackKey(stack), stack.getAmount(), action.simulate()).amount();
        if (remaining > 0)
            return new InfusionStack(stack, remaining);
        return InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack extractChemical(long amount, Action action)
    {
        if (storage.extract(new InfusionStackKey(getChemicalInTank(0)), amount, action.simulate(), false).toStack() instanceof InfusionStack result)
            return result;
        else
            return InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack extractChemical(@NotNull InfusionStack stack, Action action)
    {
        if (storage.extract(new InfusionStackKey(stack), stack.getAmount(), action.simulate(), false).toStack() instanceof InfusionStack result)
            return result;
        else
            return InfusionStack.EMPTY;
    }

    @Override
    public @NotNull InfusionStack getEmptyStack()
    {
        return InfusionStack.EMPTY;
    }
}
