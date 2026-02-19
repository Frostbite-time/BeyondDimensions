package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Util.BDMath;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStackTypedHandler implements IEnergyStorage
{

    private StackHandler handlerStorage;

    public EnergyStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int receiveEnergy(int count, boolean simulate)
    {
        return (int) (count - handlerStorage.insert(EnergyStackKey.INSTANCE, count, simulate).amount());
    }

    @Override
    public int extractEnergy(int count, boolean simulate)
    {
        return (int) handlerStorage.extract(EnergyStackKey.INSTANCE, count, simulate, false).amount();
    }

    @Override
    public int getEnergyStored()
    {
        // 汇总“已使用能量的槽位”（EnergyStackKey.ID 桶）的当前总量
        return handlerStorage.getBucket(EnergyStackKey.ID)
                .map(bucket -> {
                    long sum = 0L;
                    final int n = bucket.size();
                    for (int i = 0; i < n; i++)
                    {
                        final int slot = bucket.get(i);
                        // 理论上桶内槽位都是有效的，这里不做额外校验
                        long amt = handlerStorage.getStackBySlot(slot).amount();
                        if (amt <= 0) continue;

                        long remain = (long) Integer.MAX_VALUE - sum;
                        if (amt >= remain)
                        {
                            return Integer.MAX_VALUE; // 提前截断，避免溢出与无谓循环
                        }
                        sum += amt;
                    }
                    return (int) sum;
                })
                .orElse(0);
    }

    @Override
    public int getMaxEnergyStored()
    {
        // 统计“空槽（Empty）+ 已用能量槽（Energy）”的容量总和
        long sumSlot = 0L;
        long cap = Math.min(EnergyStackKey.INSTANCE.getVanillaMaxStackSize(), handlerStorage.getSlotCapacity(0));

        // 1) 能量桶
        var energyBucketOpt = handlerStorage.getBucket(EnergyStackKey.ID);
        if (energyBucketOpt.isPresent())
        {
            sumSlot += energyBucketOpt.get().size();
        }

        // 2) 空桶
        var emptyBucketOpt = handlerStorage.getBucket(EmptyStackKey.INSTANCE);
        if (emptyBucketOpt.isPresent())
        {
            sumSlot += emptyBucketOpt.get().size();
        }

        return BDMath.clampLongToInt(sumSlot * cap);
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