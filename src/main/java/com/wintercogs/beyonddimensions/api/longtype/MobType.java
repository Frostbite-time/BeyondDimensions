package com.wintercogs.beyonddimensions.api.longtype;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

public final class MobType extends LongType<MobType>
{
    public static final Codec<MobType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(MobType::entityType),
            Codec.LONG.fieldOf("amount").forGetter(MobType::getStackCount)
    ).apply(instance, MobType::new));

    private final EntityType<?> entityType;

    public MobType(EntityType<?> entityType, long amount)
    {
        this.entityType = entityType;
        this.stackCount = amount;
    }

    public EntityType<?> entityType()
    {
        return entityType;
    }

    @Override
    public Component getName()
    {
        return entityType.getDescription();
    }

    @Override
    public MobType getEmpty()
    {
        return new MobType(entityType, 0);
    }

    @Override
    public MobType copy()
    {
        return new MobType(entityType, stackCount);
    }

    @Override
    public MobType copyWithAmount(long amount)
    {
        return new MobType(entityType, amount);
    }
}
