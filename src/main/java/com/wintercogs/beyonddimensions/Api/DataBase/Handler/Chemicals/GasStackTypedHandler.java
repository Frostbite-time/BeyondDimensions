package com.wintercogs.beyonddimensions.Api.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.GasStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;

import java.util.List;

public class GasStackTypedHandler implements IGasHandler
{

    private StackTypedHandler handlerStorage;

    public GasStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getTanks()
    {
        return handlerStorage.getTypeIdIndexList(GasStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public GasStack getChemicalInTank(int tank)
    {
        return handlerStorage.getTypeIdIndexList(GasStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(obj -> (GasStack) obj.getStack())
                .orElse(GasStack.EMPTY);
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, GasStack stack)
    {
        handlerStorage.getTypeIdIndexList(GasStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .ifPresent(actualIndex ->
                        handlerStorage.setStackDirectly(actualIndex, new GasStackType(stack.copy()))
                );
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64000L;
    }

    @Override
    public boolean isValid(int tank, GasStack stack)
    {
        return true;
    }

    @Override
    public GasStack insertChemical(int tank, GasStack stack, Action action)
    {
        if (stack.isEmpty()) return GasStack.EMPTY;
        return handlerStorage.getTypeIdIndexList(GasStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> {
                    IStackType remainingStack = handlerStorage.insert(
                            actualIndex,
                            new GasStackType(stack.copy()),
                            action.simulate()
                    );
                    long remaining = remainingStack.getStackAmount();
                    return (remaining > 0) ? new GasStack(stack,remaining): GasStack.EMPTY;
                })
                .orElse(stack.copy());
    }

    @Override
    public GasStack extractChemical(int tank, long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(GasStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((GasStackType)extracts).copyStack())
                .orElse(GasStack.EMPTY);
    }

    @Override
    public GasStack insertChemical(GasStack stack, Action action)
    {
        if(stack.isEmpty())
            return GasStack.EMPTY;
        long remaining = handlerStorage.insert(new GasStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;// 始终全部插入
    }

    @Override
    public GasStack extractChemical(long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(GasStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> stack.copy())
                .map(stack -> handlerStorage.extract(stack, action.simulate()))
                .map(extracts -> ((GasStackType)extracts).copyStack())
                .orElse(GasStack.EMPTY);
    }

    @Override
    public GasStack extractChemical(GasStack stack, Action action)
    {
        return ((GasStackType)handlerStorage.extract(new GasStackType(stack.copy()),action.simulate()))
                .copyStack();
    }

    @Override
    public GasStack getEmptyStack()
    {
        return GasStack.EMPTY;
    }
}
