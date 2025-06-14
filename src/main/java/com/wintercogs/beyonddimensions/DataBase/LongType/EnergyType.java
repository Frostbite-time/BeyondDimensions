package com.wintercogs.beyonddimensions.DataBase.LongType;

public class EnergyType extends LongType<EnergyType>
{

    public EnergyType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public EnergyType copy()
    {
        return new EnergyType(stackCount);
    }

    @Override
    public EnergyType copyWithAmount(long amount)
    {
        return new EnergyType(amount);
    }
}
