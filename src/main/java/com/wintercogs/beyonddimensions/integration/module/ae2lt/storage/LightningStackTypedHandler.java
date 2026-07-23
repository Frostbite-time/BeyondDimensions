package com.wintercogs.beyonddimensions.integration.module.ae2lt.storage;

import com.moakiee.ae2lt.api.lightning.ILightningEnergyHandler;
import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;

public final class LightningStackTypedHandler implements ILightningEnergyHandler
{
    private final StackHandler storage;

    public LightningStackTypedHandler(StackHandler storage)
    {
        this.storage = storage;
    }

    @Override
    public long getStored(LightningTier tier)
    {
        return storage.getBucket(LightningStackKey.of(tier)).map(bucket -> {
            long total = 0;
            for (int i = 0; i < bucket.size(); i++)
            {
                long amount = storage.getStackBySlot(bucket.get(i)).amount();
                if (Long.MAX_VALUE - total < amount) return Long.MAX_VALUE;
                total += amount;
            }
            return total;
        }).orElse(0L);
    }

    @Override
    public long getCapacity(LightningTier tier)
    {
        int occupied = storage.getBucket(LightningStackKey.of(tier)).map(StackHandler.SlotBucket::size).orElse(0);
        int empty = storage.getBucket(EmptyStackKey.INSTANCE).map(StackHandler.SlotBucket::size).orElse(0);
        long slots = (long) occupied + empty;
        if (slots == 0) return 0;
        long perSlot = Math.min(LightningStackKey.of(tier).getVanillaMaxStackSize(), storage.getSlotCapacity(0));
        return perSlot > Long.MAX_VALUE / slots ? Long.MAX_VALUE : perSlot * slots;
    }

    @Override
    public long insert(LightningTier tier, long amount, boolean simulate)
    {
        long requested = Math.max(0, amount);
        long remainder = storage.insert(LightningStackKey.of(tier), requested, simulate).amount();
        return requested - remainder;
    }

    @Override
    public long extract(LightningTier tier, long amount, boolean simulate)
    {
        return storage.extract(LightningStackKey.of(tier), Math.max(0, amount), simulate, false).amount();
    }
}
