package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.Api.Registry.StackTypeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 用于定义不同stack的行为 物品 流体 以及其他模组中行为逻辑与{@link ItemStack}相似的资源。
 * <p>
 * 此外，如果你想定义类似能量这种行为逻辑的资源，只需继承并实现抽象类{@link LongStackType}。
 * <p>
 * 对于这个泛型接口的使用，绝大部分情况下你都不需要考虑其具体类型。
 * <p>
 * 除了接口已经定义的方法外，实现还需重写hashcode以及equals方法，使其能匹配类型、内容、NBT等数据，总之是除了存储数量之外的一切数据
 * <p>
 * 另外，我建议你在具体实现中缓存hashcode，以降低性能开销
 */
public interface IStackType<T> {

    /**
     * CODEC定义
     */
    public static final Codec<IStackType> CODEC = ResourceLocation.CODEC
            .dispatch(
                    "type",
                    IStackType::getTypeId,  // 分发到具体实现的编解码器
                    id -> {
                        IStackType<?> type = StackTypeRegistry.getType(id);
                        return type.codec(); // A → MapCodec
                    }
            );

    /*
     * 流编码器定义
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, IStackType> STREAM_CODEC =
        new StreamCodec<>()
        {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, IStackType stackType)
            {
                stackType.serialize(buf);
            }

            @Override
            public IStackType decode(RegistryFriendlyByteBuf byteBuf)
            {
                return IStackType.deserializeCommon(byteBuf);
            }
        };


    /*
     * 定义实现的编解码器 需要注册表信息，在接口实现实在太复杂了，分开到每个具体实现就会简单很多
     */
    MapCodec<? extends IStackType<T>> codec();


    /**
     * 从未知Object构建实例
     * @param key 类似Item
     * @param amount 数量
     * @param dataComponentPatch 数据组件
     * @return 类似ItemStack
     */
    IStackType<T> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch);

    /**
     * 获取类型的唯一标识符 如(beyonddimension:stack_type/item) beyonddimension为本modID，提供对原版Item的支持，Item为要支持的Stack类型
     */
    ResourceLocation getTypeId();

    /**
     * 获取当前类型的空堆叠，如：ItemStackType.getEmpty会返回 new ItemStackType()
     */
    IStackType<T> getEmpty();

    /**
     * 返回存储的堆叠本身的引用。不要直接修改返回值，仅用于读取。
     * <p>
     * 如：ItemStackType返回其存储的ItemStack
     * @return 类似ItemStack
     */
    T getStack();

    /**
     * 设置存储的堆叠
     */
    void setStack(T stack); // 用于适应工厂方式来新建实例

    /**
     * 获取堆叠的类型，如ItemStackType，返回ItemStack.class
     */
    Class<T> getStackClass();

    /**
     * 获取根类型，如：ItemStackType 返回Item.class
     */
    Class<?> getSourceClass();

    /**
     * 获取根，如：ItemStackType，返回其存储的ItemStack的Item
     */
    Object getSource();

    /**
     * 获取资源所属的模组id
     */
    String getModId();

    /**
     * 判断堆叠是否为空，即存储数量或被存储的堆叠本身状态为空即为空
     */
    boolean isEmpty();

    /**
     * 判断堆叠是否为空堆叠类型，无视存储数量
     */
    boolean isEmptyStack();

    /**
     * 获得当前存储类型的空实例，如ItemStackType返回ItemStack.EMPTY
     */
    T getEmptyStack();

    /**
     * 复制存储的堆叠
     */
    T copyStack();

    /**
     * 按数量复制存储的堆叠
     */
    T copyStackWithCount(long count);

    /**
     * 复制当前实例
     */
    IStackType<T> copy();

    /**
     * 按数量复制当前实例
     */
    IStackType<T> copyWithCount(long count);

    /**
     * 获取当前存储数量
     */
    long getStackAmount();

    /**
     * 设置当前存储数量
     */
    void setStackAmount(long amount);

    /**
     * 增加当前存储数量
     */
    void grow(long amount);

    /**
     * 减少当前存储数量
     */
    void shrink(long amount);

    /**
     * 当前存储的堆叠，其在原版游戏的普通容器（如箱子）中，单个堆叠的最大容量应为多少？
     */
    long getVanillaMaxStackSize();

    /**
     * 你期望当前存储的堆叠最大容量为多少
     */
    long getCustomMaxStackSize();

    /**
     * 按数量分割出一部分存储的堆叠，并减少当前数量
     * @param amount 分割数量
     * @return 实际分割出来的堆叠
     */
    T splitStack(long amount);

    /**
     * 按数量分割出一部分存储实例，并减少当前数量
     * @param amount 分割数量
     * @return 实际分割出来的实例
     */
    IStackType<T>  split(long amount);

    /**
     * 检查2个实例是否能模糊匹配，即：2个物品，是否为同一种物品，不管NBT等数据
     */
    boolean isSame(IStackType<?> other);

    /**
     * 检查2个实例是否能精确匹配，即：2个物品，种类、NBT等数据是否一致。但是不考虑存储数量
     */
    boolean isSameTypeSameComponents(IStackType<?> other);

    /**
     * 网络序列化，第一步始终写入类型id
     */
    void serialize(RegistryFriendlyByteBuf buf);

    /**
     * 网络反序列化，除非你是特意的，否则不要读取写入的类型id。也不要调用这个函数。
     * <p>
     * 你应该由{@link IStackType#deserializeCommon(RegistryFriendlyByteBuf)}来反序列化
     */
    IStackType<T> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId);

    /**
     * 网络反序列化，会自动匹配类型
     */
    static IStackType deserializeCommon(RegistryFriendlyByteBuf buf)
    {
        ResourceLocation typeId = buf.readResourceLocation();
        for(IStackType stacktype : StackTypeRegistry.getAllTypes())
        {
            IStackType stack = stacktype.deserialize(buf,typeId);
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
    IStackType<T> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess);

    /**
     * NBT反序列化，自动匹配类型
     */
    static IStackType deserializeNBTCommon(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        ResourceLocation typeId = ResourceLocation.tryParse(nbt.getString("Type"));
        for(IStackType stacktype : StackTypeRegistry.getAllTypes())
        {
            if(stacktype.getTypeId().equals(typeId))
            {
                IStackType stack = stacktype.deserializeNBT(nbt,levelRegistryAccess);
                if(stack!=null)
                {
                    return stack;
                }
            }
        }

        return null;
    }

    /**
     * UI渲染，即绘制当前资源的图标。
     * <p>
     * 必须以注解标注为仅客户端
     */
    @OnlyIn(Dist.CLIENT)
    void render(net.minecraft.client.gui.GuiGraphics gui, int x, int y);

    /**
     * 对当前存储数量进行格式化
     */
    String getCountText(long count);

    /**
     * 获取资源名称
     */
    Component getDisplayName();

    /**
     * 获取资源的工具提示
     */
    List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag);

    /**
     *
     */
    Optional<TooltipComponent> getTooltipImage();

    /**
     * 绘制工具提示，必须要标记为仅客户端
     */
    @OnlyIn(Dist.CLIENT)
    void renderTooltip(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, int mouseX, int mouseY);

}
