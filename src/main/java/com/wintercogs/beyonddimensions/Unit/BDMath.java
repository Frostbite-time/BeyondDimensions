package com.wintercogs.beyonddimensions.Unit;

// 记录一些十分常用的数学方法
public class BDMath
{
    public static int clampLongToInt(long value)
    {
        return Math.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

}
