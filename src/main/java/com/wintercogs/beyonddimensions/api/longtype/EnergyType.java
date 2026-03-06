package com.wintercogs.beyonddimensions.api.longtype;

import net.minecraft.network.chat.Component;

public final class EnergyType extends LongType<EnergyType>
{

    public EnergyType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.energytype.name");
    }

    @Override
    public EnergyType getEmpty()
    {
        return new EnergyType(0);
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
