package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.ManaType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManaStackKey extends LongStackKey<ManaType> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/mana");

    /** 唯一实例 */
    public static final ManaStackKey INSTANCE = new ManaStackKey();

    /** 无字段的新格式：decode 直接返回单例，encode 不写任何键 */
    public static final MapCodec<ManaStackKey> TYPE_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<ManaStackKey> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                   com.mojang.serialization.MapLike<T> input) {
            return DataResult.success(ManaStackKey.INSTANCE);
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(ManaStackKey value,
                                                                    com.mojang.serialization.DynamicOps<T> ops,
                                                                    com.mojang.serialization.RecordBuilder<T> prefix) {
            return prefix; // 不写任何字段
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
            return java.util.stream.Stream.empty();
        }
    };

    public static final Codec<ManaStackKey> CODEC = TYPE_CODEC.codec();

    private ManaStackKey() {
        // 仅用于渲染/显示：提供一个最小非空量的栈；不影响 Key 语义
        this.stack = new ManaType(1);
    }

    @Override
    public MapCodec<ManaStackKey> codec() {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if(stack instanceof ManaType manaType)
            return new KeyAmount(ManaStackKey.INSTANCE, manaType.getStackCount());
        return null;
    }

    @Override
    public ResourceLocation getTypeID() {
        return ID;
    }

    @Override
    public String getModId() {
        return BeyondDimensions.Botania_ModId;
    }

    @Override
    public IStackKey<ManaType> getEmpty()
    {
        return new ManaStackKey();
    }

    /** 允许从 ManaType 或 Number（数量无意义）映射到同一个 Key 实例 */
    @Override
    public @Nullable ManaStackKey fromSourceObject(Object key, net.minecraft.core.component.DataComponentPatch ignored) {
        if (key instanceof ManaType || key instanceof Number) {
            return INSTANCE;
        }
        return null;
    }

    @Override
    public @NotNull ManaType getSource() {
        return new ManaType(0);
    }

    @Override
    public ManaType getEmptyStack() {
        return new ManaType(0);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    // ------- 网络序列化：仅写 typeId；读回单例 -------

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {}

    @Override
    public @NotNull ManaStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    // ------- NBT：仅写 Type；读取直接返回单例（忽略旧的 Amount） -------

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        return tag;
    }

    @Override
    public @NotNull ManaStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        return INSTANCE;
    }

    // ------- 渲染 -------

    @Override
    public IStackRender getRender() {
        return ManaStackKeyRender.INSTANCE;
    }

    @Override
    public ManaType getRenderStack() {
        // 确保数量 >= 1，避免部分版本对 0 量渲染异常
        ManaType cache = this.stack;
        if (cache.getStackCount() <= 0) {
            cache.setStackCount(1);
        }
        return cache;
    }
}