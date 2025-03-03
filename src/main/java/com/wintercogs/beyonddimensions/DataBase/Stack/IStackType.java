package com.wintercogs.beyonddimensions.DataBase.Stack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

// 用于定义不同stack的行为 物品 流体 以及其他模组中行为逻辑stack相似的资源
public interface IStackType<T> {

    // 类型的唯一标识符 如(beyonddimension:stack_type/item) beyonddimension为本modID，提供对原版Item的支持，Item为要支持的Stack类型
    ResourceLocation getTypeId();

    // 获取类型
    Class<T> getStackClass();

    // 新增方法：判断堆栈是否为空（如ItemStack.isEmpty()）
    boolean isEmpty(T stack);

    // 空实例（例如：ItemStack.EMPTY）
    T getEmptyStack();

    // 新增方法：复制堆栈
    T copyStack(T stack);

    // 按数量复制堆叠
    T copyStackWithCount(T stack, long count);

    // 获取stack当前的数量
    long getStackAmount(T stack);

    // 单个堆叠的最大容量
    long getMaxStackSize(T stack);

    // 获取期望的单个堆叠最大容量
    long getCustomMaxStackSize();

    T splitStack(T stack, long amount); // 分割出指定数量的堆叠

    boolean isSameStack(T existing, T other); // 检查两个堆叠除数量和组件外的一切是否相同，例如如果是物品 就只检查物品类型是否相同

    boolean isSameStackSameComponents(T existing, T other); // 检查两个堆叠除数量外的一切是否相同，例如如果是物品 就检查物品类型和所有组件是否相同

    // 合并逻辑（返回合并后剩余量）
    long mergeStacks(T existing, T toInsert, long maxAmount);

    // 序列化/反序列化（用于网络和NBT）
    void serialize(FriendlyByteBuf buf, T stack, RegistryAccess levelRegistryAccess);
    T deserialize(FriendlyByteBuf buf,RegistryAccess levelRegistryAccess);

    // 新增方法：NBT序列化（用于磁盘存储）
    CompoundTag serializeNBT(T stack, HolderLookup.Provider levelRegistryAccess);
    T deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess);

    // UI渲染（在指定位置绘制图标和数量）
    void render(GuiGraphics gui, T stack, int x, int y);

    String getCountText(long count);

}
