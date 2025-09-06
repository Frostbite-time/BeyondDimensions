package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.wintercogs.beyonddimensions.Api.DataBase.LongType.LongType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public abstract class LongStackKey<T extends LongType<T>> implements IStackKey<T>
{
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    public abstract ResourceLocation getTypeID();
    protected T stack;

    protected int hashCodeCache = 0; // 哈希码缓存
    protected boolean NeedRecalHash = true; // 指示什么时候需要重新计算哈希

    @Override
    public ResourceLocation getTypeId()
    {
        return getTypeID();
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


    @Override
    public String getModId()
    {
        return BeyondDimensions.MODID;
    }

    @Override
    public boolean isEmpty()
    {
        return stack.isEmpty();
    }

    @Override
    public T copyStack()
    {
        return copyStackWithCount(1L);
    }

    @Override
    public T copyStackWithCount(long count)
    {
        return (T)stack.copyWithAmount(count);
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
    public boolean isSame(IStackKey<?> other) {
        return other != null && Objects.equals(other.getTypeId(), this.getTypeId());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other) {
        // LongType Key 无组件，语义与 isSame 相同
        return isSame(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IStackKey<?> k)) return false;
        return Objects.equals(k.getTypeId(), this.getTypeId());
    }

    @Override
    public int hashCode() {
        // 基于物品类型和组件生成哈希码
        if(NeedRecalHash)
        {
            hashCodeCache = Objects.hash(getTypeId());
            NeedRecalHash = false;
        }
        return hashCodeCache;
    }
}
