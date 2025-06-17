package com.wintercogs.beyonddimensions.DataBase.Storage.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.GasStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import mekanism.api.Action;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasHandler;

public class GasUnifiedStorageHandler implements IGasHandler
{

    private UnifiedStorage storage;

    public GasUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getTypeIdIndexList(GasStackType.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size()+1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public GasStack getChemicalInTank(int slot)
    {
        return storage.getTypeIdIndexList(GasStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (GasStackType)storage.getStackBySlot(actualIndex))
                .map(GasStackType::getStack)
                .orElse(GasStack.EMPTY);
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
        return storage.getSlotCapacity(0);
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
