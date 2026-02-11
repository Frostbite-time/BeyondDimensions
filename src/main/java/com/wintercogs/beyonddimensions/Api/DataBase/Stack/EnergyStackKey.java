package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.EnergyType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class EnergyStackKey extends LongStackKey<EnergyType>
{

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/energy");

    /**
     * 唯一实例（不区分空/非空）
     */
    public static final EnergyStackKey INSTANCE = new EnergyStackKey();

    /**
     * 无字段的新格式：decode 直接返回单例，encode 不写任何键
     */
    public static final MapCodec<EnergyStackKey> TYPE_CODEC = new MapCodec<>()
    {
        @Override
        public <T> DataResult<EnergyStackKey> decode(com.mojang.serialization.DynamicOps<T> ops,
                                                     com.mojang.serialization.MapLike<T> input)
        {
            return DataResult.success(EnergyStackKey.INSTANCE);
        }

        @Override
        public <T> com.mojang.serialization.RecordBuilder<T> encode(EnergyStackKey value,
                                                                    com.mojang.serialization.DynamicOps<T> ops,
                                                                    com.mojang.serialization.RecordBuilder<T> prefix)
        {
            return prefix; // 不写任何字段
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops)
        {
            return java.util.stream.Stream.empty();
        }
    };

    public static final Codec<EnergyStackKey> CODEC = TYPE_CODEC.codec();

    private EnergyStackKey()
    {
        this.stack = new EnergyType(0);
    }

    // ---------------- IStackKey 必要实现 ----------------

    @Override
    public MapCodec<EnergyStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof EnergyType energyType)
            return new KeyAmount(EnergyStackKey.INSTANCE, energyType.getStackCount());
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
        return 1000000;
    }

    /**
     * 允许从 EnergyType/数字（数量无意义）转换为同一个 Key 实例
     */
    @Override
    public @Nullable EnergyStackKey fromSourceObject(Object key, DataComponentPatch ignored)
    {
        if (key instanceof EnergyType || key instanceof Number)
        {
            return INSTANCE;
        }
        return null;
    }

    @Override
    public @NotNull EnergyType getSource()
    {
        return this.stack;
    }

    @Override
    public String getModId()
    {
        return "NeoForge";
    }

    @Override
    public EnergyStackKey getEmpty()
    {
        return EnergyStackKey.INSTANCE;
    }

    @Override
    public EnergyType getEmptyStack()
    {
        return new EnergyType(0);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public Stream<? extends TagKey<?>> getTags() {
        return Stream.empty();
    }

    // ---------------- 网络序列化 ----------------
    // 只写入 typeId；deserialize 时直接返回单例

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
    }

    @Override
    public @NotNull EnergyStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    // ---------------- NBT ----------------
    // 仅写 Type；读取时直接返回单例。旧 LongType 是纯 long，可直接忽略。

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull EnergyStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        // 无论旧/新，都统一成单例 Key（旧数据里的 Amount 属于值层，不参与 Key）
        return INSTANCE;
    }

    // ---------------- 渲染支持（可选：若你的渲染系统通过 getRender() 取渲染器） ----------------

    @Override
    public @NotNull IStackRender getRender()
    {
        return EnergyStackKeyRender.INSTANCE; // 若不需要渲染器，可改为抛 UnsupportedOperationException
    }
}