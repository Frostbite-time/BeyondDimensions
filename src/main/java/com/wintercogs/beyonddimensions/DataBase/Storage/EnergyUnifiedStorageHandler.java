package com.wintercogs.beyonddimensions.DataBase.Storage;

import com.wintercogs.beyonddimensions.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class EnergyUnifiedStorageHandler implements IEnergyStorage
{
    private UnifiedStorage storage;

    public EnergyUnifiedStorageHandler(UnifiedStorage storage) {
        this.storage = storage;
    }

    @Override
    public int receiveEnergy(int count, boolean simulate)
    {
        return (int) (count - storage.insert(new EnergyStackType(count),simulate).getStackAmount());
    }

    @Override
    public int extractEnergy(int count, boolean simulate)
    {
        return (int) storage.extract(new EnergyStackType(count),simulate).getStackAmount();
    }

    @Override
    public int getEnergyStored()
    {
        return storage.getTypeIdIndexList(EnergyStackType.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex>=0)
                .map(actualIndex -> (EnergyStackType)storage.getStackBySlot(actualIndex))
                .map(energyStackType -> BDMath.clampLongToInt(energyStackType.getStackAmount()))
                .orElse(0);
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
