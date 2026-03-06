package com.wintercogs.beyonddimensions.util;

public class BDMath
{

    public static int clampLongToInt(long value)
    {
        if (Integer.MIN_VALUE > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException(Integer.MIN_VALUE + " > " + Integer.MAX_VALUE);
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(value, Integer.MIN_VALUE));
    }

}
