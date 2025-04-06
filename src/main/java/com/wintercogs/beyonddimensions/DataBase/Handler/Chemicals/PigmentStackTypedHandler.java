package com.wintercogs.beyonddimensions.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.PigmentStackType;
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
        List<Integer> slots = handlerStorage.getTypeIdIndexList(PigmentStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public PigmentStack getChemicalInTank(int tank)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = handlerStorage.getTypeIdIndexList(PigmentStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=tank && tank < slots.size())
        {
            actualIndex = slots.get(tank);
        }

        if(actualIndex != -1)
        {
            return (PigmentStack) handlerStorage.getStackBySlot(actualIndex).getStack();
        }
        else return PigmentStack.EMPTY;
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, PigmentStack stack)
    {
        int actualIndex = -1;
        actualIndex = handlerStorage.getTypeIdIndexList(PigmentStackType.ID).get(tank);
        if(actualIndex >= 0)
            handlerStorage.setStackDirectly(actualIndex,new PigmentStackType(stack.copy()));
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
        if(stack.isEmpty())
            return PigmentStack.EMPTY;
        long remaining = handlerStorage.insert(handlerStorage.getTypeIdIndexList(PigmentStackType.ID).get(tank),new PigmentStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new PigmentStack(stack, remaining);
        return PigmentStack.EMPTY;
    }

    @Override
    public PigmentStack extractChemical(int tank, long amount, Action action)
    {
        return ((PigmentStackType)handlerStorage.extract(handlerStorage.getTypeIdIndexList(PigmentStackType.ID).get(tank),amount,action.simulate()))
                .copyStack();
    }

    @Override
    public PigmentStack insertChemical(PigmentStack stack, Action action)
    {
        if(stack.isEmpty())
            return PigmentStack.EMPTY;
        long remaining = handlerStorage.insert(new PigmentStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new PigmentStack(stack, remaining);
        return PigmentStack.EMPTY;// 始终全部插入
    }

    @Override
    public PigmentStack extractChemical(long amount, Action action)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(PigmentStackType.ID).get(0);
        return ((PigmentStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),action.simulate()))
                .copyStack();
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
