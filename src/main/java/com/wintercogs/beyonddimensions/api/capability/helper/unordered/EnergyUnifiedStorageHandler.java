package com.wintercogs.beyonddimensions.api.capability.helper.unordered;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyUnifiedStorageHandler implements IEnergyStorage
{
    private final UnifiedStorage storage;
    private final boolean receives;
    private final boolean extracts;

    public EnergyUnifiedStorageHandler(UnifiedStorage storage)
    {
        this(storage, true, true);
    }

    /**
     * @param receives 能否向网络存入能量
     * @param extracts 能否从网络取出能量
     */
    public EnergyUnifiedStorageHandler(UnifiedStorage storage, boolean receives, boolean extracts)
    {
        this.storage = storage;
        this.receives = receives;
        this.extracts = extracts;
    }

    @Override
    public int receiveEnergy(int count, boolean simulate)
    {
        if (!receives)
            return 0;
        return (int) (count - storage.insert(EnergyStackKey.INSTANCE, count, simulate).amount());
    }

    @Override
    public int extractEnergy(int count, boolean simulate)
    {
        if (!extracts)
            return 0;
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
        return extracts;
    }

    @Override
    public boolean canReceive()
    {
        return receives;
    }
}
