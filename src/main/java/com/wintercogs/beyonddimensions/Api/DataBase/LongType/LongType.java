package com.wintercogs.beyonddimensions.Api.DataBase.LongType;

import net.minecraft.network.chat.Component;

/**
 * 任何纯数值型堆叠的包装类
 */
public abstract class LongType<T>
{
    protected long stackCount;

    public long getStackCount()
    {
        return stackCount;
    }

    public void setStackCount(long stackCount)
    {
        this.stackCount = stackCount;
    }

    public void grow(long amount)
    {
        setStackCount(getStackCount()+amount);
    }

    public void shrink(long amount)
    {
        grow(-amount);
    }

    public boolean isEmpty()
    {
        return stackCount <= 0;
    }

    public abstract Component getName();

    public abstract LongType<T> getEmpty();

    public abstract LongType<T> copy();

    public abstract LongType<T> copyWithAmount(long amount);

    public boolean isSame(LongType<?> other)
    {
        if (other == null) {
            return false;
        }
        return getClass() == other.getClass()
                && this.stackCount == other.stackCount;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LongType other = (LongType) obj;
        return stackCount == other.stackCount;
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(stackCount);
    }
}

