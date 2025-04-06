package com.wintercogs.beyonddimensions.DataBase.Storage.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.GasStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;

import java.util.List;

public class GasUnifiedStorageHandler implements IGasHandler
{

    private UnifiedStorage storage;

    public GasUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        List<Integer> slots = storage.getTypeIdIndexList(GasStackType.ID);
        if(slots != null)
            return slots.size();
        else return 0;
    }

    @Override
    public GasStack getChemicalInTank(int slot)
    {
        // 此处的slot参数是基于特化类型ItemStackType的索引
        List<Integer> slots = storage.getTypeIdIndexList(GasStackType.ID);
        int actualIndex = -1;
        if(slots != null && 0<=slot && slot < slots.size())
        {
            actualIndex = slots.get(slot);
        }

        if(actualIndex != -1)
        {
            return (GasStack) storage.getStackBySlot(actualIndex).getStack();
        }
        else return GasStack.EMPTY;
    }

    @Override
    public void setChemicalInTank(int tank, GasStack stack)
    {
        // 凡通过handler机械化输入的物品无论以何方法，全部为自动插入
        if(stack.isEmpty())
            return ;
        storage.insert(new GasStackType(stack.copy()), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean isValid(int tank, GasStack stack)
    {
        return true;
    }

    // 返回剩余量，与Fluid的返回插入量不同
    @Override
    public GasStack insertChemical(int tank, GasStack stack, Action action)
    {
        if(stack.isEmpty())
            return GasStack.EMPTY;
        long remaining = storage.insert(new GasStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;// 始终全部插入
    }

    // 尝试从指定槽位提取指定数量化学品
    @Override
    public GasStack extractChemical(int tank, long amount, Action action)
    {
        return ((GasStackType)storage.extract(new GasStackType(new GasStack(getChemicalInTank(tank),amount)),action.simulate()))
                .copyStack();
    }

    @Override
    public GasStack insertChemical(GasStack stack, Action action)
    {
        if(stack.isEmpty())
            return GasStack.EMPTY;
        long remaining = storage.insert(new GasStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new GasStack(stack, remaining);
        return GasStack.EMPTY;// 始终全部插入
    }

    // 从第一个槽位提取指定化学品
    @Override
    public GasStack extractChemical(long amount, Action action)
    {
        return ((GasStackType)storage.extract(new GasStackType( new GasStack(getChemicalInTank(0),amount)),action.simulate()))
                .copyStack();
    }

    // 按类型提取化学品
    @Override
    public GasStack extractChemical(GasStack stack, Action action)
    {
        return ((GasStackType)storage.extract(new GasStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
