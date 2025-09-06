package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import net.minecraft.world.item.ItemStack;

/**
 * 一个包含key和amount的记录类，极其轻量
 * <p>
 * 一般仅作于外部的只读视图
 */
public record KeyAmount(IStackKey<?> key, long amount)
{
    public boolean isEmpty()
    {
        return key == null || amount <= 0L || key.isEmpty();
    }

    /** 给出当前kv对所代表的实际stack副本，不支持long数量的stack可能会被内部实现自动限制到int上限 */
    public Object toStack()
    {
        if(key != null) return key.copyStackWithCount(amount);
        else return ItemStack.EMPTY;
    }
}
