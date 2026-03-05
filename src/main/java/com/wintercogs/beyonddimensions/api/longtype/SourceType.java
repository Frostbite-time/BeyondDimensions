package com.wintercogs.beyonddimensions.api.longtype;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

// 新生魔艺-魔源
public class SourceType extends LongType<SourceType>
{
    public static final Codec<SourceType> CODEC = createCodec(SourceType::new);

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
