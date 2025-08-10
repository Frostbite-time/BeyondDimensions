package com.wintercogs.beyonddimensions.Api.DataBase.LongType;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

// 工业先锋-监守者之魂
public class WardenSoulType extends LongType<WardenSoulType>
{
    public static final Codec<WardenSoulType> CODEC = createCodec(WardenSoulType::new);

    public WardenSoulType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.warden_soul_type.name");
    }

    @Override
    public WardenSoulType getEmpty()
    {
        return new WardenSoulType(0);
    }

    @Override
    public WardenSoulType copy()
    {
        return new WardenSoulType(stackCount);
    }

    @Override
    public WardenSoulType copyWithAmount(long amount)
    {
        return new WardenSoulType(amount);
    }
}
