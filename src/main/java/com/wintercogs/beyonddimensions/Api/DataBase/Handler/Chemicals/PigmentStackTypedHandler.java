package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.PigmentStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;

import java.util.List;

public class PigmentStackTypedHandler implements IPigmentHandler
{

    private StackTypedHandler handlerStorage;

    public PigmentStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getTanks()
    {
        return handlerStorage.getTypeIdIndexList(PigmentStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public PigmentStack getChemicalInTank(int tank)
    {
        return handlerStorage.getTypeIdIndexList(PigmentStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(obj -> (PigmentStack) obj.getStack())
                .orElse(PigmentStack.EMPTY);
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, PigmentStack stack)
    {
        handlerStorage.getTypeIdIndexList(PigmentStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .ifPresent(actualIndex ->
                        handlerStorage.setStackDirectly(actualIndex, new PigmentStackType(stack.copy()))
                );
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64000L;
    }

    @Override
    public boolean isValid(int tank, PigmentStack stack)
    {
        return true;
    }

    @Override
    public PigmentStack insertChemical(int tank, PigmentStack stack, Action action)
    {
        if (stack.isEmpty()) return PigmentStack.EMPTY;
        return handlerStorage.getTypeIdIndexList(PigmentStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> {
                    IStackType remainingStack = handlerStorage.insert(
                            actualIndex,
                            new PigmentStackType(stack.copy()),
                            action.simulate()
                    );
                    long remaining = remainingStack.getStackAmount();
                    return (remaining > 0) ? new PigmentStack(stack,remaining): PigmentStack.EMPTY;
                })
                .orElse(stack.copy());
    }

    @Override
    public PigmentStack extractChemical(int tank, long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(PigmentStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((PigmentStackType)extracts).copyStack())
                .orElse(PigmentStack.EMPTY);
    }

    @Override
    public PigmentStack insertChemical(PigmentStack stack, Action action)
    {
        if(stack.isEmpty())
            return PigmentStack.EMPTY;
        long remaining = handlerStorage.insert(new PigmentStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new PigmentStack(stack, remaining);
        return PigmentStack.EMPTY;
    }

    @Override
    public PigmentStack extractChemical(long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(PigmentStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> stack.copy())
                .map(stack -> handlerStorage.extract(stack, action.simulate()))
                .map(extracts -> ((PigmentStackType)extracts).copyStack())
                .orElse(PigmentStack.EMPTY);
    }

    @Override
    public PigmentStack extractChemical(PigmentStack stack, Action action)
    {
        return ((PigmentStackType)handlerStorage.extract(new PigmentStackType(stack.copy()),action.simulate()))
                .copyStack();
    }

    @Override
    public PigmentStack getEmptyStack()
    {
        return PigmentStack.EMPTY;
    }
}
