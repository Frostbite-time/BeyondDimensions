package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

/**
 * 一个包含key和amount的记录类
 * <p>
 * 一般仅作于外部的只读视图
 */
public record KeyAmount(IStackKey<?> key, long amount)
{
    public boolean isEmpty()
    {
        return key == null || amount <= 0L;
    }

    public Object toStack()
    {
        return key.copyStackWithCount(amount);
    }
}
