package com.wintercogs.beyonddimensions.api.longtype;

import net.minecraft.network.chat.Component;

public class SourceType extends LongType<SourceType>
{

    public SourceType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.source_type.name");
    }

    @Override
    public SourceType getEmpty()
    {
        return new SourceType(0);
    }

    @Override
    public SourceType copy()
    {
        return new SourceType(stackCount);
    }

    @Override
    public SourceType copyWithAmount(long amount)
    {
        return new SourceType(amount);
    }
}
