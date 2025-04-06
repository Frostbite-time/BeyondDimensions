package com.wintercogs.beyonddimensions.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.GasStackType;
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
        List<Integer> slots = handlerStorage.getTypeIdIndexList(GasStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public GasStack getChemicalInTank(int tank)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = handlerStorage.getTypeIdIndexList(GasStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=tank && tank < slots.size())
        {
            actualIndex = slots.get(tank);
        }

        if(actualIndex != -1)
        {
            return (GasStack) handlerStorage.getStackBySlot(actualIndex).getStack();
        }
        else return GasStack.EMPTY;
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, GasStack stack)
    {
        int actualIndex = -1;
        actualIndex = handlerStorage.getTypeIdIndexList(GasStackType.ID).get(tank);
        if(actualIndex >= 0)
            handlerStorage.setStackDirectly(actualIndex,new GasStackType(stack.copy()));
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
        if(stack.isEmpty())
            return GasStack.EMPTY;
        long remaining = handlerStorage.insert(handlerStorage.getTypeIdIndexList(GasStackType.ID).get(tank),new GasStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;
    }

    @Override
    public GasStack extractChemical(int tank, long amount, Action action)
    {
        return ((GasStackType)handlerStorage.extract(handlerStorage.getTypeIdIndexList(GasStackType.ID).get(tank),amount,action.simulate()))
                .copyStack();
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
        int actualIndex = handlerStorage.getTypeIdIndexList(GasStackType.ID).get(0);
        return ((GasStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),action.simulate()))
                .copyStack();
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
