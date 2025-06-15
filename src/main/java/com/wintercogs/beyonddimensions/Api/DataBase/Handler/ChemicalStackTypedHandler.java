package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ChemicalStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import java.util.List;

public class ChemicalStackTypedHandler implements IChemicalHandler
{

    private StackTypedHandler handlerStorage;

    public ChemicalStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getChemicalTanks()
    {
        return handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank)
    {
        return handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(obj -> (ChemicalStack) obj.getStack())
                .orElse(ChemicalStack.EMPTY);
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack)
    {
        handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .ifPresent(actualIndex ->
                        handlerStorage.setStackDirectly(actualIndex, new ChemicalStackType(stack.copy()))
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
        return handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> {
                    IStackType remainingStack = handlerStorage.insert(
                            actualIndex,
                            new ChemicalStackType(stack.copy()),
                            action.simulate()
                    );
                    long remaining = remainingStack.getStackAmount();
                    return (remaining > 0) ? stack.copyWithAmount(remaining) : ChemicalStack.EMPTY;
                })
                .orElse(stack.copy());
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((ChemicalStackType)extracts).copyStack())
                .orElse(ChemicalStack.EMPTY);
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action)
    {
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = handlerStorage.insert(new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(ChemicalStackType.ID)
                .map(slots -> slots.getFirst())
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> stack.copy())
                .map(stack -> handlerStorage.extract(stack, action.simulate()))
                .map(extracts -> ((ChemicalStackType)extracts).copyStack())
                .orElse(ChemicalStack.EMPTY);
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action)
    {
        return ((ChemicalStackType)handlerStorage.extract(new ChemicalStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
