package com.wintercogs.beyonddimensions.DataBase.StackHandlerWrapper;

import net.minecraft.resources.ResourceLocation;

// 为类似IItemHandler类所做的包装，用来动态的包装来自其他模组的handler
public interface IStackHandlerWrapper<T>
{
    // 处理器类型表示
    ResourceLocation getTypeId();

    // 以下是通用实现
    public int getSlots();

    public T getStackInSlot(int slot);

    public long getCapacity(int slot);

    public boolean isStackValid(int slot, T stack);

    // 返回剩余量
    public long insert(int slot, T Stack, boolean sim);

    // 返回剩余量
    public long insert(T stack, boolean sim);

    // 返回插提取量
    public long extract(int slot, long amount, boolean sim);

    // 返回提取量
    public long extract(T stack, boolean sim);


}
