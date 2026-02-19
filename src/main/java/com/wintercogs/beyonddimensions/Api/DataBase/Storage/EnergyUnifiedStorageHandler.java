package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Util.BDMath;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyUnifiedStorageHandler implements IEnergyStorage
{
    private final UnifiedStorage storage;

    public EnergyUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    @Override
    public int receiveEnergy(int count, boolean simulate)
    {
        return (int) (count - storage.insert(EnergyStackKey.INSTANCE, count, simulate).amount());
    }

    @Override
    public int extractEnergy(int count, boolean simulate)
    {
        return (int) storage.extract(EnergyStackKey.INSTANCE, count, simulate, false).amount();
    }

    @Override
    public int getEnergyStored()
    {
        return BDMath.clampLongToInt(storage.getStackByKey(EnergyStackKey.INSTANCE).amount());
    }

    @Override
    public int getMaxEnergyStored()
    {
        return BDMath.clampLongToInt(storage.getSlotCapacity(0));
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
