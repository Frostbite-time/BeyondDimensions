package com.wintercogs.beyonddimensions.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.InfusionStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;

import java.util.List;

public class InfusionStackTypedHandler implements IInfusionHandler
{

    private StackTypedHandler handlerStorage;

    public InfusionStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getTanks()
    {
        return handlerStorage.getTypeIdIndexList(InfusionStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public InfusionStack getChemicalInTank(int tank)
    {
        return handlerStorage.getTypeIdIndexList(InfusionStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(obj -> (InfusionStack) obj.getStack())
                .orElse(InfusionStack.EMPTY);
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, InfusionStack stack)
    {
        handlerStorage.getTypeIdIndexList(InfusionStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .ifPresent(actualIndex ->
                        handlerStorage.setStackDirectly(actualIndex, new InfusionStackType(stack.copy()))
                );
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return 64000L;
    }

    @Override
    public boolean isValid(int tank, InfusionStack stack)
    {
        return true;
    }

    @Override
    public InfusionStack insertChemical(int tank, InfusionStack stack, Action action)
    {
        if (stack.isEmpty()) return InfusionStack.EMPTY;
        return handlerStorage.getTypeIdIndexList(InfusionStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> {
                    IStackType remainingStack = handlerStorage.insert(
                            actualIndex,
                            new InfusionStackType(stack.copy()),
                            action.simulate()
                    );
                    long remaining = remainingStack.getStackAmount();
                    return (remaining > 0) ? new InfusionStack(stack,remaining): InfusionStack.EMPTY;
                })
                .orElse(stack.copy());
    }

    @Override
    public InfusionStack extractChemical(int tank, long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(InfusionStackType.ID)
                .filter(slots -> tank >= 0 && tank < slots.size())
                .map(slots -> slots.get(tank))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> handlerStorage.extract(actualIndex, amount, action.simulate()))
                .map(extracts -> ((InfusionStackType)extracts).copyStack())
                .orElse(InfusionStack.EMPTY);
    }

    @Override
    public InfusionStack insertChemical(InfusionStack stack, Action action)
    {
        if(stack.isEmpty())
            return InfusionStack.EMPTY;
        long remaining = handlerStorage.insert(new InfusionStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new InfusionStack(stack, remaining);
        return InfusionStack.EMPTY;// 始终全部插入
    }

    @Override
    public InfusionStack extractChemical(long amount, Action action)
    {
        return handlerStorage.getTypeIdIndexList(InfusionStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> stack.copy())
                .map(stack -> handlerStorage.extract(stack, action.simulate()))
                .map(extracts -> ((InfusionStackType)extracts).copyStack())
                .orElse(InfusionStack.EMPTY);
    }

    @Override
    public InfusionStack extractChemical(InfusionStack stack, Action action)
    {
        return ((InfusionStackType)handlerStorage.extract(new InfusionStackType(stack.copy()),action.simulate()))
                .copyStack();
    }

    @Override
    public InfusionStack getEmptyStack()
    {
        return InfusionStack.EMPTY;
    }
}
