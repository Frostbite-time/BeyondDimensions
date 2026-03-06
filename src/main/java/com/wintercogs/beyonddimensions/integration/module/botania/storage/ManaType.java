package com.wintercogs.beyonddimensions.integration.module.botania.storage;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.api.longtype.LongType;
import net.minecraft.network.chat.Component;

// 植物魔法-mana兼容
public class ManaType extends LongType<ManaType>
{
    public static final Codec<ManaType> CODEC = createCodec(ManaType::new);

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
