package com.wintercogs.beyonddimensions.Api.DataBase.LongType;

import net.minecraft.network.chat.Component;

public class ManaType extends LongType<ManaType>
{

    public ManaType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.mana_type.name");
    }

    @Override
    public ManaType getEmpty()
    {
        return new ManaType(0);
    }

    @Override
    public ManaType copy()
    {
        return new ManaType(stackCount);
    }

    @Override
    public ManaType copyWithAmount(long amount)
    {
        return new ManaType(amount);
    }
}
