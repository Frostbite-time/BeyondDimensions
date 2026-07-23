package com.wintercogs.beyonddimensions.integration.module.ae2lt.storage;

import com.moakiee.ae2lt.api.lightning.ILightningEnergyHandler;
import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;

public final class LightningUnifiedStorageHandler implements ILightningEnergyHandler
{
    private final UnifiedStorage storage;

    public LightningUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public long getStored(LightningTier tier)
    {
        return storage.getStackByKey(LightningStackKey.of(tier)).amount();
    }

    @Override
    public long getCapacity(LightningTier tier)
    {
        return storage.getSlotCapacity(0);
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
