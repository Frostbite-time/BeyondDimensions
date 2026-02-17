package com.wintercogs.beyonddimensions.Api.DataBase.Storage.Chemicals;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.SlurryStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import mekanism.api.Action;
import mekanism.api.chemical.slurry.ISlurryHandler;
import mekanism.api.chemical.slurry.SlurryStack;

public class SlurryUnifiedStorageHandler implements ISlurryHandler
{

    private UnifiedStorage storage;

    public SlurryUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int getTanks()
    {
        return storage.getTypeIdIndexList(SlurryStackKey.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size() + 1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public SlurryStack getChemicalInTank(int slot)
    {
        return storage.getTypeIdIndexList(SlurryStackKey.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> (SlurryStackKey) storage.getStackBySlot(actualIndex))
                .map(SlurryStackKey::getStack)
                .orElse(SlurryStack.EMPTY);
    }

    @Override
    public void setChemicalInTank(int tank, SlurryStack stack)
    {
        // 凡通过handler机械化输入的物品无论以何方法，全部为自动插入
        if (stack.isEmpty())
            return;
        storage.insert(new SlurryStackKey(stack.copy()), false);
    }

    @Override
    public long getTankCapacity(int tank)
    {
        return storage.getSlotCapacity(0);
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
        if (stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = storage.insert(new SlurryStackKey(stack.copy()), action.simulate()).getStackAmount();
        if (remaining > 0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;// 始终全部插入
    }

    // 尝试从指定槽位提取指定数量化学品
    @Override
    public SlurryStack extractChemical(int tank, long amount, Action action)
    {
        return ((SlurryStackKey) storage.extract(new SlurryStackKey(new SlurryStack(getChemicalInTank(tank), amount)), action.simulate()))
                .copyStack();
    }

    @Override
    public SlurryStack insertChemical(SlurryStack stack, Action action)
    {
        if (stack.isEmpty())
            return SlurryStack.EMPTY;
        long remaining = storage.insert(new SlurryStackKey(stack.copy()), action.simulate()).getStackAmount();
        if (remaining > 0)
            return new SlurryStack(stack, remaining);
        return SlurryStack.EMPTY;// 始终全部插入
    }

    // 从第一个槽位提取指定化学品
    @Override
    public SlurryStack extractChemical(long amount, Action action)
    {
        return ((SlurryStackKey) storage.extract(new SlurryStackKey(new SlurryStack(getChemicalInTank(0), amount)), action.simulate()))
                .copyStack();
    }

    // 按类型提取化学品
    @Override
    public SlurryStack extractChemical(SlurryStack stack, Action action)
    {
        return ((SlurryStackKey) storage.extract(new SlurryStackKey(stack.copy()), action.simulate()))
                .copyStack();
    }
}
