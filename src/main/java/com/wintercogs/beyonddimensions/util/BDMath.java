package com.wintercogs.beyonddimensions.util;

public class BDMath
{
    public static int clampLongToInt(long value)
    {
        return Math.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
