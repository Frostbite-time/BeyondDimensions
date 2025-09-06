package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.buuz135.industrialforegoingsouls.capabilities.ISoulHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.WardenSoulType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.WardenSoulStackKey;
import com.wintercogs.beyonddimensions.Unit.BDMath;

public class WardenSoulStackTypedHandler implements ISoulHandler
{
    private final StackHandler handlerStorage;

    public WardenSoulStackTypedHandler(StackHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int getSoulTanks()
    {
        return handlerStorage.getBucket(WardenSoulStackKey.ID)
                .map(StackHandler.SlotBucket::size)
                .orElse(0);
    }

    @Override
    public int getSoulInTank(int slot)
    {
        return handlerStorage.getBucket(WardenSoulStackKey.ID)
                .filter(slots -> slot>=0 && slot<slots.size())
                .map(slots -> slots.get(slot))
                .map(handlerStorage::getStackBySlot)
                .map(stack -> {
                    Object outStack = handlerStorage.getOutStackByKey(stack.key());
                    if(outStack instanceof WardenSoulType)
                    {
                        return BDMath.clampLongToInt(stack.amount());
                    }
                    return null;
                })
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
        return (int) (amount - handlerStorage.insert(WardenSoulStackKey.INSTANCE, amount,action.simulate()).amount());
    }

    @Override
    public int drain(int amount, Action action)
    {
        return (int) handlerStorage.extract(WardenSoulStackKey.INSTANCE, amount,action.simulate()).amount();
    }
}
