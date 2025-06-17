package com.wintercogs.beyonddimensions.DataBase.Storage.Chemicals;

import com.wintercogs.beyonddimensions.DataBase.Stack.Chemicals.InfusionStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import mekanism.api.Action;
import mekanism.api.chemical.infuse.IInfusionHandler;
import mekanism.api.chemical.infuse.InfusionStack;

public class InfusionUnifiedStorageHandler implements IInfusionHandler
{

    private UnifiedStorage storage;

    public InfusionUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getTypeIdIndexList(InfusionStackType.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size()+1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public InfusionStack getChemicalInTank(int slot)
    {
        return storage.getTypeIdIndexList(InfusionStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (InfusionStackType)storage.getStackBySlot(actualIndex))
                .map(InfusionStackType::getStack)
                .orElse(InfusionStack.EMPTY);
    }

    @Override
    public void setChemicalInTank(int tank, InfusionStack stack)
    {
        // 凡通过handler机械化输入的物品无论以何方法，全部为自动插入
        if(stack.isEmpty())
            return ;
        storage.insert(new InfusionStackType(stack.copy()), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return storage.getSlotCapacity(0);
    }

    @Override
    public boolean isValid(int tank, InfusionStack stack)
    {
        return true;
    }

    // 返回剩余量，与Fluid的返回插入量不同
    @Override
    public InfusionStack insertChemical(int tank, InfusionStack stack, Action action)
    {
        if(stack.isEmpty())
            return InfusionStack.EMPTY;
        long remaining = storage.insert(new InfusionStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new InfusionStack(stack, remaining);
        return InfusionStack.EMPTY;// 始终全部插入
    }

    // 尝试从指定槽位提取指定数量化学品
    @Override
    public InfusionStack extractChemical(int tank, long amount, Action action)
    {
        return ((InfusionStackType)storage.extract(new InfusionStackType(new InfusionStack(getChemicalInTank(tank),amount)),action.simulate()))
                .copyStack();
    }

    @Override
    public InfusionStack insertChemical(InfusionStack stack, Action action)
    {
        if(stack.isEmpty())
            return InfusionStack.EMPTY;
        long remaining = storage.insert(new InfusionStackType(stack.copy()), action.simulate()).getStackAmount();
        if(remaining>0)
            return new InfusionStack(stack, remaining);
        return InfusionStack.EMPTY;// 始终全部插入
    }

    // 从第一个槽位提取指定化学品
    @Override
    public InfusionStack extractChemical(long amount, Action action)
    {
        return ((InfusionStackType)storage.extract(new InfusionStackType( new InfusionStack(getChemicalInTank(0),amount)),action.simulate()))
                .copyStack();
    }

    // 按类型提取化学品
    @Override
    public InfusionStack extractChemical(InfusionStack stack, Action action)
    {
        return ((InfusionStackType)storage.extract(new InfusionStackType(stack.copy()),action.simulate()))
                .copyStack();
    }
}
