package com.wintercogs.beyonddimensions.DataBase.Stack;

import net.minecraft.resources.ResourceLocation;

// 存储系统中重要的部分，用于统一所有不同stack
public class GenericStack {
    private final ResourceLocation typeId;
    private final Object stack;
    private long amount;


    public GenericStack(ResourceLocation typeId, Object stack, long amount) {
        this.typeId = typeId;
        this.stack = stack;
        this.amount = amount;
    }

    public <T> T getStack(IStackType<T> type) {
        if (!type.getTypeId().equals(typeId)) {
            throw new IllegalArgumentException("Type mismatch");
        }
        return type.getStackClass().cast(stack);
    }

    @SuppressWarnings("unchecked")
    public <T> T getTypedStack(IStackType<T> type) {
        if (!type.getTypeId().equals(this.typeId)) {
            throw new IllegalArgumentException("Type mismatch");
        }
        return (T) this.stack; // 此处转换是安全的
    }

    public ResourceLocation getTypeId()
    {
        return typeId;
    }

    public long getAmount()
    {
        return this.amount;
    }

    public void setAmount(long newAmount)
    {
        this.amount = newAmount;
    }

    public void grow(long amount)
    {
        this.setAmount(getAmount()+amount);
    }

    public void shrink(long amount)
    {
        this.grow(-amount);
    }
}
