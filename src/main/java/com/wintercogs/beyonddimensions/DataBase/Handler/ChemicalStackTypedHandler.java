package com.wintercogs.beyonddimensions.DataBase.Handler;

import com.wintercogs.beyonddimensions.DataBase.Stack.ChemicalStackType;
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
        List<Integer> slots = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank)
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
            return (ChemicalStack) handlerStorage.getStackBySlot(actualIndex).getStack();
        }
        else return ChemicalStack.EMPTY;
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack)
    {
        int actualIndex = -1;
        actualIndex = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(tank);
        if(actualIndex >= 0)
            handlerStorage.setStackDirectly(actualIndex,new ChemicalStackType(stack.copy()));
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
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = handlerStorage.insert(handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(tank),new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action)
    {
        return ((ChemicalStackType)handlerStorage.extract(handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).get(tank),amount,action.simulate()))
                .copyStack();
    }

    @Override
    public ChemicalStack insertChemical(ChemicalStack stack, Action action)
    {
        if(stack.isEmpty())
            return ChemicalStack.EMPTY;
        long remaining = handlerStorage.insert(new ChemicalStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return stack.copyWithAmount(remaining);
        return ChemicalStack.EMPTY;// 始终全部插入
    }

    @Override
    public ChemicalStack extractChemical(long amount, Action action)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(ChemicalStackType.ID).getFirst();
        return ((ChemicalStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),action.simulate()))
                .copyStack();
    }

    @Override
    public ChemicalStack extractChemical(ChemicalStack stack, Action action)
    {
        return ((ChemicalStackType)handlerStorage.extract(new ChemicalStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
