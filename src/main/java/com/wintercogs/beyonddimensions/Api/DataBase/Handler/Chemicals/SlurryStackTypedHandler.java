package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;

import java.util.List;

public class SlurryStackTypedHandler implements ISlurryHandler
{

    private StackTypedHandler handlerStorage;

    public SlurryStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getTanks()
    {
        return handlerStorage.getTypeIdIndexList(SlurryStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public SlurryStack getChemicalInTank(int tank)
    {
        return handlerStorage.getTypeIdIndexList(SlurryStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(obj -> (SlurryStack) obj.getStack())
                .orElse(SlurryStack.EMPTY);
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, SlurryStack stack)
    {
        handlerStorage.getTypeIdIndexList(SlurryStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .ifPresent(actualIndex ->
                        handlerStorage.setStackDirectly(actualIndex, new SlurryStackType(stack.copy()))
                );
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64000L;
    }

    @Override
    public boolean isValid(int tank, SlurryStack stack)
    {
        return true;
    }

    @Override
    public SlurryStack insertChemical(int tank, SlurryStack stack, Action action)
    {
        if (stack.isEmpty()) return SlurryStack.EMPTY;
        return handlerStorage.getTypeIdIndexList(SlurryStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> {
                    IStackType remainingStack = handlerStorage.insert(
                            actualIndex,
                            new SlurryStackType(stack.copy()),
                            action.simulate()
                    );
                    long remaining = remainingStack.getStackAmount();
                    return (remaining > 0) ? new SlurryStack(stack,remaining): SlurryStack.EMPTY;
                })
                .orElse(stack.copy());
    }

    @Override
    public SlurryStack extractChemical(int tank, long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(SlurryStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((SlurryStackType)extracts).copyStack())
                .orElse(SlurryStack.EMPTY);
    }

    @Override
    public SlurryStack insertChemical(SlurryStack stack, Action action)
    {
        if(stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = handlerStorage.insert(new SlurryStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;
    }

    @Override
    public SlurryStack extractChemical(long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(SlurryStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> stack.copy())
                .map(stack -> handlerStorage.extract(stack, action.simulate()))
                .map(extracts -> ((SlurryStackType)extracts).copyStack())
                .orElse(SlurryStack.EMPTY);
    }

    @Override
    public SlurryStack extractChemical(SlurryStack stack, Action action)
    {
        return ((SlurryStackType)handlerStorage.extract(new SlurryStackType(stack.copy()),action.simulate()))
                .copyStack();
    }

    @Override
    public SlurryStack getEmptyStack()
    {
        return SlurryStack.EMPTY;
    }
}
