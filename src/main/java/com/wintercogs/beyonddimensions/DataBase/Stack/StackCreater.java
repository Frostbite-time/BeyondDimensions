package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.Registry.StackTypeRegistry;
import net.minecraft.util.ResourceLocation;

// 用于创建不同stackType类型实例的工厂类 当不希望暴露具体类型时，使用这个类
public class StackCreater
{
    public static <T> IStackType<T> Create(ResourceLocation typeId, T stack, long amount)
    {
        IStackType<T> stackType = (IStackType<T>) StackTypeRegistry.getType(typeId).copy();
        stackType.setStack(stack);
        stackType.setStackAmount(amount);
        return stackType;
    }

    public static <T> IStackType<T> CreateEmpty(ResourceLocation typeId)
    {
        IStackType<T> stackType = (IStackType<T>) StackTypeRegistry.getType(typeId).copy();
        stackType.setStack(stackType.getEmptyStack());
        return stackType;
    }
}
