package com.wintercogs.beyonddimensions.util;

public class StringFormat
{
    private static final String[] UNITS = {"K", "M", "G", "T", "P", "E"};
    private static final double[] SCALES = {1e3, 1e6, 1e9, 1e12, 1e15, 1e18};

    /**
     * 格子里的数量：最多四个字符，如 999、1.2K、86K、1.3M
     */
    public static String formatCompact(long amount)
    {
        if (amount < 1_000)
            return Long.toString(amount);
        int unit = SCALES.length - 1;
        while (amount < SCALES[unit])
            unit--;
        double value = amount / SCALES[unit];
        if (value < 10)
        {
            // 截断而不是四舍五入，避免 9.99K 显示成 10.0K
            long tenths = (long) (value * 10);
            return tenths % 10 == 0 ? tenths / 10 + UNITS[unit] : tenths / 10 + "." + tenths % 10 + UNITS[unit];
        }
        return (long) value + UNITS[unit];
    }

    /**
     * 流体与化学品先换算为桶数，再沿用格子里的数量缩写规则；不足一桶时显示 {@code <.1}。
     */
    public static String formatBucket(long amount)
    {
        if (amount < 1_000)
            return "<.1";
        return formatCompact(amount / 1_000);
    }
}
