package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import net.minecraft.resources.ResourceLocation;

// 用于创建不同stackType类型实例的工厂类 当不希望暴露具体类型时，使用这个类
public class StackCreater
{
    public static <T> IStackKey<T> Create(ResourceLocation typeId, T stack, long amount)
    {
        IStackKey<T> stackType = (IStackKey<T>) StackKeyRegistry.getType(typeId).copy();
        stackType.setStack(stack);
        stackType.setStackAmount(amount);
        return stackType;
    }

    public static <T> IStackKey<T> Create(ResourceLocation typeId, T stack)
    {
        IStackKey<T> stackType = (IStackKey<T>) StackKeyRegistry.getType(typeId).copy();
        stackType.setStack(stack); // 这样做，amount是stack本身数量
        return stackType;
    }

    public static <T> IStackKey<T> CreateEmpty(ResourceLocation typeId)
    {
        IStackKey<T> stackType = (IStackKey<T>) StackKeyRegistry.getType(typeId).copy();
        stackType.setStack(stackType.getEmptyStack());
        return stackType;
    }
}
