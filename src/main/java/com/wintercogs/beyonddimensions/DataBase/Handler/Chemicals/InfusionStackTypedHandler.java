package com.wintercogs.beyonddimensions.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.InfusionStackType;
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
        List<Integer> slots = handlerStorage.getTypeIdIndexList(InfusionStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public InfusionStack getChemicalInTank(int tank)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = handlerStorage.getTypeIdIndexList(InfusionStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=tank && tank < slots.size())
        {
            actualIndex = slots.get(tank);
        }

        if(actualIndex != -1)
        {
            return (InfusionStack) handlerStorage.getStackBySlot(actualIndex).getStack();
        }
        else return InfusionStack.EMPTY;
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, InfusionStack stack)
    {
        int actualIndex = -1;
        actualIndex = handlerStorage.getTypeIdIndexList(InfusionStackType.ID).get(tank);
        if(actualIndex >= 0)
            handlerStorage.setStackDirectly(actualIndex,new InfusionStackType(stack.copy()));
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
        if(stack.isEmpty())
            return InfusionStack.EMPTY;
        long remaining = handlerStorage.insert(handlerStorage.getTypeIdIndexList(InfusionStackType.ID).get(tank),new InfusionStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new InfusionStack(stack, remaining);
        return InfusionStack.EMPTY;
    }

    @Override
    public InfusionStack extractChemical(int tank, long amount, Action action)
    {
        return ((InfusionStackType)handlerStorage.extract(handlerStorage.getTypeIdIndexList(InfusionStackType.ID).get(tank),amount,action.simulate()))
                .copyStack();
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
        int actualIndex = handlerStorage.getTypeIdIndexList(InfusionStackType.ID).get(0);
        return ((InfusionStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),action.simulate()))
                .copyStack();
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
