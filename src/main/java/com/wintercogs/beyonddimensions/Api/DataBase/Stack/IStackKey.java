package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 资源Key，对一种资源的唯一标识，其实现必须是不可变对象
 */
public interface IStackKey<T>
{
    /**
     * CODEC定义
     */
    public static final Codec<IStackKey<?>> CODEC = ResourceLocation.CODEC
            .dispatch(
                    "type",
                    IStackKey::getTypeId,  // 分发到具体实现的编解码器
                    id -> {
                        IStackKey<?> type = StackKeyRegistry.getType(id);
                        return type.codec(); // A → MapCodec
                    }
            );

    /*
     * 流编码器定义
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, IStackKey<?>> STREAM_CODEC =
            new StreamCodec<>()
            {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, IStackKey<?> stackType)
                {
                    stackType.serialize(buf);
                }

                @Override
                public IStackKey<?> decode(RegistryFriendlyByteBuf byteBuf)
                {
                    return IStackKey.deserializeCommon(byteBuf);
                }
            };

    /**
     * 获取类型的唯一标识符 如(beyonddimension:stack_type/item) beyonddimension为本modID，提供对原版Item的支持，Item为要支持的Stack类型
     */
    ResourceLocation getTypeId();

    /*
     * 定义实现的编解码器 需要注册表信息，在接口实现实在太复杂了，分开到每个具体实现就会简单很多
     */
    MapCodec<? extends IStackKey<T>> codec();

    @Nullable KeyAmount fromStackObject(Object stack);

    /**
     * 从未知Object构建实例，如果Object不合法，则返回null
     * @param key 类似Item
     * @param dataComponentPatch 数据组件
     * @return 类似ItemStack
     */
    @Nullable IStackKey<T> fromSourceObject(Object key, DataComponentPatch dataComponentPatch);

    /**
     * 获取堆叠的类型，如ItemStackType，返回ItemStack.class
     */
    Class<T> getStackClass();

    /**
     * 获取根，如：ItemStackType，返回其存储的ItemStack的Item
     */
    @NotNull Object getSource();

    /**
     * 获取根类型，如：ItemStackType 返回Item.class
     */
    Class<?> getSourceClass();

    /**
     * 获取资源所属的模组id
     */
    String getModId();

    /**
     * 判断堆叠是否为空堆叠
     */
    boolean isEmpty();

    /**
     * 获取当前类型的空堆叠，如：ItemStackType.getEmpty会返回 new ItemStackType()
     */
    IStackKey<T> getEmpty();

    /**
     * 获得当前存储类型的空实例，如ItemStackType返回ItemStack.EMPTY
     */
    T getEmptyStack();

    /**
     * 复制存储的堆叠，数量固定为1
     */
    T copyStack();

    /**
     * 按数量复制存储的堆叠
     */
    T copyStackWithCount(long count);

    /**
     * 当前存储的堆叠，其在原版游戏的普通容器（如箱子）中，单个堆叠的最大容量应为多少？
     */
    long getVanillaMaxStackSize();

    /**
     * 你期望当前存储的堆叠最大容量为多少
     */
    long getCustomMaxStackSize();

    /**
     * 当前堆叠是否有此标签？
     */
    boolean hasTag(TagKey<?> tagKey);

    /**
     * 检查2个实例是否能模糊匹配，即：2个物品，是否为同一种物品，不管NBT等数据
     */
    boolean isSame(IStackKey<?> other);

    /**
     * 检查2个实例是否能精确匹配，即：2个物品，种类、NBT等数据是否一致。但是不考虑存储数量
     */
    boolean isSameTypeSameComponents(IStackKey<?> other);

    /**
     * 网络序列化，第一步始终写入类型id
     */
    void serialize(RegistryFriendlyByteBuf buf);

    /**
     * 网络反序列化，除非你是特意的，否则不要读取写入的类型id。也不要调用这个函数。
     * <p>
     * 你应该由{@link IStackKey#deserializeCommon(RegistryFriendlyByteBuf)}来反序列化
     */
    IStackKey<T> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId);

    /**
     * 网络反序列化，会自动匹配类型
     */
    static IStackKey deserializeCommon(RegistryFriendlyByteBuf buf)
    {
        ResourceLocation typeId = buf.readResourceLocation();
        for(IStackKey stacktype : StackKeyRegistry.getAllTypes())
        {
            IStackKey stack = stacktype.deserialize(buf,typeId);
            if(stack!=null)
            {
                return stack;
            }
        }

        return null;
    }

    /**
     * NBT序列化，必须写入Type数据
     */
    CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess);

    /**
     * NBT反序列化，与网络序列化不同的是，你可以在需要的时候调用它。
     */
    IStackKey<T> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess);

    /**
     * NBT反序列化，自动匹配类型
     */
    static IStackKey deserializeNBTCommon(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        ResourceLocation typeId = ResourceLocation.tryParse(nbt.getString("Type"));
        for(IStackKey stacktype : StackKeyRegistry.getAllTypes())
        {
            if(stacktype.getTypeId().equals(typeId))
            {
                IStackKey stack = stacktype.deserializeNBT(nbt,levelRegistryAccess);
                if(stack!=null)
                {
                    return stack;
                }
            }
        }

        return null;
    }

    IStackRender getRender();

    T getRenderStack();

    /**
     * 强制要求重写哈希码
     */
    int hashCode();

    /**
     * 强制要求重写equals
     */
    boolean equals(Object other);

}
