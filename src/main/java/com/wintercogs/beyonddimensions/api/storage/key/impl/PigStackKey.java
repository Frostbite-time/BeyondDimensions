package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.longtype.PigType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.PigStackKeyRender;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public final class PigStackKey extends LongStackKey<PigType>
{
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "stack_type/pig");
    public static final PigStackKey INSTANCE = new PigStackKey();

    public static final MapCodec<PigStackKey> TYPE_CODEC = new MapCodec<>()
    {
        @Override
        public <T> DataResult<PigStackKey> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                  com.mojang.serialization.MapLike<T> input)
        {
            return DataResult.success(INSTANCE);
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(PigStackKey value,
                                                                     com.mojang.serialization.DynamicOps<T> ops,
                                                                     com.mojang.serialization.RecordBuilder<T> prefix)
        {
            return prefix;
        }

        @Override
        public <T> Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops)
        {
            return Stream.empty();
        }
    };

    public static final Codec<PigStackKey> CODEC = TYPE_CODEC.codec();

    private PigStackKey()
    {
        this.stack = new PigType(0);
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public MapCodec<PigStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        return stack instanceof PigType pig ? new KeyAmount(INSTANCE, pig.getStackCount()) : null;
    }

    @Override
    public @Nullable PigStackKey fromSourceObject(Object key, DataComponentPatch ignored)
    {
        return key instanceof PigType || key instanceof Number ? INSTANCE : null;
    }

    @Override
    public @NotNull PigType getSource()
    {
        return stack;
    }

    @Override
    public String getModId()
    {
        return "Minecraft";
    }

    @Override
    public PigStackKey getEmpty()
    {
        return INSTANCE;
    }

    @Override
    public PigType getEmptyStack()
    {
        return new PigType(0);
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
    public void serialize(RegistryFriendlyByteBuf buf)
    {
    }

    @Override
    public @NotNull PigStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider access)
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull PigStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider access)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return PigStackKeyRender.INSTANCE;
    }
}
