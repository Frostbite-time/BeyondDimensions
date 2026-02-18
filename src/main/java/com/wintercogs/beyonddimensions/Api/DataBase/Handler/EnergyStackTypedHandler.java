package com.wintercogs.beyonddimensions.Api.DataBase.Handler;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStackTypedHandler implements IEnergyStorage
{

    private StackTypedHandler handlerStorage;

    public EnergyStackTypedHandler(StackTypedHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public int receiveEnergy(int count, boolean simulate)
    {
        return (int) (count - handlerStorage.insert(new EnergyStackKey(count), simulate).getStackAmount());
    }

    @Override
    public int extractEnergy(int count, boolean simulate)
    {
        return (int) handlerStorage.extract(new EnergyStackKey(count), simulate).getStackAmount();
    }

    @Override
    public int getEnergyStored()
    {
        return handlerStorage.getTypeIdIndexList(EnergyStackKey.ID)
                .map(slots -> slots.get(0))
                .filter(actualIndex -> actualIndex >= 0)
                .map(actualIndex -> (EnergyStackKey) handlerStorage.getStackBySlot(actualIndex))
                .map(energyStackType -> BDMath.clampLongToInt(energyStackType.getStackAmount()))
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
