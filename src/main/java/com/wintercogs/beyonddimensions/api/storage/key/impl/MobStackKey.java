package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.longtype.MobType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.MobStackKeyRender;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public final class MobStackKey extends LongStackKey<MobType>
{
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "stack_type/mob");
    public static final MobStackKey EMPTY = new MobStackKey(EntityType.PIG);

    public static final MapCodec<MobStackKey> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(MobStackKey::entityType)
    ).apply(instance, MobStackKey::new));

    private final EntityType<?> entityType;

    public MobStackKey(EntityType<?> entityType)
    {
        this.entityType = entityType;
        this.stack = new MobType(entityType, 0);
    }

    public EntityType<?> entityType()
    {
        return entityType;
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public MapCodec<MobStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        return stack instanceof MobType mob
                ? new KeyAmount(new MobStackKey(mob.entityType()), mob.getStackCount())
                : null;
    }

    @Override
    public @Nullable MobStackKey fromSourceObject(Object key, DataComponentPatch ignored)
    {
        if (key instanceof EntityType<?> type) return new MobStackKey(type);
        if (key instanceof MobType mob) return new MobStackKey(mob.entityType());
        return null;
    }

    @Override
    public @NotNull MobType getSource()
    {
        return stack;
    }

    @Override
    public String getModId()
    {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return id == null ? "Minecraft" : id.getNamespace();
    }

    @Override
    public MobStackKey getEmpty()
    {
        return EMPTY;
    }

    @Override
    public MobType getEmptyStack()
    {
        return new MobType(entityType, 0);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return Stream.empty();
    }

    @Override
    public boolean isSame(com.wintercogs.beyonddimensions.api.storage.key.IStackKey<?> other)
    {
        return other instanceof MobStackKey mob && mob.entityType == entityType;
    }

    @Override
    public boolean isSameTypeSameComponents(com.wintercogs.beyonddimensions.api.storage.key.IStackKey<?> other)
    {
        return isSame(other);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof MobStackKey mob && mob.entityType == entityType;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(ID, entityType);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ENTITY_TYPE).encode(buf, entityType);
    }

    @Override
    public @NotNull MobStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return new MobStackKey(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ENTITY_TYPE).decode(buf));
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider access)
    {
        CompoundTag tag = new CompoundTag();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        tag.putString("entity_type", typeId.toString());
        return tag;
    }

    @Override
    public @NotNull MobStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider access)
    {
        ResourceLocation typeId = ResourceLocation.tryParse(nbt.getString("entity_type"));
        EntityType<?> type = typeId == null
                ? EntityType.PIG
                : BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(EntityType.PIG);
        return new MobStackKey(type);
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return MobStackKeyRender.INSTANCE;
    }
}
