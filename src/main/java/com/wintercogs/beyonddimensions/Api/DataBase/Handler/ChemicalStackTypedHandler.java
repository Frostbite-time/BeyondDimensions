package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import org.jetbrains.annotations.NotNull;

public class ChemicalStackTypedHandler implements IChemicalHandler
{

    private final StackHandler handlerStorage;

    public ChemicalStackTypedHandler(StackHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getChemicalTanks()
    {
        return handlerStorage.getBucket(ChemicalStackKey.ID)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    @Override
    public @NotNull ChemicalStack getChemicalInTank(int slot)
    {
        return handlerStorage.getBucket(ChemicalStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(handlerStorage::getStackBySlot)
                .map(stack -> {
                    Object outStack = handlerStorage.getOutStackByKey(stack.key());
                    if(outStack instanceof ChemicalStack chemicalStack)
                    {
                        if(!chemicalStack.isEmpty())
                            chemicalStack.setAmount(stack.amount());
                        return chemicalStack;
                    }
                    return null;
                })
                .orElse(ChemicalStack.EMPTY);
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, @NotNull ChemicalStack stack)
    {
        handlerStorage.getBucket(ChemicalStackKey.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .ifPresent(actualIndex ->
                        handlerStorage.setStackDirectly(actualIndex, new ChemicalStackKey(stack), stack.getAmount())
                );
    }

    @Override
    public long getChemicalTankCapacity(int tank)
    {
        return 64000L;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack)
    {
        return true;
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action)
    {
        if (stack.isEmpty()) return ChemicalStack.EMPTY;
        return handlerStorage.getBucket(ChemicalStackKey.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> {
                    KeyAmount remainingStack = handlerStorage.insert(actualIndex, new ChemicalStackKey(stack), stack.getAmount(),action.simulate());
                    long remaining = remainingStack.amount();
                    return (remaining > 0) ? stack.copyWithAmount(remaining) : ChemicalStack.EMPTY;
                })
                .orElse(stack.copy());
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action)
    {
        return handlerStorage.getBucket(ChemicalStackKey.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> {
                    if(handlerStorage.extract(actualIndex, amount, action.simulate()).toStack() instanceof ChemicalStack outStack)
                        return outStack;
                    else
                        return ChemicalStack.EMPTY;
                })
                .orElse(ChemicalStack.EMPTY);
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action)
    {
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = handlerStorage.insert(new ChemicalStackKey(stack), stack.getAmount(),action.simulate()).amount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action)
    {
        return handlerStorage.getBucket(ChemicalStackKey.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> {
                    if(handlerStorage.extract(stack.key(), amount,action.simulate()).toStack() instanceof ChemicalStack outStack)
                        return outStack;
                    else
                        return ChemicalStack.EMPTY;
                })
                .orElse(ChemicalStack.EMPTY);
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action)
    {
        if(handlerStorage.extract(new ChemicalStackKey(stack), stack.getAmount(),action.simulate()).toStack() instanceof ChemicalStack outStack)
            return outStack;
        else
            return ChemicalStack.EMPTY;
    }
}
