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
import org.jetbrains.annotations.Nullable;

public class SourceStackKey extends LongStackKey<SourceType> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/source");

    /** 唯一实例 */
    public static final SourceStackKey INSTANCE = new SourceStackKey();

    /** 新格式：不写任何字段；decode 直接返回单例 */
    public static final MapCodec<SourceStackKey> TYPE_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<SourceStackKey> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                     com.mojang.serialization.MapLike<T> input) {
            return DataResult.success(SourceStackKey.INSTANCE);
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(SourceStackKey value,
                                                                    com.mojang.serialization.DynamicOps<T> ops,
                                                                    com.mojang.serialization.RecordBuilder<T> prefix) {
            return prefix; // 不写任何键
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
            return java.util.stream.Stream.empty();
        }
    };

    public static final Codec<SourceStackKey> CODEC = TYPE_CODEC.codec();

    private SourceStackKey() {
        // 仅用于渲染：最小非空
        this.stack = new SourceType(1);
    }

    @Override
    public MapCodec<SourceStackKey> codec() {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if(stack instanceof SourceType sourceType)
            return new KeyAmount(SourceStackKey.INSTANCE, sourceType.getStackCount());
        return null;
    }

    @Override
    public ResourceLocation getTypeID() {
        return ID;
    }

    @Override
    public String getModId() {
        return BeyondDimensions.ARS_ModId;
    }

    @Override
    public IStackKey<SourceType> getEmpty()
    {
        return new SourceStackKey();
    }

    /** 允许从 SourceType 或 Number（数量无意义）映射为同一个 Key 实例 */
    @Override
    public @Nullable SourceStackKey fromSourceObject(Object key, net.minecraft.core.component.DataComponentPatch ignored) {
        if (key instanceof SourceType || key instanceof Number) return INSTANCE;
        return null;
    }

    @Override
    public SourceType getSource() {
        return new SourceType(0);
    }

    @Override
    public SourceType getEmptyStack() {
        return new SourceType(0);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public IStackRender getRender() {
        return SourceStackKeyRender.INSTANCE;
    }

    @Override
    public SourceType getRenderStack() {
        SourceType cache = this.stack;
        if (cache.getStackCount() <= 0) cache.setStackCount(1);
        return cache;
    }

    // —— 网络：仅写 typeId；读回单例 —— //
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(getTypeId());
    }

    @Override
    public SourceStackKey deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) return null;
        return INSTANCE;
    }

    // —— NBT：仅写 Type；读回单例（忽略旧 Amount） —— //
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider access) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        return tag;
    }

    @Override
    public SourceStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider access) {
        return INSTANCE;
    }
}