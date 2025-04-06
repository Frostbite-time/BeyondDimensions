package com.wintercogs.beyonddimensions.DataBase.Storage.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.PigmentStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import mekanism.api.Action;
import mekanism.api.chemical.pigment.IPigmentHandler;
import mekanism.api.chemical.pigment.PigmentStack;

import java.util.List;

public class PigmentUnifiedStorageHandler implements IPigmentHandler
{

    private UnifiedStorage storage;

    public PigmentUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        List<Integer> slots = storage.getTypeIdIndexList(PigmentStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public PigmentStack getChemicalInTank(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = storage.getTypeIdIndexList(PigmentStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (PigmentStack) storage.getStackBySlot(actualIndex).getStack();
        }
        else return PigmentStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, PigmentStack stack)
    {
        // 凡通过handler机械化输入的物品无论以何方法，全部为自动插入
        if(stack.isEmpty())
            return ;
        storage.insert(new PigmentStackType(stack.copy()), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, PigmentStack stack)
    {
        return true;
    }

    // 返回剩余量，与Fluid的返回插入量不同
    @Override
    public PigmentStack insertChemical(int tank, PigmentStack stack, Action action)
    {
        if(stack.isEmpty())
            return PigmentStack.EMPTY;
        long remaining = storage.insert(new PigmentStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new PigmentStack(stack, remaining);
        return PigmentStack.EMPTY;// 始终全部插入
    }

    // 尝试从指定槽位提取指定数量化学品
    @Override
    public PigmentStack extractChemical(int tank, long amount, Action action)
    {
        return ((PigmentStackType)storage.extract(new PigmentStackType(new PigmentStack(getChemicalInTank(tank),amount)),action.simulate()))
                .copyStack();
    }

    @Override
    public PigmentStack insertChemical(PigmentStack stack, Action action)
    {
        if(stack.isEmpty())
            return PigmentStack.EMPTY;
        long remaining = storage.insert(new PigmentStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new PigmentStack(stack, remaining);
        return PigmentStack.EMPTY;// 始终全部插入
    }

    // 从第一个槽位提取指定化学品
    @Override
    public PigmentStack extractChemical(long amount, Action action)
    {
        return ((PigmentStackType)storage.extract(new PigmentStackType( new PigmentStack(getChemicalInTank(0),amount)),action.simulate()))
                .copyStack();
    }

    // 按类型提取化学品
    @Override
    public PigmentStack extractChemical(PigmentStack stack, Action action)
    {
        return ((PigmentStackType)storage.extract(new PigmentStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
