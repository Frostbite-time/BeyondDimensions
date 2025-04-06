package com.wintercogs.beyonddimensions.DataBase.Storage.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.SlurryStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;

import java.util.List;

public class SlurryUnifiedStorageHandler implements ISlurryHandler
{

    private UnifiedStorage storage;

    public SlurryUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        List<Integer> slots = storage.getTypeIdIndexList(SlurryStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public SlurryStack getChemicalInTank(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = storage.getTypeIdIndexList(SlurryStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (SlurryStack) storage.getStackBySlot(actualIndex).getStack();
        }
        else return SlurryStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, SlurryStack stack)
    {
        // 凡通过handler机械化输入的物品无论以何方法，全部为自动插入
        if(stack.isEmpty())
            return ;
        storage.insert(new SlurryStackType(stack.copy()), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, SlurryStack stack)
    {
        return true;
    }

    // 返回剩余量，与Fluid的返回插入量不同
    @Override
    public SlurryStack insertChemical(int tank, SlurryStack stack, Action action)
    {
        if(stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = storage.insert(new SlurryStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;// 始终全部插入
    }

    // 尝试从指定槽位提取指定数量化学品
    @Override
    public SlurryStack extractChemical(int tank, long amount, Action action)
    {
        return ((SlurryStackType)storage.extract(new SlurryStackType(new SlurryStack(getChemicalInTank(tank),amount)),action.simulate()))
                .copyStack();
    }

    @Override
    public SlurryStack insertChemical(SlurryStack stack, Action action)
    {
        if(stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = storage.insert(new SlurryStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;// 始终全部插入
    }

    // 从第一个槽位提取指定化学品
    @Override
    public SlurryStack extractChemical(long amount, Action action)
    {
        return ((SlurryStackType)storage.extract(new SlurryStackType( new SlurryStack(getChemicalInTank(0),amount)),action.simulate()))
                .copyStack();
    }

    // 按类型提取化学品
    @Override
    public SlurryStack extractChemical(SlurryStack stack, Action action)
    {
        return ((SlurryStackType)storage.extract(new SlurryStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
