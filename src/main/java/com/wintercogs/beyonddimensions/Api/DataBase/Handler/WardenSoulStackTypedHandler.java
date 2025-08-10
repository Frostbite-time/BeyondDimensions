package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.WardenSoulStackType;
import com.wintercogs.beyonddimensions.Unit.BDMath;

import java.util.List;

public class WardenSoulStackTypedHandler implements ISoulHandler
{
    private StackTypedHandler handlerStorage;

    public WardenSoulStackTypedHandler(StackTypedHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getSoulTanks()
    {
        return handlerStorage.getTypeIdIndexList(WardenSoulStackType.ID)
                .map(List::size)
                .orElse(0);
    }

    @Override
    public int getSoulInTank(int slot)
    {
        return handlerStorage.getTypeIdIndexList(WardenSoulStackType.ID)
                .filter(slots -> slot >= 0 && slot < slots.size())
                .map(slots -> slots.get(slot))
                .filter(actualIndex -> actualIndex >= 0)
                .map(handlerStorage::getStackBySlot)
                .map(stack -> BDMath.clampLongToInt(stack.getStackAmount()))
                .orElse(0);
    }

    @Override
    public int getTankCapacity(int slot)
    {
        return BDMath.clampLongToInt(handlerStorage.getSlotCapacity(slot));
    }

    @Override
    public int fill(int amount, Action action)
    {
        return (int) (amount - handlerStorage.insert(new WardenSoulStackType(amount),action.simulate()).getStackAmount());
    }

    @Override
    public int drain(int amount, Action action)
    {
        return (int) handlerStorage.extract(new WardenSoulStackType(amount),action.simulate()).getStackAmount();
    }
}
