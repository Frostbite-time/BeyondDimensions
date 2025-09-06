package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyStackTypedHandler implements IEnergyStorage
{

    private StackHandler handlerStorage;

    public EnergyStackTypedHandler(StackHandler handlerStorage) {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int receiveEnergy(int count, boolean simulate)
    {
        return (int) (count - handlerStorage.insert(EnergyStackKey.INSTANCE, count,simulate).amount());
    }

    @Override
    public int extractEnergy(int count, boolean simulate)
    {
        return (int) handlerStorage.extract(EnergyStackKey.INSTANCE, count,simulate).amount();
    }

    @Override
    public int getEnergyStored()
    {
        return handlerStorage.getBucket(EnergyStackKey.ID)
                .map(bucket -> {
                    long sum = 0L;
                    for (int slot : bucket.snapshot()) {
                        if (slot < 0) continue;
                        long amt = handlerStorage.getStackBySlot(slot).amount();
                        if (amt <= 0) continue;

                        // 保证sum <= Integer.MAX_VALUE，不会溢出
                        long remain = (long) Integer.MAX_VALUE - sum;
                        if (amt > remain) {
                            return Integer.MAX_VALUE;
                        }
                        sum += amt;
                    }
                    return (int) sum; // 安全转换
                })
                .orElse(0);
    }

    @Override
    public int getMaxEnergyStored()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract()
    {
        return true;
    }

    @Override
    public boolean canReceive()
    {
        return true;
    }
}
