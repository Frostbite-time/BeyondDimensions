package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.longtype.WardenSoulType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.WardenSoulStackKeyRender;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class WardenSoulStackKey extends LongStackKey<WardenSoulType>
{
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/warden_soul");

    /**
     * 唯一实例
     */
    public static final WardenSoulStackKey INSTANCE = new WardenSoulStackKey();

    /**
     * 新格式：不写字段；decode 直接返回单例
     */
    public static final MapCodec<WardenSoulStackKey> TYPE_CODEC = new MapCodec<>()
    {
        @Override
        public <T> DataResult<WardenSoulStackKey> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                         com.mojang.serialization.MapLike<T> input)
        {
            return DataResult.success(WardenSoulStackKey.INSTANCE);
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(WardenSoulStackKey value,
                                                                    com.mojang.serialization.DynamicOps<T> ops,
                                                                    com.mojang.serialization.RecordBuilder<T> prefix)
        {
            return prefix;
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops)
        {
            return java.util.stream.Stream.empty();
        }
    };

    public static final Codec<WardenSoulStackKey> CODEC = TYPE_CODEC.codec();

    private WardenSoulStackKey()
    {
        this.stack = new WardenSoulType(0);
    }

    @Override
    public MapCodec<WardenSoulStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof WardenSoulType wardenSoulType)
            return new KeyAmount(WardenSoulStackKey.INSTANCE, wardenSoulType.getStackCount());
        return null;
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 1000;
    }

    @Override
    public String getModId()
    {
        return BeyondDimensions.IFS_ModId;
    }

    @Override
    public WardenSoulStackKey getEmpty()
    {
        return WardenSoulStackKey.INSTANCE;
    }

    @Override
    public @Nullable WardenSoulStackKey fromSourceObject(Object key, net.minecraft.core.component.DataComponentPatch ignored)
    {
        if (key instanceof WardenSoulType || key instanceof Number) return INSTANCE;
        return null;
    }

    @Override
    public @NotNull WardenSoulType getSource()
    {
        return this.stack;
    }

    @Override
    public WardenSoulType getEmptyStack()
    {
        return new WardenSoulType(0);
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
    public @NotNull IStackRender getRender()
    {
        return WardenSoulStackKeyRender.INSTANCE;
    }

    // —— 网络：仅写 typeId；读回单例 —— //
    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
    }

    @Override
    public @NotNull WardenSoulStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    // —— NBT：仅写 Type；读回单例（旧 LongType 的 long 忽略） —— //
    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider access)
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull WardenSoulStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider access)
    {
        return INSTANCE;
    }
}