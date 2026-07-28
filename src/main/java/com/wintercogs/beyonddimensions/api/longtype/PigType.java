package com.wintercogs.beyonddimensions.api.longtype;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public final class PigType extends LongType<PigType>
{
    public static final Codec<PigType> CODEC = createCodec(PigType::new);

    public PigType(long amount)
    {
        this.stackCount = amount;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.pig.name");
    }

    @Override
    public PigType getEmpty()
    {
        return new PigType(0);
    }

    @Override
    public PigType copy()
    {
        return new PigType(stackCount);
    }

    @Override
    public PigType copyWithAmount(long amount)
    {
        return new PigType(amount);
    }
}
