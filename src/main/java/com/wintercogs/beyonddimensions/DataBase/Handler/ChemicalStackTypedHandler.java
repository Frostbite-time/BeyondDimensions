package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;

import java.util.List;

public class ChemicalStackTypedHandler implements IGasHandler
{

    private StackTypedHandler handlerStorage;

    public ChemicalStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getTanks()
    {
        List<Integer> slots = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public GasStack getChemicalInTank(int tank)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID);
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
        actualIndex = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(tank);
        if(actualIndex >= 0)
            handlerStorage.setStackDirectly(actualIndex,new ChemicalStackType(stack.copy()));
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
        long remaining = handlerStorage.insert(handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(tank),new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;
    }

    @Override
    public GasStack extractChemical(int tank, long amount, Action action)
    {
        return ((ChemicalStackType)handlerStorage.extract(handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(tank),amount,action.simulate()))
                .copyStack();
    }

    @Override
    public GasStack insertChemical(GasStack stack, Action action)
    {
        if(stack.isEmpty())
            return GasStack.EMPTY;
        long remaining = handlerStorage.insert(new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;// 始终全部插入
    }

    @Override
    public GasStack extractChemical(long amount, Action action)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(0);
        return ((ChemicalStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),action.simulate()))
                .copyStack();
    }

    @Override
    public GasStack extractChemical(GasStack stack, Action action)
    {
        return ((ChemicalStackType)handlerStorage.extract(new ChemicalStackType(stack.copy()),action.simulate()))
                .copyStack();
    }

    @Override
    public GasStack getEmptyStack()
    {
        return GasStack.EMPTY;
    }
}
