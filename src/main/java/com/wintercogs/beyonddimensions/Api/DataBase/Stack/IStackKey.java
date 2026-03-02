package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * 资源Key，对一种资源的唯一标识，其实现必须是不可变对象
 */
public interface IStackKey<T>
{
    /**
     * CODEC定义，根据StackKeyRegistry分发到对应子类
     */
    public static final Codec<IStackKey<?>> CODEC = Identifier.CODEC
            .dispatch(
                    "type",
                    IStackKey::getTypeId,  // 分发到具体实现的编解码器
                    id -> {
                        IStackKey<?> type = StackKeyRegistry.getType(id);
                        return type.codec(); // A → MapCodec
                    }
            );

    /*
     * 流编码器定义，根据StackKeyRegistry分发到对应子类
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, IStackKey<?>> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, IStackKey<?> key)
        {
            // 先写类型 id
            buf.writeIdentifier(key.getTypeId());
            // 再写负载（各子类自己实现，注意不再写 typeId）
            key.serialize(buf);
        }

        @Override
        public @NotNull IStackKey<?> decode(RegistryFriendlyByteBuf buf)
        {
            // 先读类型 id
            Identifier typeId = buf.readIdentifier();
            // 通过注册表找到对应的“类型原型/工厂”
            IStackKey<?> typePrototype = StackKeyRegistry.getType(typeId);
            // 让对应类型去读取负载并返回具体实例
            return typePrototype.deserialize(buf);
        }
    };

    /**
     * 获取类型的唯一标识符 如(beyonddimension:stack_type/item) beyonddimension为本modID，提供对原版Item的支持，Item为要支持的Stack类型
     */
    Identifier getTypeId();

    /*
     * 用于向IStackKey的Codec提供具体编解码器
     */
    MapCodec<? extends IStackKey<T>> codec();

    /**
     * 从具体堆叠转出一个KeyAmount，如：ItemStack->KeyAmount(ItemStackKey,Long)
     * <p>
     * 如当前实例的解释无法完成转换，应当返回null
     */
    @Nullable KeyAmount fromStackObject(Object stack);

    /**
     * 从未知源Object构建实例，如果Object不合法，则返回null
     *
     * @param key                类似Item
     * @param dataComponentPatch 数据组件
     * @return 类似ItemStack
     */
    @Nullable IStackKey<T> fromSourceObject(Object key, DataComponentPatch dataComponentPatch);

    /**
     * 如ItemStackKey，返回ItemStack，应当返回一个缓存对象以提高性能
     * <p>
     * 由此输出的对象应当总是将数量设定为1，外界需要数量则应当自己再重新设置
     * <p>
     * 对于有组件的堆叠，不要修改它的组件！
     */
    T getReadOnlyStack();

    /**
     * 获取堆叠的类型，如ItemStackKey，返回ItemStack.class
     */
    Class<T> getStackClass();

    /**
     * 获取根，如：ItemStackType，返回其存储的Item
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
     * 获取当前类型的空堆叠，如：ItemStackKey.getEmpty会返回 ItemStackKey.EMPTY
     */
    IStackKey<T> getEmpty();

    /**
     * 获得当前存储类型的空实例，如ItemStackKey返回ItemStack.EMPTY
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
     * 当前堆叠标签
     */
    Stream<? extends TagKey<?>> getTags();

    /**
     * 检查2个实例是否能模糊匹配，即：2个物品，是否为同一种物品，不管NBT等数据
     */
    boolean isSame(IStackKey<?> other);

    /**
     * 检查2个实例是否能精确匹配，即：2个物品，种类、NBT等数据是否一致
     */
    boolean isSameTypeSameComponents(IStackKey<?> other);

    /**
     * 网络序列化，只写入自己的实际负载
     */
    void serialize(RegistryFriendlyByteBuf buf);

    /**
     * 网络反序列化，只处理自己的负载
     */
    @NotNull IStackKey<T> deserialize(RegistryFriendlyByteBuf buf);

    /**
     * NBT序列化
     */
    @NotNull CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess);

    /**
     * NBT反序列化
     */
    @NotNull IStackKey<T> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess);

    /**
     * 获取对应渲染器，仅在客户端调用
     */
    @NotNull IStackRender getRender();

    /**
     * 获取仅用于渲染显示的堆叠，尽可能返回缓存以提高性能
     * <p>
     * 其行为与GetReadOnly可以相同，但是可以返回不同的缓存备份以区分用途
     */
    @NotNull T getRenderStack();

    /**
     * 强制要求重写哈希码
     */
    int hashCode();

    /**
     * 强制要求重写equals
     */
    boolean equals(Object other);

}
