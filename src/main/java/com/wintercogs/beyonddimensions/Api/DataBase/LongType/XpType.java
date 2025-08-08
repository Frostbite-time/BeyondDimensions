package com.wintercogs.beyonddimensions.Api.DataBase.LongType;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public class XpType extends LongType<XpType>
{
    public static final Codec<XpType> CODEC = createCodec(XpType::new);

    public XpType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.xptype.name");
    }

    @Override
    public XpType getEmpty()
    {
        return new XpType(0);
    }

    @Override
    public XpType copy()
    {
        return new XpType(stackCount);
    }

    @Override
    public XpType copyWithAmount(long amount)
    {
        return new XpType(amount);
    }
}
