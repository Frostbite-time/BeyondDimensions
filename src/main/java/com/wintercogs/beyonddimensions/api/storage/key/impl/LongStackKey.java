package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.wintercogs.beyonddimensions.api.longtype.LongType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class LongStackKey<T extends LongType<T>> implements IStackKey<T>
{
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    public abstract ResourceLocation getTypeID();

    protected T stack;

    protected int hashCodeCache = 0; // 哈希码缓存

    @Override
    public ResourceLocation getTypeId()
    {
        return getTypeID();
    }

    @Override
    public T getReadOnlyStack()
    {
        this.stack.setStackCount(1);
        return this.stack;
    }

    @Override
    public @NotNull T getRenderStack()
    {
        this.stack.setStackCount(1);
        return this.stack;
    }

    @Override
    public Class<T> getStackClass()
    {
        return (Class<T>) stack.getClass();
    }

    @Override
    public Class<?> getSourceClass()
    {
        return stack.getClass();
    }

    /**
     * 不可能为空键
     */
    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public T copyStack()
    {
        return copyStackWithCount(1L);
    }

    @Override
    public T copyStackWithCount(long count)
    {
        return (T) stack.copyWithAmount(count);
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return Long.MAX_VALUE; //决定了其在接口方块中的一次性最大容量
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        return other != null && Objects.equals(other.getTypeId(), this.getTypeId());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        // LongType Key 无组件，语义与 isSame 相同
        return isSame(other);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof IStackKey<?> k)) return false;
        return Objects.equals(k.getTypeId(), this.getTypeId());
    }

    @Override
    public int hashCode()
    {
        // 基于物品类型和组件生成哈希码
        if (hashCodeCache == 0)
        {
            hashCodeCache = 31 + Objects.hashCode(getTypeId());
        }
        return hashCodeCache;
    }
}