package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.SourceType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class SourceStackKey extends LongStackKey<SourceType>
{

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/source");

    /**
     * 唯一实例
     */
    public static final SourceStackKey INSTANCE = new SourceStackKey();

    /**
     * 新格式：不写任何字段；decode 直接返回单例
     */
    public static final MapCodec<SourceStackKey> TYPE_CODEC = new MapCodec<>()
    {
        @Override
        public <T> DataResult<SourceStackKey> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                     com.mojang.serialization.MapLike<T> input)
        {
            return DataResult.success(SourceStackKey.INSTANCE);
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(SourceStackKey value,
                                                                    com.mojang.serialization.DynamicOps<T> ops,
                                                                    com.mojang.serialization.RecordBuilder<T> prefix)
        {
            return prefix; // 不写任何键
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops)
        {
            return java.util.stream.Stream.empty();
        }
    };

    public static final Codec<SourceStackKey> CODEC = TYPE_CODEC.codec();

    private SourceStackKey()
    {
        this.stack = new SourceType(0);
    }

    @Override
    public MapCodec<SourceStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof SourceType sourceType)
            return new KeyAmount(SourceStackKey.INSTANCE, sourceType.getStackCount());
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
        return 10000;
    }

    @Override
    public String getModId()
    {
        return BeyondDimensions.ARS_ModId;
    }

    @Override
    public SourceStackKey getEmpty()
    {
        return SourceStackKey.INSTANCE;
    }

    /**
     * 允许从 SourceType 或 Number（数量无意义）映射为同一个 Key 实例
     */
    @Override
    public @Nullable SourceStackKey fromSourceObject(Object key, net.minecraft.core.component.DataComponentPatch ignored)
    {
        if (key instanceof SourceType || key instanceof Number) return INSTANCE;
        return null;
    }

    @Override
    public @NotNull SourceType getSource()
    {
        return this.stack;
    }

    @Override
    public SourceType getEmptyStack()
    {
        return new SourceType(0);
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
        return SourceStackKeyRender.INSTANCE;
    }

    // —— 网络：仅写 typeId；读回单例 —— //
    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
    }

    @Override
    public @NotNull SourceStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    // —— NBT：仅写 Type；读回单例（忽略旧 Amount） —— //
    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider access)
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull SourceStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider access)
    {
        return INSTANCE;
    }
}