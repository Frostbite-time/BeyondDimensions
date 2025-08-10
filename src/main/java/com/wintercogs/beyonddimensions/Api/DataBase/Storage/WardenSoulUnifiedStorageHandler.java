package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.WardenSoulStackType;
import com.wintercogs.beyonddimensions.Unit.BDMath;

public class WardenSoulUnifiedStorageHandler implements ISoulHandler
{

    private UnifiedStorage storage;

    public WardenSoulUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }


    @Override
    public int getSoulTanks()
    {
        return storage.getTypeIdIndexList(WardenSoulStackType.ID)
                .map(list -> storage.isFullSlotsSize() ? list.size() : list.size()+1)
                .orElse(storage.isFullSlotsSize() ? 0 : 1);
    }

    @Override
    public int getSoulInTank(int slot)
    {
        return storage.getTypeIdIndexList(WardenSoulStackType.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (WardenSoulStackType)storage.getStackBySlot(actualIndex))
                .map(stack -> BDMath.clampLongToInt(stack.getStackAmount()))
                .orElse(0);
    }

    @Override
    public int getTankCapacity(int slot)
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
    }

    // 以下输入一个int进行交互，返回值不会大于int，因此强制转换是安全的
    // 返回插入量
    @Override
    public int fill(int amount, Action action)
    {
        return (int) (amount - storage.insert(new WardenSoulStackType(amount),action.simulate()).getStackAmount());
    }

    // 返回提取量
    @Override
    public int drain(int amount, Action action)
    {
        return (int) storage.extract(new WardenSoulStackType(amount),action.simulate()).getStackAmount();
    }
}
