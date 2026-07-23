package com.wintercogs.beyonddimensions.integration.module.ae2lt.storage;

import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.api.longtype.LongType;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class LightningType extends LongType<LightningType>
{
    public static final Codec<LightningType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LightningTier.CODEC.fieldOf("tier").forGetter(LightningType::tier),
            Codec.LONG.fieldOf("amount").forGetter(LightningType::getStackCount)
    ).apply(instance, LightningType::new));

    private final LightningTier tier;

    public LightningType(LightningTier tier, long amount)
    {
        this.tier = Objects.requireNonNull(tier);
        this.stackCount = amount;
    }

    public LightningTier tier()
    {
        return tier;
    }

    @Override
    public Component getName()
    {
        return Component.translatable("types.beyonddimensions.lightning." + tier.getSerializedName());
    }

    @Override
    public LightningType getEmpty()
    {
        return new LightningType(tier, 0);
    }

    @Override
    public LightningType copy()
    {
        return new LightningType(tier, stackCount);
    }

    @Override
    public LightningType copyWithAmount(long amount)
    {
        return new LightningType(tier, amount);
    }

    @Override
    public boolean isSame(LongType<?> other)
    {
        return other instanceof LightningType lightning && lightning.tier == tier;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof LightningType lightning && lightning.tier == tier;
    }

    @Override
    public int hashCode()
    {
        return tier.hashCode();
    }
}
