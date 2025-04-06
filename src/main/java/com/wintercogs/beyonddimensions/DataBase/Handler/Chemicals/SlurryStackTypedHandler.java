package com.wintercogs.beyonddimensions.DataBase.Handler.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.SlurryStackType;
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
        List<Integer> slots = handlerStorage.getTypeIdIndexList(SlurryStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public SlurryStack getChemicalInTank(int tank)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = handlerStorage.getTypeIdIndexList(SlurryStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=tank && tank < slots.size())
        {
            actualIndex = slots.get(tank);
        }

        if(actualIndex != -1)
        {
            return (SlurryStack) handlerStorage.getStackBySlot(actualIndex).getStack();
        }
        else return SlurryStack.EMPTY;
    }

    // 直接设置指定槽位化学品
    @Override
    public void setChemicalInTank(int tank, SlurryStack stack)
    {
        int actualIndex = -1;
        actualIndex = handlerStorage.getTypeIdIndexList(SlurryStackType.ID).get(tank);
        if(actualIndex >= 0)
            handlerStorage.setStackDirectly(actualIndex,new SlurryStackType(stack.copy()));
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
        if(stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = handlerStorage.insert(handlerStorage.getTypeIdIndexList(SlurryStackType.ID).get(tank),new SlurryStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;
    }

    @Override
    public SlurryStack extractChemical(int tank, long amount, Action action)
    {
        return ((SlurryStackType)handlerStorage.extract(handlerStorage.getTypeIdIndexList(SlurryStackType.ID).get(tank),amount,action.simulate()))
                .copyStack();
    }

    @Override
    public SlurryStack insertChemical(SlurryStack stack, Action action)
    {
        if(stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = handlerStorage.insert(new SlurryStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;// 始终全部插入
    }

    @Override
    public SlurryStack extractChemical(long amount, Action action)
    {
        int actualIndex = handlerStorage.getTypeIdIndexList(SlurryStackType.ID).get(0);
        return ((SlurryStackType)handlerStorage.extract(handlerStorage.getStackBySlot(actualIndex).copy(),action.simulate()))
                .copyStack();
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
