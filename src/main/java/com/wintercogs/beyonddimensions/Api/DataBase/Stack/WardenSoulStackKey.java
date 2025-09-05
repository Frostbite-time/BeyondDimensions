package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.WardenSoulType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

public class WardenSoulStackKey extends LongStackKey<WardenSoulType> {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/warden_soul");

    /** 唯一实例 */
    public static final WardenSoulStackKey INSTANCE = new WardenSoulStackKey();

    /** 新格式：不写字段；decode 直接返回单例 */
    public static final MapCodec<WardenSoulStackKey> TYPE_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<WardenSoulStackKey> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                         com.mojang.serialization.MapLike<T> input) {
            return DataResult.success(WardenSoulStackKey.INSTANCE);
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(WardenSoulStackKey value,
                                                                    com.mojang.serialization.DynamicOps<T> ops,
                                                                    com.mojang.serialization.RecordBuilder<T> prefix) {
            return prefix;
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
            return java.util.stream.Stream.empty();
        }
    };

    public static final Codec<WardenSoulStackKey> CODEC = TYPE_CODEC.codec();

    private WardenSoulStackKey() {
        // 仅用于渲染最小非空量；不影响 Key 语义
        this.stack = new WardenSoulType(1);
    }

    @Override
    public MapCodec<WardenSoulStackKey> codec() {
        return TYPE_CODEC;
    }

    @Override
    public ResourceLocation getTypeID() {
        return ID;
    }

    @Override
    public String getModId() {
        return BeyondDimensions.IFS_ModId;
    }

    @Override
    public IStackKey<WardenSoulType> getEmpty()
    {
        return new WardenSoulStackKey();
    }

    @Override
    public @Nullable WardenSoulStackKey fromObject(Object key, net.minecraft.core.component.DataComponentPatch ignored) {
        if (key instanceof WardenSoulType || key instanceof Number) return INSTANCE;
        return null;
    }

    @Override
    public Object getSource() {
        return new WardenSoulType(0);
    }

    @Override
    public WardenSoulType getEmptyStack() {
        return new WardenSoulType(0);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public IStackRender<?> getRender() {
        return WardenSoulStackKeyRender.INSTANCE;
    }

    @Override
    public WardenSoulType getRenderStack() {
        WardenSoulType cache = this.stack;
        if (cache.getStackCount() <= 0) cache.setStackCount(1);
        return cache;
    }

    // —— 网络：仅写 typeId；读回单例 —— //
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(getTypeId());
    }

    @Override
    public WardenSoulStackKey deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) return null;
        return INSTANCE;
    }

    // —— NBT：仅写 Type；读回单例（旧 LongType 的 long 忽略） —— //
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider access) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        return tag;
    }

    @Override
    public WardenSoulStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider access) {
        return INSTANCE;
    }
}