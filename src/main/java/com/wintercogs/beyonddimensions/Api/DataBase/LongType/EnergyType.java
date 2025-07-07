package com.wintercogs.beyonddimensions.Api.DataBase.LongType;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public class EnergyType extends LongType<EnergyType>
{
    public static final Codec<EnergyType> CODEC = createCodec(EnergyType::new);

    public EnergyType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.energyType.name");
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
